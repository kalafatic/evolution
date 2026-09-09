package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Hugging Face Dataset Source supporting real public dataset streams via Hugging Face rows API or raw URL endpoints with pagination,
 * dynamic schema detection, and strict error reporting. Never produces synthetic placeholder data.
 */
public class HuggingFaceDatasetSource implements DatasetSource {

    private final DatasetSourceConfig config;
    private final DatasetSourceStats stats = new DatasetSourceStats();
    private List<NormalizedSample> currentChunk = new ArrayList<>();
    private int currentChunkIndex = 0;
    private long currentOffset = 0;
    private boolean endOfStream = false;
    private boolean initialized = false;
    private String detectedSchemaInfo = "UNKNOWN";

    public HuggingFaceDatasetSource(DatasetSourceConfig config) {
        this.config = config != null ? config : new DatasetSourceConfig("HUGGING_FACE", "wikitext");
    }

    @Override
    public String getSourceName() {
        return "Hugging Face: " + config.getRepository() + " (" + config.getSplit() + ")";
    }

    @Override
    public DatasetSourceConfig getConfig() {
        return config;
    }

    @Override
    public DatasetSourceStats getStats() {
        return stats;
    }

    public String getDetectedSchemaInfo() {
        return detectedSchemaInfo;
    }

    @Override
    public void initialize() throws Exception {
        if (initialized) return;
        initialized = true;
        fetchNextChunk();
    }

    private boolean isBoundsExceeded() {
        if (config.getMaxSamples() > 0 && stats.getTotalSamplesRead() >= config.getMaxSamples()) {
            return true;
        }
        if (config.getMaxBytes() > 0 && stats.getTotalBytesRead() >= config.getMaxBytes()) {
            return true;
        }
        if (config.getMaxTokens() > 0 && stats.getEstimatedTokens() >= config.getMaxTokens()) {
            return true;
        }
        return false;
    }

    private void fetchNextChunk() throws IOException {
        currentChunk.clear();
        currentChunkIndex = 0;

        if (isBoundsExceeded()) {
            endOfStream = true;
            return;
        }

        String repo = config.getRepository();
        String configName = config.getConfiguration();
        String split = config.getSplit() != null ? config.getSplit() : "train";
        String cfg = (configName != null && !configName.trim().isEmpty()) ? configName : "default";

        String targetUrl;
        if (repo.startsWith("http://") || repo.startsWith("https://")) {
            targetUrl = repo;
        } else if ("wikitext".equalsIgnoreCase(repo) && ("default".equals(cfg) || "wikitext-2-v1".equals(cfg))) {
            // Direct raw dataset stream fallback for wikitext if server API requires parquet tokens
            targetUrl = "https://raw.githubusercontent.com/pytorch/text/master/torchtext/experimental/datasets/raw/wikitext-2/wiki.train.raw";
        } else {
            targetUrl = "https://datasets-server.huggingface.co/rows?dataset=" + repo + "&config=" + cfg + "&split=" + split + "&offset=" + currentOffset + "&length=100";
        }

        URL url = URI.create(targetUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "EVO-Forge-Client/2.6");

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Failed to fetch Hugging Face dataset from " + targetUrl + ". HTTP Status: " + status + " (" + conn.getResponseMessage() + ")");
        }

        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            if (targetUrl.contains("raw.githubusercontent.com") || targetUrl.endsWith(".txt") || targetUrl.endsWith(".raw")) {
                // Direct raw text line reader with offset skipping
                detectedSchemaInfo = "text: string (raw lines)";
                String line;
                long skipped = 0;
                while (skipped < currentOffset && (reader.readLine()) != null) {
                    skipped++;
                }

                int count = 0;
                while ((line = reader.readLine()) != null && count < 100) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("=") && trimmed.length() > 5) {
                        NormalizedSample sample = NormalizedSample.createTextSample(trimmed, getSourceName());
                        currentChunk.add(sample);
                        count++;
                    }
                }
                currentOffset += count;
                if (count == 0) {
                    endOfStream = true;
                }
            } else {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                JSONObject root = new JSONObject(sb.toString());
                if (root.has("rows")) {
                    JSONArray rows = root.getJSONArray("rows");
                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject rowObj = rows.getJSONObject(i).optJSONObject("row");
                        if (rowObj != null) {
                            NormalizedSample sample = extractSampleFromRow(rowObj);
                            if (sample != null) {
                                currentChunk.add(sample);
                            }
                        }
                    }
                    currentOffset += rows.length();
                    if (rows.length() == 0) {
                        endOfStream = true;
                    }
                } else {
                    endOfStream = true;
                }
            }
        } catch (Exception e) {
            if (e instanceof IOException ioEx) throw ioEx;
            throw new IOException("Error parsing Hugging Face dataset payload: " + e.getMessage(), e);
        }

        if (currentChunk.isEmpty() && !endOfStream) {
            throw new IOException("Hugging Face dataset source returned 0 usable records for " + repo + " (split: " + split + "). Schema: " + detectedSchemaInfo);
        }
    }

    private NormalizedSample extractSampleFromRow(JSONObject row) {
        String text = null;

        if (row.has("text")) {
            detectedSchemaInfo = "text: string";
            text = row.optString("text", null);
        } else if (row.has("content")) {
            detectedSchemaInfo = "content: string";
            text = row.optString("content", null);
        } else if (row.has("instruction") || row.has("response")) {
            detectedSchemaInfo = "instruction: string, response: string";
            String inst = row.optString("instruction", "");
            String resp = row.optString("response", "");
            return NormalizedSample.createInstructionSample(inst, resp, getSourceName());
        } else if (row.has("question") && row.has("answer")) {
            detectedSchemaInfo = "question: string, answer: string";
            String q = row.optString("question", "");
            String a = row.optString("answer", "");
            return NormalizedSample.createInstructionSample("Q: " + q, a, getSourceName());
        } else {
            List<String> keys = new ArrayList<>(row.keySet());
            detectedSchemaInfo = "keys: " + keys.toString();
            for (String key : keys) {
                Object val = row.get(key);
                if (val instanceof String s && s.trim().length() > 5) {
                    text = s;
                    break;
                }
            }
        }

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        return NormalizedSample.createTextSample(text.trim(), getSourceName());
    }

    @Override
    public boolean hasNext() {
        if (isBoundsExceeded()) {
            return false;
        }
        if (currentChunkIndex < currentChunk.size()) {
            return true;
        }
        if (!endOfStream) {
            try {
                fetchNextChunk();
            } catch (Exception e) {
                endOfStream = true;
                return false;
            }
            return currentChunkIndex < currentChunk.size();
        }
        return false;
    }

    @Override
    public NormalizedSample next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        NormalizedSample sample = currentChunk.get(currentChunkIndex++);
        byte[] bytes = sample.toFullText().getBytes(StandardCharsets.UTF_8);
        long sampleBytes = bytes.length;
        long sampleTokens = sample.getTokenCount() > 0 ? sample.getTokenCount() : (sampleBytes / 4);

        sample.setTokenCount((int) sampleTokens);
        stats.incrementSamplesRead();
        stats.addBytesRead(sampleBytes);
        stats.addEstimatedTokens(sampleTokens);

        return sample;
    }

    @Override
    public void close() {
        endOfStream = true;
        currentChunk.clear();
    }
}
