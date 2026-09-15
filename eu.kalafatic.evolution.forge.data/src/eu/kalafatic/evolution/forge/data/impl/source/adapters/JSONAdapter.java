package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal source adapter for structured JSON dataset files.
 */
public class JSONAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".json") && !path.endsWith(".jsonl") && !path.endsWith(".jsonl.gz") || "JSON".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "JSONAdapter", false, 0, "json", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "JSONAdapter", exists, size, "json", exists, exists ? "JSON Dataset File" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [JSONAdapter] File not found: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing source JSON: " + file.getName());
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) return samples;

        if (content.startsWith("[")) {
            JSONArray arr = new JSONArray(content);
            for (int i = 0; i < arr.length(); i++) {
                if (context.isCancelled()) break;
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    NormalizedSample s = JSONLAdapter.parseJsonObject(obj, file.getName());
                    if (s != null) samples.add(s);
                }
            }
        } else if (content.startsWith("{")) {
            JSONObject root = new JSONObject(content);
            JSONArray array = root.optJSONArray("data");
            if (array == null) array = root.optJSONArray("samples");
            if (array == null) array = root.optJSONArray("rows");

            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    if (context.isCancelled()) break;
                    JSONObject obj = array.optJSONObject(i);
                    if (obj != null) {
                        NormalizedSample s = JSONLAdapter.parseJsonObject(obj, file.getName());
                        if (s != null) samples.add(s);
                    }
                }
            } else {
                NormalizedSample s = JSONLAdapter.parseJsonObject(root, file.getName());
                if (s != null) samples.add(s);
            }
        }

        context.log("[forge.dataset] JSON Parsed valid samples: " + samples.size());
        return samples;
    }
}
