package eu.kalafatic.evolution.forge.data.api.artifact;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Reusable, persistent EVO Training Dataset Artifact (.evodata).
 * Zip-based container containing data.jsonl, metadata.json, profile.json, and report.txt.
 */
public class EvoDatasetArtifact {

    public static final String FORMAT_VERSION = "1.1";

    public enum Status {
        CREATING,
        PREPARING,
        VALIDATING,
        FINALIZING,
        READY,
        INSUFFICIENT_SOURCE_DATA,
        CANCELLED,
        FAILED,
        CORRUPTED
    }

    private File artifactFile;
    private String name;
    private Status status = Status.PREPARING;
    private DatasetSourceConfig sourceConfig;
    private DatasetSourceStats stats;
    private double validationSplitRatio = 0.02;
    private long totalTrainTokens = 0;
    private long totalValTokens = 0;
    private List<NormalizedSample> trainSamples = new ArrayList<>();
    private List<NormalizedSample> valSamples = new ArrayList<>();

    private String tokenizerType;
    private String tokenizerVersion;
    private int tokenizerVocabSize;
    private final List<String> tokenizerSpecialTokens = new ArrayList<>();
    private String loadDiagnostics = "";

    public EvoDatasetArtifact(File artifactFile) {
        this.artifactFile = artifactFile;
        if (artifactFile != null) {
            this.name = artifactFile.getName();
        }
    }

    public void setTokenizerIdentity(String type, String version, int vocabSize, List<String> specialTokens) {
        this.tokenizerType = type;
        this.tokenizerVersion = version;
        this.tokenizerVocabSize = vocabSize;
        this.tokenizerSpecialTokens.clear();
        if (specialTokens != null) {
            this.tokenizerSpecialTokens.addAll(specialTokens);
        }
    }

    public String getTokenizerType() { return tokenizerType; }
    public String getTokenizerVersion() { return tokenizerVersion; }
    public int getTokenizerVocabSize() { return tokenizerVocabSize; }
    public List<String> getTokenizerSpecialTokens() { return new ArrayList<>(tokenizerSpecialTokens); }

    /**
     * Non-empty when loading produced integrity/format warnings or the artifact
     * was detected as corrupted.
     */
    public String getLoadDiagnostics() { return loadDiagnostics; }

    /**
     * Validates that a training tokenizer is compatible with the tokenizer
     * identity declared by this dataset (when one is declared). A dataset without
     * a declared tokenizer is treated as raw text (tokenizer-agnostic) and passes.
     *
     * @throws IllegalStateException when the declared tokenizer mismatches
     */
    public void validateTokenizerCompatibility(String tokenizerType, int vocabSize) {
        if (this.tokenizerType == null || this.tokenizerType.isEmpty()) {
            return; // no declared identity: dataset is raw text
        }
        if (!this.tokenizerType.equals(tokenizerType)) {
            throw new IllegalStateException(String.format(
                    "Tokenizer mismatch: dataset '%s' was prepared with tokenizer '%s' but training uses '%s'.",
                    name != null ? name : artifactFile.getName(), this.tokenizerType, tokenizerType));
        }
        if (this.tokenizerVocabSize > 0 && vocabSize != this.tokenizerVocabSize) {
            throw new IllegalStateException(String.format(
                    "Tokenizer vocabulary mismatch: dataset '%s' declares vocab size %d but training uses %d.",
                    name != null ? name : artifactFile.getName(), this.tokenizerVocabSize, vocabSize));
        }
    }

    public void save(List<NormalizedSample> allSamples, DatasetSourceConfig config, DatasetSourceStats stats, double valSplit) throws Exception {
        // Preserve terminal statuses set explicitly by the caller (e.g. CANCELLED
        // after an interrupted acquisition); only default the status when unset.
        if (this.status == null || this.status == Status.CREATING) {
            this.status = Status.PREPARING;
        }
        this.sourceConfig = config;
        this.stats = stats;
        this.validationSplitRatio = valSplit;

        if (artifactFile.getParentFile() != null) {
            artifactFile.getParentFile().mkdirs();
        }

        // Split samples after deduplication & prevent Train/Validation Leakage
        trainSamples.clear();
        valSamples.clear();
        totalTrainTokens = 0;
        totalValTokens = 0;

        List<NormalizedSample> samplesToSplit = new ArrayList<>(allSamples);
        java.util.Collections.shuffle(samplesToSplit, new java.util.Random(42));

        int valCount = (int) Math.round(samplesToSplit.size() * valSplit);
        if (samplesToSplit.size() >= 2 && valSplit > 0.0 && valCount == 0) {
            valCount = 1;
        }
        if (valCount >= samplesToSplit.size()) {
            valCount = Math.max(0, samplesToSplit.size() - 1);
        }
        int trainCount = samplesToSplit.size() - valCount;

        java.util.Set<String> trainHashes = new java.util.HashSet<>();
        java.util.Set<String> trainNormHashes = new java.util.HashSet<>();

        for (int i = 0; i < samplesToSplit.size(); i++) {
            NormalizedSample sample = samplesToSplit.get(i);
            if (i < trainCount) {
                trainSamples.add(sample);
                if (sample.getHash() != null) {
                    trainHashes.add(sample.getHash());
                }
                String normText = sample.toFullText().toLowerCase().replaceAll("[^a-z0-9]", "");
                if (!normText.isEmpty()) {
                    trainNormHashes.add(normText);
                }
                totalTrainTokens += sample.getTokenCount() > 0 ? sample.getTokenCount() : (sample.getCharCount() / 4);
            } else {
                // Check leakage against training set
                String sampleHash = sample.getHash();
                String normText = sample.toFullText().toLowerCase().replaceAll("[^a-z0-9]", "");
                boolean isLeaking = (sampleHash != null && trainHashes.contains(sampleHash)) ||
                                   (!normText.isEmpty() && trainNormHashes.contains(normText));
                if (isLeaking) {
                    if (stats != null) {
                        stats.incrementTrainValLeakage();
                    }
                    continue; // Exclude leaking validation sample
                }

                valSamples.add(sample);
                totalValTokens += sample.getTokenCount() > 0 ? sample.getTokenCount() : (sample.getCharCount() / 4);
            }
        }

        // Determine final status before serializing metadata and report
        if (this.status != Status.CANCELLED && this.status != Status.FAILED) {
            if (stats != null && stats.getRequestedUsableBytes() > 0 && stats.getAcceptedBytes() < stats.getRequestedUsableBytes()) {
                this.status = Status.INSUFFICIENT_SOURCE_DATA;
            } else {
                this.status = Status.READY;
            }
        }

        // Write to temporary archive first (transactional semantics)
        File tempFile = new File(artifactFile.getAbsolutePath() + ".tmp");
        long fixedTimestamp = 1704067200000L; // Deterministic zip entry timestamp

        long actualTrainingBytes = 0;
        long actualValidationBytes = 0;

        // Serialize payloads first so per-entry SHA-256 checksums can be recorded
        // in metadata.json (integrity layer, format v1.1).
        byte[] trainEntryBytes = serializeSamplesToJsonl(trainSamples);
        byte[] valEntryBytes = serializeSamplesToJsonl(valSamples);
        actualTrainingBytes = trainEntryBytes.length;
        actualValidationBytes = valEntryBytes.length;

        if (stats != null) {
            stats.setTrainingBytes(actualTrainingBytes);
            stats.setValidationBytes(actualValidationBytes);
        }

        byte[] reportEntryBytes = buildReportText().getBytes(StandardCharsets.UTF_8);
        String trainSha256 = sha256Hex(trainEntryBytes);
        String valSha256 = sha256Hex(valEntryBytes);
        String reportSha256 = sha256Hex(reportEntryBytes);
        byte[] metaEntryBytes = buildMetadataJson(trainSha256, valSha256, reportSha256,
                actualTrainingBytes, actualValidationBytes).getBytes(StandardCharsets.UTF_8);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {

            // 1. data.jsonl
            ZipEntry zeData = new ZipEntry("data.jsonl");
            zeData.setTime(fixedTimestamp);
            zos.putNextEntry(zeData);
            zos.write(trainEntryBytes);
            zos.closeEntry();

            // 2. val_data.jsonl
            ZipEntry zeVal = new ZipEntry("val_data.jsonl");
            zeVal.setTime(fixedTimestamp);
            zos.putNextEntry(zeVal);
            zos.write(valEntryBytes);
            zos.closeEntry();

            // 3. metadata.json
            ZipEntry zeMeta = new ZipEntry("metadata.json");
            zeMeta.setTime(fixedTimestamp);
            zos.putNextEntry(zeMeta);
            zos.write(metaEntryBytes);
            zos.closeEntry();

            // 4. report.txt
            ZipEntry zeReport = new ZipEntry("report.txt");
            zeReport.setTime(fixedTimestamp);
            zos.putNextEntry(zeReport);
            zos.write(reportEntryBytes);
            zos.closeEntry();
        }

        if (artifactFile.exists()) {
            artifactFile.delete();
        }
        if (!tempFile.renameTo(artifactFile)) {
            java.nio.file.Files.move(tempFile.toPath(), artifactFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // Post-serialization structural and semantic validation (single read pass;
        // load() verifies entry checksums and reports corruption via status).
        validateArtifact(artifactFile);
    }

    private static byte[] serializeSamplesToJsonl(List<NormalizedSample> samples) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            for (NormalizedSample s : samples) {
                byte[] lineBytes = (s.toJsonLine() + "\n").getBytes(StandardCharsets.UTF_8);
                baos.write(lineBytes);
            }
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to serialize dataset samples", e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void validateArtifact(File file) throws Exception {
        if (!file.exists() || file.length() == 0) {
            this.status = Status.FAILED;
            throw new IllegalStateException("Dataset artifact file is missing or 0 bytes.");
        }

        EvoDatasetArtifact loaded = load(file);
        if (loaded.getStatus() == Status.CORRUPTED) {
            this.status = Status.FAILED;
            throw new IllegalStateException("Validation failed: dataset artifact integrity check failed: " + loaded.getLoadDiagnostics());
        }
        if (loaded.getTrainSamples().isEmpty()) {
            this.status = Status.FAILED;
            throw new IllegalStateException("Validation failed: Dataset artifact contains 0 training samples.");
        }

        for (NormalizedSample s : loaded.getTrainSamples()) {
            if (s.toFullText().contains("Sample #") && s.toFullText().contains("Normalized sample content")) {
                this.status = Status.FAILED;
                throw new IllegalStateException("Validation failed: Artifact contains synthetic placeholder records.");
            }
        }
    }

    public static EvoDatasetArtifact load(File file) throws Exception {
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(file);
        artifact.trainSamples.clear();
        artifact.valSamples.clear();
        artifact.loadDiagnostics = "";
        Status loadedStatus = null;

        // Pass 1: metadata.json (integrity manifest, tokenizer identity, format version)
        Map<String, String> entrySha256 = new HashMap<>();
        String formatVersion = null;
        JSONObject metaJson = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || name.contains("..") || name.startsWith("/") || name.contains("\\..") || name.contains("../")) {
                    zis.closeEntry();
                    continue;
                }
                if ("metadata.json".equals(new File(name).getName())) {
                    byte[] entryBytes = zis.readAllBytes();
                    metaJson = new JSONObject(new String(entryBytes, StandardCharsets.UTF_8));
                }
                zis.closeEntry();
            }
        }

        if (metaJson != null) {
            try {
                formatVersion = metaJson.optString("formatVersion", null);
                String statusStr = metaJson.optString("status", null);
                if (statusStr != null) {
                    try {
                        loadedStatus = Status.valueOf(statusStr);
                    } catch (Exception ignored) {}
                }

                if (formatVersion != null && !formatVersion.isEmpty()) {
                    String major = formatVersion.contains(".") ? formatVersion.substring(0, formatVersion.indexOf('.')) : formatVersion;
                    if (!"1".equals(major)) {
                        artifact.status = Status.CORRUPTED;
                        artifact.loadDiagnostics = "Unsupported .evodata format version: " + formatVersion;
                    }
                }

                JSONObject filesObj = metaJson.optJSONObject("files");
                if (filesObj != null) {
                    for (String key : filesObj.keySet()) {
                        JSONObject fObj = filesObj.optJSONObject(key);
                        if (fObj != null) {
                            String sha = fObj.optString("sha256", "");
                            if (!sha.isEmpty()) {
                                entrySha256.put(key, sha);
                            }
                        }
                    }
                }

                JSONObject tokenizerObj = metaJson.optJSONObject("tokenizer");
                if (tokenizerObj != null) {
                    artifact.tokenizerType = tokenizerObj.optString("type", null);
                    artifact.tokenizerVersion = tokenizerObj.optString("version", null);
                    artifact.tokenizerVocabSize = tokenizerObj.optInt("vocabSize", 0);
                    JSONArray specialArr = tokenizerObj.optJSONArray("specialTokens");
                    if (specialArr != null) {
                        for (int i = 0; i < specialArr.length(); i++) {
                            artifact.tokenizerSpecialTokens.add(specialArr.optString(i));
                        }
                    }
                }

                artifact.stats = new DatasetSourceStats();
                JSONObject statsObj = metaJson.optJSONObject("statistics");

                long req = metaJson.optLong("requestedUsableBytes", statsObj != null ? statsObj.optLong("requestedUsableBytes", 0) : 0);
                long actual = metaJson.optLong("actualUsableBytes", metaJson.optLong("acceptedBytes", statsObj != null ? statsObj.optLong("acceptedBytes", 0) : 0));
                long dl = metaJson.optLong("downloadedBytes", 0);

                JSONArray sourcesArray = metaJson.optJSONArray("sources");
                if (sourcesArray != null && sourcesArray.length() > 0) {
                    JSONObject srcObj = sourcesArray.optJSONObject(0);
                    if (srcObj != null) {
                        if (dl == 0) dl = srcObj.optLong("downloadedBytes", 0);
                        if (actual == 0) actual = srcObj.optLong("acceptedBytes", 0);
                    }
                }

                artifact.stats.setRequestedUsableBytes(req);
                artifact.stats.setAcceptedBytes(actual);
                artifact.stats.setDownloadedBytes(dl);
                artifact.stats.setExtractedBytes(metaJson.optLong("extractedBytes", actual));
                artifact.stats.setRawContentBytes(metaJson.optLong("rawContentBytes", dl));
                artifact.stats.setRejectedBytes(metaJson.optLong("rejectedBytes", 0));
                artifact.stats.setDuplicateBytes(metaJson.optLong("duplicateBytes", 0));
                artifact.stats.setTrainingBytes(metaJson.optLong("trainingBytes", statsObj != null ? statsObj.optLong("totalTrainingBytes", 0) : 0));
                artifact.stats.setValidationBytes(metaJson.optLong("validationBytes", 0));

                long accRecs = metaJson.optLong("acceptedRecords", statsObj != null ? statsObj.optLong("acceptedRecords", 0) : 0);
                long rejRecs = metaJson.optLong("rejectedRecords", statsObj != null ? statsObj.optLong("rejectedRecords", 0) : 0);
                long dupRecs = metaJson.optLong("duplicateRecords", statsObj != null ? statsObj.optLong("duplicateRecords", 0) : 0);
                artifact.stats.setAcceptedRecords(accRecs);
                artifact.stats.setRejectedRecords(rejRecs);
                artifact.stats.setDuplicateRecords(dupRecs);

                if (statsObj != null) {
                    artifact.stats.setExactDuplicates(statsObj.optLong("exactDuplicates", 0));
                    artifact.stats.setNormalizedDuplicates(statsObj.optLong("normalizedDuplicates", 0));
                    artifact.stats.setNearDuplicates(statsObj.optLong("nearDuplicates", 0));
                    artifact.stats.setMalformedJsonCount(statsObj.optLong("malformedJsonCount", 0));
                    artifact.stats.setEmptyMeaninglessCount(statsObj.optLong("emptyMeaninglessCount", 0));
                    artifact.stats.setCorruptedTextCount(statsObj.optLong("corruptedTextCount", 0));
                    artifact.stats.setInvalidQaCount(statsObj.optLong("invalidQaCount", 0));
                    artifact.stats.setTrainValLeakageCount(statsObj.optLong("trainValLeakageCount", 0));
                }
            } catch (Exception e) {
                artifact.status = Status.CORRUPTED;
                artifact.loadDiagnostics = "Metadata parsing failed: " + e.getMessage();
            }
        } else {
            // Legacy artifacts without metadata.json remain loadable.
            if (artifact.loadDiagnostics.isEmpty()) {
                artifact.loadDiagnostics = "No metadata.json entry found; integrity checks skipped.";
            }
        }

        // Pass 2: data entries with checksum verification
        List<String> integrityFailures = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // Security check against Zip Slip path traversal
                if (name == null || name.contains("..") || name.startsWith("/") || name.contains("\\..") || name.contains("../")) {
                    zis.closeEntry();
                    continue;
                }
                String normalizedName = new File(name).getName();
                if ("data.jsonl".equals(normalizedName) || "val_data.jsonl".equals(normalizedName)) {
                    boolean isVal = "val_data.jsonl".equals(normalizedName);
                    byte[] entryBytes = zis.readAllBytes();

                    String storedSha = entrySha256.get(normalizedName);
                    if (storedSha != null && !storedSha.isEmpty()) {
                        String computedSha = sha256Hex(entryBytes);
                        if (!storedSha.equals(computedSha)) {
                            integrityFailures.add(normalizedName + " checksum mismatch (stored " + storedSha + ", computed " + computedSha + ")");
                        }
                    }

                    String content = new String(entryBytes, StandardCharsets.UTF_8);
                    String[] lines = content.split("\n");

                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            try {
                                JSONObject json = new JSONObject(trimmed);
                                NormalizedSample sample = new NormalizedSample();
                                if (json.has("id") && !json.isNull("id")) {
                                    sample.setId(json.optString("id"));
                                }
                                String typeStr = json.optString("type", json.optString("sampleType", "TEXT"));
                                sample.setType(TrainingSampleType.fromString(typeStr));
                                String inst = null;
                                if (json.has("instruction") && !json.isNull("instruction")) {
                                    inst = json.optString("instruction");
                                } else if (json.has("prompt") && !json.isNull("prompt")) {
                                    inst = json.optString("prompt");
                                }
                                String resp = json.has("response") && !json.isNull("response") ? json.optString("response") : null;
                                if (inst != null || resp != null) {
                                    sample.setInstruction(inst);
                                    sample.setResponse(resp);
                                } else {
                                    sample.setText(json.optString("text", ""));
                                }
                                if (json.has("conversationId") && !json.isNull("conversationId")) {
                                    sample.setConversationId(json.optString("conversationId"));
                                }
                                if (json.has("messages")) {
                                    JSONArray msgArray = json.optJSONArray("messages");
                                    if (msgArray != null) {
                                        List<NormalizedMessage> normMsgs = new ArrayList<>();
                                        for (int i = 0; i < msgArray.length(); i++) {
                                            JSONObject mObj = msgArray.optJSONObject(i);
                                            if (mObj != null) {
                                                NormalizedMessage m = new NormalizedMessage();
                                                m.setRole(mObj.optString("role", "user"));
                                                m.setText(mObj.optString("text", mObj.optString("content", "")));
                                                m.setMessageId(mObj.has("messageId") && !mObj.isNull("messageId") ? mObj.optString("messageId") : null);
                                                m.setParentMessageId(mObj.has("parentMessageId") && !mObj.isNull("parentMessageId") ? mObj.optString("parentMessageId") : null);
                                                normMsgs.add(m);
                                            }
                                        }
                                        sample.setConversationMessages(normMsgs);
                                    }
                                }
                                sample.setSource(json.optString("source", file.getName()));
                                sample.setQualityScore(json.optDouble("qualityScore", 1.0));
                                sample.setTokenCount(json.optInt("tokenCount", 0));
                                sample.setHash(json.optString("hash", null));
                                sample.recalculateCountsAndHash();

                                if (isVal) {
                                    artifact.valSamples.add(sample);
                                    artifact.totalValTokens += sample.getTokenCount();
                                } else {
                                    artifact.trainSamples.add(sample);
                                    artifact.totalTrainTokens += sample.getTokenCount();
                                }
                            } catch (Exception ex) {
                                // Do NOT silently convert malformed JSON records into synthetic training samples
                                if (artifact.stats != null) {
                                    artifact.stats.incrementRejected();
                                    artifact.stats.addRejectedBytes(trimmed.getBytes(StandardCharsets.UTF_8).length);
                                }
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        if (!integrityFailures.isEmpty()) {
            artifact.status = Status.CORRUPTED;
            artifact.loadDiagnostics = "Integrity verification failed: " + String.join("; ", integrityFailures);
        }

        if (loadedStatus != null && artifact.status != Status.CORRUPTED) {
            artifact.status = loadedStatus;
        } else if (loadedStatus == null && artifact.status != Status.CORRUPTED) {
            artifact.status = Status.READY;
        }
        return artifact;
    }

    private String buildMetadataJson(String trainSha256, String valSha256, String reportSha256, long trainBytes, long valBytes) {
        // Deterministic timestamp keeps byte-level reproducibility of the archive.
        long deterministicTimestamp = 1704067200000L;
        long acceptedRecs = stats != null ? stats.getAcceptedRecords() : (trainSamples.size() + valSamples.size());
        long rejectedRecs = stats != null ? stats.getRejectedRecords() : 0;
        long duplicateRecs = stats != null ? stats.getDuplicateRecords() : 0;
        long acceptedB = stats != null ? stats.getAcceptedBytes() : 0;
        long downloadedB = stats != null ? stats.getDownloadedBytes() : 0;
        long estTokens = stats != null && stats.getEstimatedTokens() > 0 ? stats.getEstimatedTokens() : (totalTrainTokens + totalValTokens);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"formatVersion\": \"").append(FORMAT_VERSION).append("\",\n");
        sb.append("  \"datasetId\": \"").append(name != null ? name : "evodata").append("\",\n");
        sb.append("  \"name\": \"").append(name != null ? name : "evodata").append("\",\n");
        sb.append("  \"createdTimestamp\": ").append(deterministicTimestamp).append(",\n");
        sb.append("  \"timestamp\": ").append(deterministicTimestamp).append(",\n");
        sb.append("  \"status\": \"").append(status.name()).append("\",\n");
        sb.append("  \"source\": \"").append(sourceConfig != null ? sourceConfig.getSourceType() : "UNKNOWN").append("\",\n");
        sb.append("  \"repository\": \"").append(sourceConfig != null ? sourceConfig.getRepository() : "").append("\",\n");
        sb.append("  \"sources\": [\n");
        sb.append("    {\n");
        sb.append("      \"sourceType\": \"").append(sourceConfig != null ? sourceConfig.getSourceType() : "UNKNOWN").append("\",\n");
        sb.append("      \"repository\": \"").append(sourceConfig != null ? sourceConfig.getRepository() : "").append("\",\n");
        if (sourceConfig != null && sourceConfig.getRevision() != null && !sourceConfig.getRevision().isEmpty()) {
            sb.append("      \"revision\": \"").append(sourceConfig.getRevision()).append("\",\n");
        }
        if (sourceConfig != null && sourceConfig.getConfiguration() != null && !sourceConfig.getConfiguration().isEmpty()) {
            sb.append("      \"configuration\": \"").append(sourceConfig.getConfiguration()).append("\",\n");
        }
        if (sourceConfig != null && sourceConfig.getSplit() != null) {
            sb.append("      \"split\": \"").append(sourceConfig.getSplit()).append("\",\n");
        }
        sb.append("      \"downloadedBytes\": ").append(downloadedB).append(",\n");
        sb.append("      \"acceptedBytes\": ").append(acceptedB).append("\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"files\": {\n");
        sb.append("    \"data.jsonl\": { \"sha256\": \"").append(trainSha256).append("\", \"bytes\": ").append(trainBytes).append(" },\n");
        sb.append("    \"val_data.jsonl\": { \"sha256\": \"").append(valSha256).append("\", \"bytes\": ").append(valBytes).append(" }\n");
        sb.append("  },\n");
        if (tokenizerType != null && !tokenizerType.isEmpty()) {
            sb.append("  \"tokenizer\": {\n");
            sb.append("    \"type\": \"").append(tokenizerType).append("\",\n");
            sb.append("    \"version\": \"").append(tokenizerVersion != null ? tokenizerVersion : "unknown").append("\",\n");
            sb.append("    \"vocabSize\": ").append(tokenizerVocabSize).append(",\n");
            sb.append("    \"specialTokens\": [");
            for (int i = 0; i < tokenizerSpecialTokens.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(tokenizerSpecialTokens.get(i).replace("\"", "\\\"")).append("\"");
            }
            sb.append("]\n");
            sb.append("  },\n");
        }
        sb.append("  \"statistics\": {\n");
        sb.append("    \"totalSamples\": ").append(trainSamples.size() + valSamples.size()).append(",\n");
        sb.append("    \"trainSamples\": ").append(trainSamples.size()).append(",\n");
        sb.append("    \"valSamples\": ").append(valSamples.size()).append(",\n");
        sb.append("    \"acceptedRecords\": ").append(acceptedRecs).append(",\n");
        sb.append("    \"rejectedRecords\": ").append(rejectedRecs).append(",\n");
        sb.append("    \"duplicateRecords\": ").append(duplicateRecs).append(",\n");
        if (stats != null) {
            sb.append("    \"exactDuplicates\": ").append(stats.getExactDuplicates()).append(",\n");
            sb.append("    \"normalizedDuplicates\": ").append(stats.getNormalizedDuplicates()).append(",\n");
            sb.append("    \"nearDuplicates\": ").append(stats.getNearDuplicates()).append(",\n");
            sb.append("    \"malformedJsonCount\": ").append(stats.getMalformedJsonCount()).append(",\n");
            sb.append("    \"emptyMeaninglessCount\": ").append(stats.getEmptyMeaninglessCount()).append(",\n");
            sb.append("    \"corruptedTextCount\": ").append(stats.getCorruptedTextCount()).append(",\n");
            sb.append("    \"invalidQaCount\": ").append(stats.getInvalidQaCount()).append(",\n");
            sb.append("    \"trainValLeakageCount\": ").append(stats.getTrainValLeakageCount()).append(",\n");
        }
        sb.append("    \"totalTrainingBytes\": ").append(stats != null ? stats.getTrainingBytes() + stats.getValidationBytes() : acceptedB).append(",\n");
        sb.append("    \"estimatedTokens\": ").append(estTokens).append("\n");
        sb.append("  },\n");
        sb.append("  \"estimatedTokens\": ").append(estTokens).append(",\n");
        sb.append("  \"schema\": {\n");
        sb.append("    \"sampleTypes\": [\"TEXT\", \"INSTRUCTION\", \"CODE\", \"CHAT\", \"CONVERSATION\"],\n");
        sb.append("    \"columns\": [\"id\", \"type\", \"prompt\", \"response\", \"qualityScore\"]\n");
        sb.append("  },\n");
        if (stats != null) {
            sb.append("  \"requestedUsableBytes\": ").append(stats.getRequestedUsableBytes()).append(",\n");
            sb.append("  \"actualUsableBytes\": ").append(stats.getAcceptedBytes()).append(",\n");
            sb.append("  \"downloadedBytes\": ").append(stats.getDownloadedBytes()).append(",\n");
            sb.append("  \"extractedBytes\": ").append(stats.getExtractedBytes()).append(",\n");
            sb.append("  \"rawContentBytes\": ").append(stats.getRawContentBytes()).append(",\n");
            sb.append("  \"acceptedBytes\": ").append(stats.getAcceptedBytes()).append(",\n");
            sb.append("  \"rejectedBytes\": ").append(stats.getRejectedBytes()).append(",\n");
            sb.append("  \"duplicateBytes\": ").append(stats.getDuplicateBytes()).append(",\n");
            sb.append("  \"trainingBytes\": ").append(stats.getTrainingBytes()).append(",\n");
            sb.append("  \"validationBytes\": ").append(stats.getValidationBytes()).append(",\n");
            sb.append("  \"acceptedRecords\": ").append(stats.getAcceptedRecords()).append(",\n");
            sb.append("  \"rejectedRecords\": ").append(stats.getRejectedRecords()).append(",\n");
            sb.append("  \"duplicateRecords\": ").append(stats.getDuplicateRecords()).append(",\n");
        }
        sb.append("  \"trainSamples\": ").append(trainSamples.size()).append(",\n");
        sb.append("  \"valSamples\": ").append(valSamples.size()).append(",\n");
        sb.append("  \"trainTokens\": ").append(totalTrainTokens).append(",\n");
        sb.append("  \"valTokens\": ").append(totalValTokens).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    public String buildReportText() {
        StringBuilder sb = new StringBuilder();
        sb.append("EVO DATA PREPARATION RESULT REPORT\n");
        sb.append("------------------------------------------\n");
        sb.append("Artifact: ").append(name).append("\n");
        sb.append("Source: ").append(sourceConfig != null ? sourceConfig.getSourceType() + " (" + sourceConfig.getRepository() + ")" : "N/A").append("\n");
        if (stats != null && stats.getRequestedUsableBytes() > 0) {
            sb.append("Requested Usable Data: ").append(stats.getRequestedUsableBytes() / (1024 * 1024)).append(" MB (").append(stats.getRequestedUsableBytes()).append(" bytes)\n");
        }
        if (stats != null) {
            sb.append("Downloaded Bytes: ").append(stats.getDownloadedBytes()).append("\n");
            sb.append("Actual Usable Bytes: ").append(stats.getAcceptedBytes()).append(" (").append(String.format("%.2f", stats.getAcceptedBytes() / (1024.0 * 1024.0))).append(" MB)\n");
            sb.append("Training Bytes: ").append(stats.getTrainingBytes()).append(" (").append(String.format("%.2f", stats.getTrainingBytes() / (1024.0 * 1024.0))).append(" MB)\n");
            sb.append("Validation Bytes: ").append(stats.getValidationBytes()).append(" (").append(String.format("%.2f", stats.getValidationBytes() / (1024.0 * 1024.0))).append(" MB)\n");
            sb.append("Estimated Tokens: ").append(String.format("%.1fM", (totalTrainTokens + totalValTokens) / 1000000.0)).append(" (").append(totalTrainTokens + totalValTokens).append(" tokens)\n");
            sb.append("Input/Read Records: ").append(stats.getTotalSamplesRead() > 0 ? stats.getTotalSamplesRead() : (stats.getAcceptedRecords() + stats.getRejectedRecords() + stats.getDuplicateRecords())).append("\n");
            sb.append("Accepted Records: ").append(stats.getAcceptedRecords()).append("\n");
            sb.append("Rejected Records: ").append(stats.getRejectedRecords()).append(" (").append(stats.getRejectedBytes()).append(" bytes)\n");
            if (stats.getMalformedJsonCount() > 0 || stats.getEmptyMeaninglessCount() > 0 || stats.getCorruptedTextCount() > 0 || stats.getInvalidQaCount() > 0) {
                sb.append("  - Malformed JSON: ").append(stats.getMalformedJsonCount()).append("\n");
                sb.append("  - Empty/Meaningless: ").append(stats.getEmptyMeaninglessCount()).append("\n");
                sb.append("  - Corrupted Text: ").append(stats.getCorruptedTextCount()).append("\n");
                sb.append("  - Invalid Q&A Pairs: ").append(stats.getInvalidQaCount()).append("\n");
            }
            sb.append("Duplicate Records: ").append(stats.getDuplicateRecords()).append(" (").append(stats.getDuplicateBytes()).append(" bytes)\n");
            if (stats.getExactDuplicates() > 0 || stats.getNormalizedDuplicates() > 0 || stats.getNearDuplicates() > 0) {
                sb.append("  - Exact Duplicates: ").append(stats.getExactDuplicates()).append("\n");
                sb.append("  - Normalized Duplicates: ").append(stats.getNormalizedDuplicates()).append("\n");
                sb.append("  - Near-Duplicates: ").append(stats.getNearDuplicates()).append("\n");
            }
            sb.append("Train/Val Leakage Filtered: ").append(stats.getTrainValLeakageCount()).append("\n");
            if (stats.getRequestedUsableBytes() > 0) {
                double coverage = (stats.getAcceptedBytes() * 100.0) / Math.max(1, stats.getRequestedUsableBytes());
                sb.append("Source Coverage: ").append(String.format("%.1f%%", Math.min(100.0, coverage))).append("\n");
                if (coverage < 100.0) {
                    sb.append("Note: Source dataset exhausted before target size was reached.\n");
                }
            }
        } else {
            sb.append("Total Processed Samples: ").append(trainSamples.size() + valSamples.size()).append("\n");
            sb.append("Accepted Samples: ").append(trainSamples.size() + valSamples.size()).append("\n");
        }
        sb.append("Train Samples: ").append(trainSamples.size()).append(" (").append(totalTrainTokens).append(" estimated tokens)\n");
        sb.append("Validation Samples: ").append(valSamples.size()).append(" (").append(totalValTokens).append(" estimated tokens)\n");
        sb.append("Status: ").append(status.name()).append("\n");
        sb.append("QUALITY LIMITATIONS & VERIFICATION SUMMARY:\n");
        sb.append("  - Exact & Normalized Deduplication: APPLIED\n");
        sb.append("  - Train/Val Leakage Prevention: APPLIED\n");
        if (stats != null && (stats.getCorruptedTextCount() > 0 || stats.getInvalidQaCount() > 0
                || stats.getEmptyMeaninglessCount() > 0)) {
            sb.append("  - Noise, Corrupted Text & Invalid Q&A Filtering: APPLIED\n");
        } else {
            sb.append("  - Noise, Corrupted Text & Invalid Q&A Filtering: NOT APPLIED IN THIS PIPELINE\n");
        }
        sb.append("  - Integrity: per-entry SHA-256 checksums recorded in metadata.json (format ").append(FORMAT_VERSION).append(")\n");
        if (tokenizerType != null && !tokenizerType.isEmpty()) {
            sb.append("  - Tokenizer: ").append(tokenizerType).append(" ").append(tokenizerVersion != null ? tokenizerVersion : "")
                    .append(" (vocab ").append(tokenizerVocabSize).append(")\n");
        }
        sb.append("  - Remaining Limitations: High-level semantic overlap or specialized domain nuances may persist.\n");
        sb.append("------------------------------------------\n");
        return sb.toString();
    }

    public File getArtifactFile() { return artifactFile; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<NormalizedSample> getTrainSamples() { return trainSamples; }

    /**
     * Rewrites only the metadata.json entry of an existing archive to record the
     * tokenizer identity (type/version/vocab/special tokens). Data entries remain
     * byte-identical, so payload checksums and reproducibility are preserved.
     */
    public void rewriteMetadataWithTokenizerIdentity(String type, String version, int vocabSize, List<String> specialTokens) throws Exception {
        if (artifactFile == null || !artifactFile.exists()) {
            throw new IllegalStateException("Artifact file does not exist: " + artifactFile);
        }
        setTokenizerIdentity(type, version, vocabSize, specialTokens);

        Map<String, byte[]> entries = new HashMap<>();
        byte[] reportBytes = null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(artifactFile), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name != null && !name.contains("..")) {
                    String normalized = new File(name).getName();
                    byte[] bytes = zis.readAllBytes();
                    if ("report.txt".equals(normalized)) {
                        reportBytes = bytes;
                    } else if (!"metadata.json".equals(normalized)) {
                        entries.put(normalized, bytes);
                    }
                }
                zis.closeEntry();
            }
        }

        byte[] trainBytes = entries.get("data.jsonl");
        byte[] valBytes = entries.get("val_data.jsonl");
        if (trainBytes == null) {
            throw new IllegalStateException("Artifact has no data.jsonl entry");
        }
        byte[] newReportBytes = (reportBytes != null) ? reportBytes : buildReportText().getBytes(StandardCharsets.UTF_8);
        byte[] metaBytes = buildMetadataJson(
                sha256Hex(trainBytes),
                sha256Hex(valBytes != null ? valBytes : new byte[0]),
                sha256Hex(newReportBytes),
                trainBytes.length,
                valBytes != null ? valBytes.length : 0
        ).getBytes(StandardCharsets.UTF_8);

        File tempFile = new File(artifactFile.getAbsolutePath() + ".tmp");
        long fixedTimestamp = 1704067200000L;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            writeEntry(zos, "data.jsonl", trainBytes, fixedTimestamp);
            if (valBytes != null) {
                writeEntry(zos, "val_data.jsonl", valBytes, fixedTimestamp);
            }
            writeEntry(zos, "metadata.json", metaBytes, fixedTimestamp);
            writeEntry(zos, "report.txt", newReportBytes, fixedTimestamp);
        }
        if (artifactFile.exists()) {
            artifactFile.delete();
        }
        if (!tempFile.renameTo(artifactFile)) {
            java.nio.file.Files.move(tempFile.toPath(), artifactFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeEntry(ZipOutputStream zos, String name, byte[] bytes, long timestamp) throws java.io.IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(timestamp);
        zos.putNextEntry(entry);
        zos.write(bytes);
        zos.closeEntry();
    }
    public List<NormalizedSample> getValSamples() { return valSamples; }
    public List<NormalizedSample> getSamples() {
        List<NormalizedSample> all = new ArrayList<>(trainSamples);
        all.addAll(valSamples);
        return all;
    }
    public long getTotalTrainTokens() { return totalTrainTokens; }
    public long getTotalValTokens() { return totalValTokens; }
}
