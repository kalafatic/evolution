package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal source adapter for tabular CSV dataset files.
 */
public class CSVAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".csv") || "CSV".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "CSVAdapter", false, 0, "csv", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "CSVAdapter", exists, size, "csv", exists, exists ? "CSV Dataset File" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [CSVAdapter] File not found: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing source CSV: " + file.getName());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) return samples;

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> colIndexMap = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                colIndexMap.put(headers.get(i).trim().toLowerCase(), i);
            }

            int promptCol = findIndex(colIndexMap, "instruction", "prompt", "input", "question");
            int responseCol = findIndex(colIndexMap, "response", "output", "answer", "completion");
            int textCol = findIndex(colIndexMap, "text", "content", "body", "article", "document");

            String line;
            while ((line = reader.readLine()) != null) {
                if (context.isCancelled()) break;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                List<String> values = parseCsvLine(trimmed);

                if (promptCol >= 0 && responseCol >= 0 && promptCol < values.size() && responseCol < values.size()) {
                    String p = values.get(promptCol).trim();
                    String r = values.get(responseCol).trim();
                    if (!p.isEmpty() && !r.isEmpty()) {
                        samples.add(NormalizedSample.createInstructionSample(p, r, file.getName()));
                    }
                } else if (textCol >= 0 && textCol < values.size()) {
                    String txt = values.get(textCol).trim();
                    if (!txt.isEmpty()) {
                        samples.add(NormalizedSample.createTextSample(txt, file.getName()));
                    }
                } else if (!values.isEmpty()) {
                    String fullRow = String.join(" ", values).trim();
                    if (!fullRow.isEmpty()) {
                        samples.add(NormalizedSample.createTextSample(fullRow, file.getName()));
                    }
                }
            }
        }

        context.log("[forge.dataset] CSV Parsed samples: " + samples.size());
        return samples;
    }

    private int findIndex(Map<String, Integer> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) return map.get(k);
        }
        return -1;
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }
}
