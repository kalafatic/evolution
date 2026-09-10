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

        String rawRepo = config.getRepository();
        String repo = rawRepo;
        if ("wikitext".equalsIgnoreCase(rawRepo)) {
            repo = "Salesforce/wikitext";
        }
        discoverSplits(repo);
        fetchNextChunk();
    }

    private List<String[]> availableSplits = new ArrayList<>(); // Pairs of [configName, splitName]
    private int currentSplitIndex = 0;

    private boolean isBoundsExceeded() {
        if (config.getMaxSamples() > 0 && stats.getTotalSamplesRead() >= config.getMaxSamples()) {
            return true;
        }
        if (config.getMaxTokens() > 0 && stats.getEstimatedTokens() >= config.getMaxTokens()) {
            return true;
        }
        // Stop only when accepted usable content bytes meet or exceed maxBytes requirement.
        if (config.getMaxBytes() > 0 && stats.getAcceptedBytes() >= config.getMaxBytes()) {
            return true;
        }
        return false;
    }

    private void discoverSplits(String repo) {
        if (repo.startsWith("http://") || repo.startsWith("https://")) return;
        try {
            String targetUrl = "https://datasets-server.huggingface.co/splits?dataset=" + repo;
            URL url = URI.create(targetUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "EVO-Forge-Client/2.6");

            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject root = new JSONObject(sb.toString());
                    if (root.has("splits")) {
                        JSONArray splits = root.getJSONArray("splits");
                        for (int i = 0; i < splits.length(); i++) {
                            JSONObject sObj = splits.getJSONObject(i);
                            String cfg = sObj.optString("config", "default");
                            String sp = sObj.optString("split", "train");
                            addSplitIfAbsent(cfg, sp);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (availableSplits.isEmpty()) {
            String cfg = config.getConfiguration() != null ? config.getConfiguration() : "default";
            String sp = config.getSplit() != null ? config.getSplit() : "train";
            addSplitIfAbsent(cfg, sp);
            if ("train".equalsIgnoreCase(sp)) {
                addSplitIfAbsent(cfg, "validation");
                addSplitIfAbsent(cfg, "test");
            }
        }
    }

    private void addSplitIfAbsent(String cfg, String sp) {
        for (String[] pair : availableSplits) {
            if (pair[0].equals(cfg) && pair[1].equals(sp)) {
                return;
            }
        }
        availableSplits.add(new String[] { cfg, sp });
    }

    private void fetchNextChunk() throws IOException {
        currentChunk.clear();
        currentChunkIndex = 0;

        if (isBoundsExceeded()) {
            endOfStream = true;
            return;
        }

        String rawRepo = config.getRepository();
        String configName = config.getConfiguration();
        String split = config.getSplit() != null ? config.getSplit() : "train";

        // Resolve HF dataset aliases for standard repos
        String repo = rawRepo;
        String cfg = (configName != null && !configName.trim().isEmpty()) ? configName : "default";
        if ("wikitext".equalsIgnoreCase(rawRepo)) {
            repo = "Salesforce/wikitext";
            if ("default".equals(cfg) || "train".equals(cfg)) {
                cfg = "wikitext-2-v1";
            }
        }

        String targetUrl;
        if (repo.startsWith("http://") || repo.startsWith("https://")) {
            targetUrl = repo;
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

        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            throw new IOException("Failed to fetch Hugging Face dataset from " + targetUrl + ". HTTP Status: " + status + " (" + conn.getResponseMessage() + ")");
        }

        try (InputStream in = stream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            if (status < 200 || status >= 300) {
                StringBuilder errSb = new StringBuilder();
                String errLine;
                while ((errLine = reader.readLine()) != null) errSb.append(errLine);
                String errMsg = errSb.toString();
                try {
                    JSONObject errJson = new JSONObject(errMsg);
                    if (errJson.has("error")) errMsg = errJson.getString("error");
                } catch (Exception ignored) {}
                throw new IOException("Hugging Face API Error (HTTP " + status + "): " + errMsg);
            }

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
                if (root.has("error")) {
                    throw new IOException("Hugging Face API returned error: " + root.getString("error"));
                }
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
                        // Check if another split is available to advance
                        if (!availableSplits.isEmpty() && currentSplitIndex < availableSplits.size() - 1) {
                            currentSplitIndex++;
                            currentOffset = 0;
                            String[] nextSplit = availableSplits.get(currentSplitIndex);
                            config.setConfiguration(nextSplit[0]);
                            config.setSplit(nextSplit[1]);
                            fetchNextChunk();
                            return;
                        } else {
                            endOfStream = true;
                        }
                    }
                } else {
                    if (!availableSplits.isEmpty() && currentSplitIndex < availableSplits.size() - 1) {
                        currentSplitIndex++;
                        currentOffset = 0;
                        String[] nextSplit = availableSplits.get(currentSplitIndex);
                        config.setConfiguration(nextSplit[0]);
                        config.setSplit(nextSplit[1]);
                        fetchNextChunk();
                        return;
                    } else {
                        endOfStream = true;
                    }
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
        if (row == null) return null;

        // 1. Check for messages array (e.g., UltraChat, OpenAssistant, ShareGPT, Llama-3-Instruct)
        if (row.has("messages")) {
            detectedSchemaInfo = "messages: JSONArray[{role, content}]";
            JSONArray msgsArray = row.optJSONArray("messages");
            if (msgsArray != null && msgsArray.length() > 0) {
                List<NormalizedSample.Message> msgList = parseMessagesArray(msgsArray, "role", "content");
                if (!msgList.isEmpty()) {
                    return NormalizedSample.createChatSample(msgList, getSourceName());
                }
            }
        }

        // 2. Check for conversations array (e.g., ShareGPT)
        if (row.has("conversations")) {
            detectedSchemaInfo = "conversations: JSONArray[{from/role, value/content}]";
            JSONArray convArray = row.optJSONArray("conversations");
            if (convArray != null && convArray.length() > 0) {
                List<NormalizedSample.Message> msgList = parseMessagesArray(convArray, "from", "value");
                if (msgList.isEmpty()) {
                    msgList = parseMessagesArray(convArray, "role", "content");
                }
                if (!msgList.isEmpty()) {
                    return NormalizedSample.createChatSample(msgList, getSourceName());
                }
            }
        }

        // 3. Instruction / Input / Output or Response
        if (row.has("instruction") || row.has("response") || row.has("output")) {
            detectedSchemaInfo = "instruction / input / output";
            String inst = row.optString("instruction", "");
            String input = row.optString("input", "");
            String resp = row.optString("response", row.optString("output", ""));
            if (!input.trim().isEmpty()) {
                inst = inst + "\n\nContext:\n" + input.trim();
            }
            if (!inst.trim().isEmpty() || !resp.trim().isEmpty()) {
                return NormalizedSample.createInstructionSample(inst.trim(), resp.trim(), getSourceName());
            }
        }

        // 4. Prompt / Response or Prompt / Completion
        if (row.has("prompt") && (row.has("response") || row.has("completion") || row.has("chosen"))) {
            detectedSchemaInfo = "prompt / response";
            String p = row.optString("prompt", "");
            String r = row.optString("response", row.optString("completion", row.optString("chosen", "")));
            if (!p.trim().isEmpty() || !r.trim().isEmpty()) {
                return NormalizedSample.createInstructionSample(p.trim(), r.trim(), getSourceName());
            }
        }

        // 5. Question / Answer
        if (row.has("question") && row.has("answer")) {
            detectedSchemaInfo = "question / answer";
            String q = row.optString("question", "");
            String a = row.optString("answer", "");
            return NormalizedSample.createInstructionSample("Q: " + q.trim(), a.trim(), getSourceName());
        }

        // 6. Direct text or content
        if (row.has("text")) {
            detectedSchemaInfo = "text: string";
            String text = row.optString("text", null);
            if (text != null && !text.trim().isEmpty()) {
                return NormalizedSample.createTextSample(text.trim(), getSourceName());
            }
        }
        if (row.has("content")) {
            detectedSchemaInfo = "content: string";
            String content = row.optString("content", null);
            if (content != null && !content.trim().isEmpty()) {
                return NormalizedSample.createTextSample(content.trim(), getSourceName());
            }
        }

        // 7. Chosen (for DPO / preference datasets)
        if (row.has("chosen")) {
            detectedSchemaInfo = "chosen: string/object";
            Object chosenObj = row.get("chosen");
            if (chosenObj instanceof String s && !s.trim().isEmpty()) {
                return NormalizedSample.createTextSample(s.trim(), getSourceName());
            } else if (chosenObj instanceof JSONArray arr) {
                List<NormalizedSample.Message> msgList = parseMessagesArray(arr, "role", "content");
                if (!msgList.isEmpty()) {
                    return NormalizedSample.createChatSample(msgList, getSourceName());
                }
            }
        }

        // 8. Fallback key inspection
        List<String> keys = new ArrayList<>(row.keySet());
        detectedSchemaInfo = "keys: " + keys;
        for (String key : keys) {
            Object val = row.get(key);
            if (val instanceof String s && s.trim().length() > 5) {
                return NormalizedSample.createTextSample(s.trim(), getSourceName());
            } else if (val instanceof JSONArray arr && arr.length() > 0) {
                List<NormalizedSample.Message> msgList = parseMessagesArray(arr, "role", "content");
                if (!msgList.isEmpty()) {
                    return NormalizedSample.createChatSample(msgList, getSourceName());
                }
            }
        }

        return null;
    }

    private List<NormalizedSample.Message> parseMessagesArray(JSONArray arr, String roleKey, String contentKey) {
        List<NormalizedSample.Message> msgList = new ArrayList<>();
        if (arr == null) return msgList;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject msgObj = arr.optJSONObject(i);
            if (msgObj != null) {
                String role = msgObj.optString(roleKey, msgObj.optString("role", msgObj.optString("from", "user")));
                String content = msgObj.optString(contentKey, msgObj.optString("content", msgObj.optString("value", "")));
                if (!content.trim().isEmpty()) {
                    if ("human".equalsIgnoreCase(role) || "user".equalsIgnoreCase(role)) role = "user";
                    else if ("gpt".equalsIgnoreCase(role) || "assistant".equalsIgnoreCase(role)) role = "assistant";
                    else if ("system".equalsIgnoreCase(role)) role = "system";

                    msgList.add(new NormalizedSample.Message(role, content.trim()));
                }
            }
        }
        return msgList;
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
