package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.api.source.ResolvedSource;
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
 * dynamic schema detection, multi-split traversal, full chunk-level accounting, and resilient failure recovery.
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
    private String resolvedConfig = null;
    private String resolvedSplit = null;

    // Stream & Chunk Level Accounting
    private long totalRowsFetched = 0;
    private long totalSamplesExtracted = 0;
    private long totalExtractionRejections = 0;
    private long totalRawBytesDownloaded = 0;

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
    public ResolvedSource preflight() {
        String rawRepo = config.getRepository();
        String requestedSplit = config.getSplit() != null ? config.getSplit() : "train";
        String requestedCfg = config.getConfiguration();

        String repo = rawRepo;
        if ("wikitext".equalsIgnoreCase(rawRepo)) {
            repo = "Salesforce/wikitext";
        } else if ("fineweb".equalsIgnoreCase(rawRepo) || "huggingfacefw/fineweb".equalsIgnoreCase(rawRepo) || "huggingfacefw/fineWeb".equalsIgnoreCase(rawRepo)) {
            repo = "HuggingFaceFW/fineweb";
        }

        String runId = config != null ? config.getRunId() : null;
        String runTag = runId != null && !runId.trim().isEmpty() ? runId : "UNKNOWN";

        if (repo.startsWith("http://") || repo.startsWith("https://")) {
            ResolvedSource res = new ResolvedSource("HUGGING_FACE", repo, requestedSplit, requestedSplit, "url", "main", repo, "raw", 0L, true, true, null);
            res.logPreflight(runId);
            return res;
        }

        try {
            String splitsUrl = "https://datasets-server.huggingface.co/splits?dataset=" + repo;
            System.out.printf("[HF-ACQ][run=%s][HTTP]\nrepository=%s\nconfig=%s\nsplit=%s\nrevision=%s\noffset=%d\nlimit=%d\n",
                    runTag, repo, requestedCfg != null ? requestedCfg : "default", requestedSplit, "main", 0, 100);

            long httpStartMs = System.currentTimeMillis();
            DownloadRequest req = new DownloadRequest(splitsUrl);
            DownloadResult result = downloader.download(req);
            long httpDurationMs = System.currentTimeMillis() - httpStartMs;

            System.out.printf("[HF-ACQ][run=%s][HTTP-RESULT]\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nhttpStatus=%d\nresponseBytes=%d\ndurationMs=%d\n",
                    runTag, 0, 100, 0, result.getStatusCode(), result.getDownloadedBytes(), httpDurationMs);

            if (!result.isSuccess()) {
                ResolvedSource res = new ResolvedSource("HUGGING_FACE", repo, requestedSplit, requestedSplit, requestedCfg, "main", splitsUrl, "json", 0L, false, false, "HTTP " + result.getStatusCode() + ": " + result.getStatusMessage());
                res.logPreflight(runId);
                return res;
            }

            JSONObject root = new JSONObject(result.getContentText());
            if (root.has("error")) {
                String err = root.getString("error");
                ResolvedSource res = new ResolvedSource("HUGGING_FACE", repo, requestedSplit, requestedSplit, requestedCfg, "main", splitsUrl, "json", 0L, false, false, err);
                res.logPreflight(runId);
                return res;
            }

            boolean configFound = false;
            boolean splitFound = false;
            String matchingCfg = null;
            long estimatedSize = 0L;

            if (root.has("splits")) {
                JSONArray splits = root.getJSONArray("splits");
                for (int i = 0; i < splits.length(); i++) {
                    JSONObject sObj = splits.getJSONObject(i);
                    String cfg = sObj.optString("config", "default");
                    String sp = sObj.optString("split", "train");

                    if (sObj.has("num_bytes")) {
                        estimatedSize = Math.max(estimatedSize, sObj.optLong("num_bytes", -1L));
                    }

                    if (requestedCfg != null && !requestedCfg.trim().isEmpty() && !requestedCfg.equalsIgnoreCase("default")) {
                        if (cfg.equalsIgnoreCase(requestedCfg)) {
                            configFound = true;
                            matchingCfg = cfg;
                            if (sp.equalsIgnoreCase(requestedSplit)) {
                                splitFound = true;
                                break;
                            }
                        }
                    } else {
                        if ("Salesforce/wikitext".equalsIgnoreCase(repo) && cfg.contains("103")) {
                            matchingCfg = cfg;
                        } else if (matchingCfg == null) {
                            matchingCfg = cfg;
                        }
                        configFound = true;
                        if (sp.equalsIgnoreCase(requestedSplit)) {
                            splitFound = true;
                            if (matchingCfg != null) break;
                        }
                    }
                }
            }

            if (matchingCfg != null) {
                this.resolvedConfig = matchingCfg;
            }
            this.resolvedSplit = requestedSplit;

            if (!splitFound) {
                String reason = !configFound ? ("Config not found: " + requestedCfg) : ("Split not found in repository: " + requestedSplit);
                ResolvedSource res = new ResolvedSource("HUGGING_FACE", repo, requestedSplit, requestedSplit, matchingCfg != null ? matchingCfg : requestedCfg, "main", splitsUrl, "json", estimatedSize, false, false, reason);
                res.logPreflight(runId);
                return res;
            }

            ResolvedSource res = new ResolvedSource("HUGGING_FACE", repo, requestedSplit, requestedSplit, this.resolvedConfig != null ? this.resolvedConfig : "default", "main", "https://datasets-server.huggingface.co/rows", "application/json", estimatedSize, true, true, null);
            res.logPreflight(runId);
            return res;

        } catch (Exception ex) {
            ResolvedSource res = new ResolvedSource("HUGGING_FACE", repo, requestedSplit, requestedSplit, requestedCfg, "main", "https://datasets-server.huggingface.co/splits", "json", 0L, false, false, ex.getMessage());
            res.logPreflight(runId);
            return res;
        }
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

    public long getTotalRowsFetched() { return totalRowsFetched; }
    public long getTotalSamplesExtracted() { return totalSamplesExtracted; }
    public long getTotalExtractionRejections() { return totalExtractionRejections; }
    public long getTotalRawBytesDownloaded() { return totalRawBytesDownloaded; }

    @Override
    public void initialize() throws Exception {
        if (initialized) return;
        initialized = true;

        String rawRepo = config.getRepository();
        String repo = rawRepo;
        if ("wikitext".equalsIgnoreCase(rawRepo)) {
            repo = "Salesforce/wikitext";
        } else if ("fineweb".equalsIgnoreCase(rawRepo) || "huggingfacefw/fineweb".equalsIgnoreCase(rawRepo) || "huggingfacefw/fineWeb".equalsIgnoreCase(rawRepo)) {
            repo = "HuggingFaceFW/fineweb";
        }
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

    private void fetchNextChunk() throws IOException {
        currentChunk.clear();
        currentChunkIndex = 0;

        if (isBoundsExceeded()) {
            endOfStream = true;
            return;
        }

        String rawRepo = config.getRepository();
        String requestedSplit = config.getSplit() != null ? config.getSplit() : "train";
        String requestedCfg = config.getConfiguration();

        String repo = rawRepo;
        if ("wikitext".equalsIgnoreCase(rawRepo)) {
            repo = "Salesforce/wikitext";
        } else if ("fineweb".equalsIgnoreCase(rawRepo) || "huggingfacefw/fineweb".equalsIgnoreCase(rawRepo) || "huggingfacefw/fineWeb".equalsIgnoreCase(rawRepo)) {
            repo = "HuggingFaceFW/fineweb";
        }

        String cfg = resolvedConfig;
        if (cfg == null) {
            cfg = (requestedCfg != null && !requestedCfg.trim().isEmpty()) ? requestedCfg : "default";
            if ("Salesforce/wikitext".equalsIgnoreCase(repo) && ("default".equalsIgnoreCase(cfg) || "wikitext".equalsIgnoreCase(cfg))) {
                cfg = "wikitext-103-v1";
            }
            resolvedConfig = cfg;
        }

        String split = resolvedSplit != null ? resolvedSplit : requestedSplit;
        resolvedSplit = split;

        String targetUrl;
        if (repo.startsWith("http://") || repo.startsWith("https://")) {
            targetUrl = repo;
        } else {
            targetUrl = "https://datasets-server.huggingface.co/rows?dataset=" + repo + "&config=" + cfg + "&split=" + split + "&offset=" + currentOffset + "&length=100";
        }

        String runId = config != null ? config.getRunId() : null;
        String runTag = runId != null && !runId.trim().isEmpty() ? runId : "UNKNOWN";
        String rev = config != null && config.getRevision() != null ? config.getRevision() : "main";

        int requestedLength = 100;
        System.out.printf("[HF-ACQ][run=%s][HTTP]\nrepository=%s\nconfig=%s\nsplit=%s\nrevision=%s\noffset=%d\nlimit=%d\n",
                runTag, repo, cfg, split, rev, currentOffset, requestedLength);

        long httpStartMs = System.currentTimeMillis();
        DownloadRequest req = new DownloadRequest(targetUrl);
        DownloadResult result;
        try {
            result = downloader.download(req);
        } catch (IOException e) {
            long httpDurationMs = System.currentTimeMillis() - httpStartMs;
            System.out.printf("[HF-ACQ][run=%s][HTTP-RESULT]\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nhttpStatus=%d\nresponseBytes=%d\ndurationMs=%d\n",
                    runTag, currentOffset, requestedLength, 0, 500, 0, httpDurationMs);
            lastError = e.getMessage();
            endOfStream = true;
            return;
        }
        long httpDurationMs = System.currentTimeMillis() - httpStartMs;

        String body = result.getContentText();
        long chunkBodyBytes = result.getDownloadedBytes() > 0 ? result.getDownloadedBytes() : body.getBytes(StandardCharsets.UTF_8).length;
        totalRawBytesDownloaded += chunkBodyBytes;

        int currentPage = (int) (currentOffset / requestedLength);
        long pageOffset = currentOffset;

        if (targetUrl.contains("raw.githubusercontent.com") || targetUrl.endsWith(".txt") || targetUrl.endsWith(".raw")) {
            detectedSchemaInfo = "text: string (raw lines)";
            int fetchedRows = 0;
            int extractedInChunk = 0;
            int rejectedInChunk = 0;
            long chunkExtractedBytes = 0;
            try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
                String line;
                long skipped = 0;
                while (skipped < currentOffset && (reader.readLine()) != null) {
                    skipped++;
                }

                while ((line = reader.readLine()) != null && fetchedRows < requestedLength) {
                    fetchedRows++;
                    totalRowsFetched++;
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("=") && trimmed.length() > 5) {
                        NormalizedSample sample = NormalizedSample.createTextSample(trimmed, getSourceName());
                        currentChunk.add(sample);
                        extractedInChunk++;
                        totalSamplesExtracted++;
                        byte[] fullTextBytes = sample.toFullText().getBytes(StandardCharsets.UTF_8);
                        chunkExtractedBytes += fullTextBytes.length;
                    } else {
                        rejectedInChunk++;
                        totalExtractionRejections++;
                    }
                }
                currentOffset += fetchedRows;
                if (fetchedRows == 0) {
                    endOfStream = true;
                }
            }
            stats.addDownloadedBytes(chunkBodyBytes);
            stats.addExtractedBytes(chunkExtractedBytes);

            System.out.printf("[HF-ACQ][run=%s][HTTP-RESULT]\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nhttpStatus=%d\nresponseBytes=%d\ndurationMs=%d\n",
                    runTag, pageOffset, requestedLength, fetchedRows, result.getStatusCode(), chunkBodyBytes, httpDurationMs);

            boolean continuation = fetchedRows > 0 && !endOfStream;
            String reason = continuation ? "CONTINUE" : (fetchedRows == 0 ? "NO_MORE_ROWS" : "END_OF_DATASET");
            System.out.printf("[HF-ACQ][run=%s][PAGE]\npage=%d\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nextractedSamples=%d\nrejectedSamples=%d\nnextOffset=%d\ncontinuation=%s\nreason=%s\n",
                    runTag, currentPage, pageOffset, requestedLength, fetchedRows, extractedInChunk, rejectedInChunk, currentOffset, continuation ? "YES" : "NO", reason);

            long targetUsableBytes = config.getMaxBytes();
            long remainingUsableBytes = targetUsableBytes > 0 ? Math.max(0, targetUsableBytes - stats.getAcceptedBytes()) : 0;

            System.out.printf("[HF-TRACE] repository=%s config=%s split=%s offset=%d pageFetchedRows=%d pageExtractedSamples=%d pageRejectedSamples=%d pageRawBytes=%d pageExtractedBytes=%d cumulativeRowsFetched=%d cumulativeSamplesExtracted=%d cumulativeExtractionRejections=%d cumulativeSamplesAccepted=%d cumulativeAcceptedBytes=%d cumulativeRawBytesDownloaded=%d currentChunkSize=%d currentChunkIndex=%d sourceExhausted=%b targetUsableBytes=%d remainingUsableBytes=%d\n",
                    repo, cfg, split, pageOffset, fetchedRows, extractedInChunk, rejectedInChunk, chunkBodyBytes, chunkExtractedBytes,
                    totalRowsFetched, totalSamplesExtracted, totalExtractionRejections, stats.getAcceptedRecords(), stats.getAcceptedBytes(),
                    totalRawBytesDownloaded, currentChunk.size(), currentChunkIndex, endOfStream, targetUsableBytes, remainingUsableBytes);
        } else {
            try {
                JSONObject root = new JSONObject(body);
                if (root.has("error")) {
                    lastError = root.getString("error");
                    endOfStream = true;
                    System.out.printf("[HF-ACQ][run=%s][HTTP-RESULT]\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nhttpStatus=%d\nresponseBytes=%d\ndurationMs=%d\n",
                            runTag, pageOffset, requestedLength, 0, result.getStatusCode(), chunkBodyBytes, httpDurationMs);
                    System.out.printf("[HF-ACQ][run=%s][PAGE]\npage=%d\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nextractedSamples=0\nrejectedSamples=0\nnextOffset=%d\ncontinuation=NO\nreason=SOURCE_FAILURE\n",
                            runTag, currentPage, pageOffset, requestedLength, 0, currentOffset);
                    return;
                }
                if (root.has("rows")) {
                    JSONArray rows = root.getJSONArray("rows");
                    int fetchedRows = rows.length();
                    int extractedInChunk = 0;
                    int rejectedInChunk = 0;
                    long chunkExtractedBytes = 0;

                    System.out.printf("[HF-ACQ][run=%s][HTTP-RESULT]\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nhttpStatus=%d\nresponseBytes=%d\ndurationMs=%d\n",
                            runTag, pageOffset, requestedLength, fetchedRows, result.getStatusCode(), chunkBodyBytes, httpDurationMs);

                    for (int i = 0; i < rows.length(); i++) {
                        totalRowsFetched++;
                        long globalRowIndex = totalRowsFetched;
                        JSONObject rowObj = rows.getJSONObject(i).optJSONObject("row");

                        boolean isSampledRow = (globalRowIndex <= 5 || globalRowIndex % 100 == 0);

                        if (rowObj != null) {
                            NormalizedSample sample = extractSampleFromRow(rowObj);
                            if (sample != null) {
                                currentChunk.add(sample);
                                extractedInChunk++;
                                totalSamplesExtracted++;
                                byte[] fullTextBytes = sample.toFullText().getBytes(StandardCharsets.UTF_8);
                                chunkExtractedBytes += fullTextBytes.length;

                                if (isSampledRow) {
                                    System.out.printf("[HF-ACQ][run=%s][ROW]\npage=%d\nrowIndex=%d\nglobalRowIndex=%d\nfields=%s\ntextField=%s\nrawTextLength=%d\nrawTextBytes=%d\ntextPresent=%b\n",
                                            runTag, currentPage, i, globalRowIndex, rowObj.keySet(), detectedSchemaInfo, sample.toFullText().length(), fullTextBytes.length, !sample.toFullText().isEmpty());
                                }
                            } else {
                                rejectedInChunk++;
                                totalExtractionRejections++;
                                if (isSampledRow) {
                                    System.out.printf("[HF-ACQ][run=%s][ROW]\npage=%d\nrowIndex=%d\nglobalRowIndex=%d\nfields=%s\ntextField=NONE\nrawTextLength=0\nrawTextBytes=0\ntextPresent=false\n",
                                            runTag, currentPage, i, globalRowIndex, rowObj.keySet());
                                }
                            }
                        } else {
                            rejectedInChunk++;
                            totalExtractionRejections++;
                            if (isSampledRow) {
                                System.out.printf("[HF-ACQ][run=%s][ROW]\npage=%d\nrowIndex=%d\nglobalRowIndex=%d\nfields=[]\ntextField=NONE\nrawTextLength=0\nrawTextBytes=0\ntextPresent=false\n",
                                        runTag, currentPage, i, globalRowIndex);
                            }
                        }
                    }

                    stats.addDownloadedBytes(chunkBodyBytes);
                    stats.addExtractedBytes(chunkExtractedBytes);

                    if (rows.length() == 0) {
                        endOfStream = true;
                    }

                    currentOffset += rows.length();

                    boolean continuation = fetchedRows > 0 && !endOfStream;
                    String reason = continuation ? "CONTINUE" : (fetchedRows == 0 ? "NO_MORE_ROWS" : "END_OF_DATASET");

                    System.out.printf("[HF-ACQ][run=%s][PAGE]\npage=%d\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nextractedSamples=%d\nrejectedSamples=%d\nnextOffset=%d\ncontinuation=%s\nreason=%s\n",
                            runTag, currentPage, pageOffset, requestedLength, fetchedRows, extractedInChunk, rejectedInChunk, currentOffset, continuation ? "YES" : "NO", reason);

                    long targetUsableBytes = config.getMaxBytes();
                    long remainingUsableBytes = targetUsableBytes > 0 ? Math.max(0, targetUsableBytes - stats.getAcceptedBytes()) : 0;

                    System.out.printf("[HF-TRACE] repository=%s config=%s split=%s offset=%d pageFetchedRows=%d pageExtractedSamples=%d pageRejectedSamples=%d pageRawBytes=%d pageExtractedBytes=%d cumulativeRowsFetched=%d cumulativeSamplesExtracted=%d cumulativeExtractionRejections=%d cumulativeSamplesAccepted=%d cumulativeAcceptedBytes=%d cumulativeRawBytesDownloaded=%d currentChunkSize=%d currentChunkIndex=%d sourceExhausted=%b targetUsableBytes=%d remainingUsableBytes=%d\n",
                            repo, cfg, split, pageOffset, fetchedRows, extractedInChunk, rejectedInChunk, chunkBodyBytes, chunkExtractedBytes,
                            totalRowsFetched, totalSamplesExtracted, totalExtractionRejections, stats.getAcceptedRecords(), stats.getAcceptedBytes(),
                            totalRawBytesDownloaded, currentChunk.size(), currentChunkIndex, endOfStream, targetUsableBytes, remainingUsableBytes);
                } else {
                    endOfStream = true;
                    System.out.printf("[HF-ACQ][run=%s][HTTP-RESULT]\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nhttpStatus=%d\nresponseBytes=%d\ndurationMs=%d\n",
                            runTag, pageOffset, requestedLength, 0, result.getStatusCode(), chunkBodyBytes, httpDurationMs);
                    System.out.printf("[HF-ACQ][run=%s][PAGE]\npage=%d\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nextractedSamples=0\nrejectedSamples=0\nnextOffset=%d\ncontinuation=NO\nreason=NO_MORE_ROWS\n",
                            runTag, currentPage, pageOffset, requestedLength, 0, currentOffset);
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                endOfStream = true;
                System.out.printf("[HF-ACQ][run=%s][PAGE]\npage=%d\noffset=%d\nrequestedRows=%d\nreceivedRows=%d\nextractedSamples=0\nrejectedSamples=0\nnextOffset=%d\ncontinuation=NO\nreason=SOURCE_FAILURE\n",
                        runTag, currentPage, pageOffset, requestedLength, 0, currentOffset);
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
        if (row.has("instruction") || row.has("response") || row.has("output") || row.has("input")) {
            detectedSchemaInfo = "instruction / input / output";
            String inst = row.optString("instruction", row.optString("input", ""));
            String input = row.has("instruction") ? row.optString("input", "") : "";
            String resp = row.optString("response", row.optString("output", ""));
            if (!input.trim().isEmpty() && !inst.equals(input)) {
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

        // 6. Direct text schema keys
        String[] textKeys = {"text", "content", "sentence", "document", "passage", "article", "body", "summary", "dialogue", "context", "code", "solution", "raw"};
        for (String key : textKeys) {
            if (row.has(key)) {
                detectedSchemaInfo = key + ": string";
                String textVal = row.optString(key, null);
                if (textVal != null && !textVal.trim().isEmpty()) {
                    return NormalizedSample.createTextSample(textVal.trim(), getSourceName());
                }
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

        // 8. Nested JSONObject inspection and Fallback key inspection
        List<String> keys = new ArrayList<>(row.keySet());
        detectedSchemaInfo = "keys: " + keys;
        for (String key : keys) {
            Object val = row.get(key);
            if (val instanceof String s && !s.trim().isEmpty()) {
                return NormalizedSample.createTextSample(s.trim(), getSourceName());
            } else if (val instanceof JSONObject nestedObj) {
                NormalizedSample nestedSample = extractSampleFromRow(nestedObj);
                if (nestedSample != null) {
                    return nestedSample;
                }
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
        while (currentChunkIndex >= currentChunk.size() && !endOfStream) {
            try {
                fetchNextChunk();
            } catch (Exception e) {
                lastError = e.getMessage();
                endOfStream = true;
                return false;
            }
        }
        return currentChunkIndex < currentChunk.size();
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
        stats.addAcceptedBytes(sampleBytes);
        stats.addEstimatedTokens(sampleTokens);

        return sample;
    }

    @Override
    public void close() {
        endOfStream = true;
        currentChunk.clear();
    }
}
