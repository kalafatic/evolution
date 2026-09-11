package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.downloader.HuggingFaceDownloader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Hugging Face Dataset Source supporting real public dataset streams via Hugging Face rows API or raw URL endpoints with pagination,
 * dynamic schema detection, multi-split traversal, and resilient failure recovery.
 */
public class HuggingFaceDatasetSource implements DatasetSource {

    private final DatasetSourceConfig config;
    private final DataDownloader downloader;
    private final DatasetSourceStats stats = new DatasetSourceStats();
    private final List<NormalizedSample> currentChunk = new ArrayList<>();
    private int currentChunkIndex = 0;
    private long currentOffset = 0;
    private boolean endOfStream = false;
    private boolean initialized = false;
    private String detectedSchemaInfo = "UNKNOWN";
    private String lastError = null;

    private final List<String[]> availableSplits = new ArrayList<>(); // Pairs of [configName, splitName]
    private int currentSplitIndex = 0;

    public HuggingFaceDatasetSource(String repo) {
        this(new DatasetSourceConfig("HUGGING_FACE", repo != null ? repo : "wikitext"), new HuggingFaceDownloader());
    }

    public HuggingFaceDatasetSource(DatasetSourceConfig config) {
        this(config, new HuggingFaceDownloader());
    }

    public HuggingFaceDatasetSource(DatasetSourceConfig config, DataDownloader downloader) {
        this.config = config != null ? config : new DatasetSourceConfig("HUGGING_FACE", "wikitext");
        this.downloader = downloader != null ? downloader : new HuggingFaceDownloader();
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

    public String getLastError() {
        return lastError;
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
        try {
            fetchNextChunk();
        } catch (Exception ex) {
            lastError = ex.getMessage();
            System.err.println("[HF DATASET SOURCE] Initial chunk fetch failed for " + repo + ": " + ex.getMessage());
            endOfStream = true;
        }
    }

    private boolean isBoundsExceeded() {
        if (config.getMaxSamples() > 0 && stats.getTotalSamplesRead() >= config.getMaxSamples()) {
            return true;
        }
        if (config.getMaxTokens() > 0 && stats.getEstimatedTokens() >= config.getMaxTokens()) {
            return true;
        }
        if (config.getMaxBytes() > 0 && stats.getAcceptedBytes() >= config.getMaxBytes()) {
            return true;
        }
        return false;
    }

    private void discoverSplits(String repo) {
        if (repo.startsWith("http://") || repo.startsWith("https://")) return;
        try {
            String targetUrl = "https://datasets-server.huggingface.co/splits?dataset=" + repo;
            DownloadRequest req = new DownloadRequest(targetUrl);
            DownloadResult res = downloader.download(req);

            if (res.isSuccess()) {
                JSONObject root = new JSONObject(res.getContentText());
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

        // Prioritize train splits over validation/test and larger configs
        availableSplits.sort((a, b) -> {
            String cfgA = a[0];
            String spA = a[1];
            String cfgB = b[0];
            String spB = b[1];

            boolean isTrainA = "train".equalsIgnoreCase(spA);
            boolean isTrainB = "train".equalsIgnoreCase(spB);

            if (isTrainA != isTrainB) {
                return isTrainA ? -1 : 1;
            }

            boolean is103A = cfgA.contains("103");
            boolean is103B = cfgB.contains("103");
            if (is103A != is103B) {
                return is103A ? -1 : 1;
            }

            return 0;
        });
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

        String repo = rawRepo;
        String cfg = (configName != null && !configName.trim().isEmpty()) ? configName : "default";
        if ("wikitext".equalsIgnoreCase(rawRepo)) {
            repo = "Salesforce/wikitext";
            if ("default".equalsIgnoreCase(cfg) || "train".equalsIgnoreCase(cfg) || "wikitext".equalsIgnoreCase(cfg)) {
                cfg = "wikitext-103-v1";
            }
        }

        String targetUrl;
        if (repo.startsWith("http://") || repo.startsWith("https://")) {
            targetUrl = repo;
        } else {
            targetUrl = "https://datasets-server.huggingface.co/rows?dataset=" + repo + "&config=" + cfg + "&split=" + split + "&offset=" + currentOffset + "&length=100";
        }

        DownloadRequest req = new DownloadRequest(targetUrl);
        DownloadResult result;
        try {
            result = downloader.download(req);
        } catch (IOException e) {
            // If current split failed, try next split before giving up
            if (!availableSplits.isEmpty() && currentSplitIndex < availableSplits.size() - 1) {
                currentSplitIndex++;
                currentOffset = 0;
                String[] nextSplit = availableSplits.get(currentSplitIndex);
                config.setConfiguration(nextSplit[0]);
                config.setSplit(nextSplit[1]);
                fetchNextChunk();
                return;
            } else {
                lastError = e.getMessage();
                endOfStream = true;
                return;
            }
        }

        String body = result.getContentText();

        if (targetUrl.contains("raw.githubusercontent.com") || targetUrl.endsWith(".txt") || targetUrl.endsWith(".raw")) {
            detectedSchemaInfo = "text: string (raw lines)";
            try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
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
            }
        } else {
            try {
                JSONObject root = new JSONObject(body);
                if (root.has("error")) {
                    // Try next split if available
                    if (!availableSplits.isEmpty() && currentSplitIndex < availableSplits.size() - 1) {
                        currentSplitIndex++;
                        currentOffset = 0;
                        String[] nextSplit = availableSplits.get(currentSplitIndex);
                        config.setConfiguration(nextSplit[0]);
                        config.setSplit(nextSplit[1]);
                        fetchNextChunk();
                        return;
                    } else {
                        lastError = root.getString("error");
                        endOfStream = true;
                        return;
                    }
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
            } catch (Exception e) {
                lastError = e.getMessage();
                endOfStream = true;
            }
        }
    }

    private NormalizedSample extractSampleFromRow(JSONObject row) {
        if (row == null) return null;

        // 1. Check for messages array
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

        // 2. Check for conversations array
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

        // 7. Chosen
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
                lastError = e.getMessage();
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
