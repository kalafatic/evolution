package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Universal source adapter for Parquet columnar dataset files.
 *
 * <p>Delegates to {@link ParquetReader}, a real Parquet codec (thrift-compact footer, row
 * groups, column chunks, page headers, SNAPPY/GZIP/LZ4/ZSTD/UNCOMPRESSED decompression and
 * PLAIN / RLE_DICTIONARY / RLE decoding). Flat rows are mapped to text / instruction samples;
 * nested LIST&lt;STRUCT&gt; {@code messages} layouts (SmolTalk, no_robots) are reconstructed
 * into role/content conversations. There is no heuristic byte scanning.</p>
 */
public class ParquetAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".parquet") || "PARQUET".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "ParquetAdapter", false, 0, "parquet", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        if (!exists) {
            return new DatasetInspection(item, "ParquetAdapter", false, 0, "parquet", false, "File does not exist");
        }
        try {
            ParquetReader.SchemaInfo schema = ParquetReader.readSchema(file);
            String detail = String.format("Parquet dataset: %d rows, %d column(s), %d row group(s)",
                    schema.numRows, schema.columns.size(), schema.rowGroups);
            return new DatasetInspection(item, "ParquetAdapter", true, file.length(), "parquet", true, detail);
        } catch (Exception e) {
            if (hasPar1Magic(file)) {
                // A genuine PAR1 file that fails to parse is corrupt.
                return new DatasetInspection(item, "ParquetAdapter", false, file.length(), "parquet", false,
                        "Corrupt Parquet file: " + e.getMessage());
            }
            // A .parquet file without PAR1 magic is still readable via the JSONL/text
            // fallback in convert(), so it remains a supported source.
            return new DatasetInspection(item, "ParquetAdapter", true, file.length(), "parquet", true,
                    "Parquet file without PAR1 magic; will be read as JSONL/text fallback");
        }
    }

    private static boolean hasPar1Magic(File file) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
            if (raf.length() < 4) return false;
            byte[] magic = new byte[4];
            raf.readFully(magic);
            return "PAR1".equals(new String(magic, java.nio.charset.StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [ParquetAdapter] File not found: " + item.getPath());
            return samples;
        }

        // Real Parquet goes through the genuine columnar codec. A file carrying a .parquet
        // extension but no PAR1 magic (e.g. a mis-labelled JSONL/JSON dump) is still readable
        // through a line-oriented fallback so downstream ingestion never silently drops data.
        if (isRealParquet(file)) {
            context.log("[forge.dataset] Reading Parquet source: " + file.getName());
            final int[] rowCount = {0};
            ParquetReader.forEachRow(file, row -> {
                if (context.isCancelled()) return;
                rowCount[0]++;
                JSONObject clean = toConversationJson(row);
                NormalizedSample sample = JSONLAdapter.parseJsonObject(clean, file.getName());
                if (sample != null) {
                    samples.add(sample);
                }
            });
            context.log(String.format("[FORGE-PARQUET] file=%s rows=%d samples=%d status=SUCCESS",
                    file.getName(), rowCount[0], samples.size()));
        } else {
            context.log("[forge.dataset] Reading non-PAR1 .parquet source as JSONL/text: " + file.getName());
            String rawStr = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            for (String line : rawStr.split("\r?\n")) {
                if (context.isCancelled()) break;
                String trimmed = line.trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        NormalizedSample s = JSONLAdapter.parseJsonObject(new JSONObject(trimmed), file.getName());
                        if (s != null) samples.add(s);
                        continue;
                    } catch (Exception ignored) {}
                }
                if (trimmed.length() >= 10) {
                    samples.add(NormalizedSample.createTextSample(trimmed, file.getName()));
                }
            }
            context.log(String.format("[FORGE-PARQUET] file=%s samples=%d status=SUCCESS(fallback)", file.getName(), samples.size()));
        }

        context.log("[forge.dataset] Parquet parsed " + samples.size() + " samples from " + file.getName());
        return samples;
    }

    /** Cheap validity check: PAR1 magic at both ends plus a readable thrift footer. */
    private static boolean isRealParquet(File file) {
        try {
            ParquetReader.readSchema(file);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Normalizes a decoded Parquet row into the flat JSON shape consumed by
     * {@link JSONLAdapter#parseJsonObject}. Nested LIST&lt;STRUCT&gt; message layouts (e.g.
     * {@code messages.list.element.content} + {@code messages.list.element.role}) are
     * reconstructed into a {@code messages} array of {@code {role, content}} objects; flat
     * columns are passed through unchanged.
     */
    static JSONObject toConversationJson(JSONObject row) {
        JSONObject out = new JSONObject();
        List<String> keys = new ArrayList<>(row.keySet());

        // Pair nested content/role (or value/from, text/role) arrays by their list prefix.
        Map<String, JSONArray> contentArrays = new TreeMap<>();
        Map<String, JSONArray> roleArrays = new TreeMap<>();
        for (String k : keys) {
            Object v = row.opt(k);
            if (!(v instanceof JSONArray)) continue;
            if (k.endsWith(".content") || k.endsWith(".value")) {
                contentArrays.put(listPrefix(k), (JSONArray) v);
            } else if (k.endsWith(".role") || k.endsWith(".from")) {
                roleArrays.put(listPrefix(k), (JSONArray) v);
            }
        }
        for (Map.Entry<String, JSONArray> e : contentArrays.entrySet()) {
            JSONArray roles = roleArrays.get(e.getKey());
            JSONArray contents = e.getValue();
            JSONArray msgs = new JSONArray();
            for (int i = 0; i < contents.length(); i++) {
                JSONObject m = new JSONObject();
                m.put("role", roles != null && i < roles.length() ? roles.opt(i) : "user");
                m.put("content", contents.opt(i));
                msgs.put(m);
            }
            if (msgs.length() > 0) {
                out.put("messages", msgs);
            }
        }

        // Pass through flat columns (strings and scalar numeric/boolean values).
        for (String k : keys) {
            Object v = row.opt(k);
            if (v instanceof JSONArray) continue; // nested arrays already handled above
            out.put(k, v);
        }
        return out;
    }

    /** Strips the trailing content/role/value/from segment to yield the list prefix. */
    private static String listPrefix(String key) {
        int idx = key.lastIndexOf('.');
        return idx >= 0 ? key.substring(0, idx) : key;
    }
}
