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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Reusable, persistent EVO Training Dataset Artifact (.evodata).
 * Zip-based container containing data.jsonl, metadata.json, profile.json, and report.txt.
 */
public class EvoDatasetArtifact {

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

    public EvoDatasetArtifact(File artifactFile) {
        this.artifactFile = artifactFile;
        if (artifactFile != null) {
            this.name = artifactFile.getName();
        }
    }

    public void save(List<NormalizedSample> allSamples, DatasetSourceConfig config, DatasetSourceStats stats, double valSplit) throws Exception {
        this.status = Status.PREPARING;
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

        int valCount = (int) (allSamples.size() * valSplit);
        int trainCount = allSamples.size() - valCount;

        java.util.Set<String> trainHashes = new java.util.HashSet<>();
        java.util.Set<String> trainNormHashes = new java.util.HashSet<>();

        for (int i = 0; i < allSamples.size(); i++) {
            NormalizedSample sample = allSamples.get(i);
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

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {

            // 1. data.jsonl
            ZipEntry zeData = new ZipEntry("data.jsonl");
            zeData.setTime(fixedTimestamp);
            zos.putNextEntry(zeData);
            for (NormalizedSample s : trainSamples) {
                byte[] lineBytes = (s.toJsonLine() + "\n").getBytes(StandardCharsets.UTF_8);
                zos.write(lineBytes);
                actualTrainingBytes += lineBytes.length;
            }
            zos.closeEntry();

            // 2. val_data.jsonl
            ZipEntry zeVal = new ZipEntry("val_data.jsonl");
            zeVal.setTime(fixedTimestamp);
            zos.putNextEntry(zeVal);
            for (NormalizedSample s : valSamples) {
                byte[] lineBytes = (s.toJsonLine() + "\n").getBytes(StandardCharsets.UTF_8);
                zos.write(lineBytes);
                actualValidationBytes += lineBytes.length;
            }
            zos.closeEntry();

            if (stats != null) {
                stats.setTrainingBytes(actualTrainingBytes);
                stats.setValidationBytes(actualValidationBytes);
            }

            // 3. metadata.json
            ZipEntry zeMeta = new ZipEntry("metadata.json");
            zeMeta.setTime(fixedTimestamp);
            zos.putNextEntry(zeMeta);
            String metaJson = buildMetadataJson();
            zos.write(metaJson.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 4. report.txt
            ZipEntry zeReport = new ZipEntry("report.txt");
            zeReport.setTime(fixedTimestamp);
            zos.putNextEntry(zeReport);
            String reportText = buildReportText();
            zos.write(reportText.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        if (artifactFile.exists()) {
            artifactFile.delete();
        }
        if (!tempFile.renameTo(artifactFile)) {
            java.nio.file.Files.move(tempFile.toPath(), artifactFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // Post-serialization structural and semantic validation
        validateArtifact(artifactFile);
    }

    private void validateArtifact(File file) throws Exception {
        if (!file.exists() || file.length() == 0) {
            this.status = Status.FAILED;
            throw new IllegalStateException("Dataset artifact file is missing or 0 bytes.");
        }

        EvoDatasetArtifact loaded = load(file);
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
        Status loadedStatus = null;

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
                if ("metadata.json".equals(normalizedName)) {
                    byte[] entryBytes = zis.readAllBytes();
                    String metaStr = new String(entryBytes, StandardCharsets.UTF_8);
                    try {
                        JSONObject metaJson = new JSONObject(metaStr);
                        String statusStr = metaJson.optString("status", null);
                        if (statusStr != null) {
                            try {
                                loadedStatus = Status.valueOf(statusStr);
                            } catch (Exception ignored) {}
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
                        artifact.stats.setRejectedBytes(metaJson.optLong("rejectedBytes", statsObj != null ? statsObj.optLong("rejectedRecords", 0) : 0));
                        artifact.stats.setDuplicateBytes(metaJson.optLong("duplicateBytes", statsObj != null ? statsObj.optLong("duplicateRecords", 0) : 0));
                        artifact.stats.setTrainingBytes(metaJson.optLong("trainingBytes", statsObj != null ? statsObj.optLong("totalTrainingBytes", 0) : 0));
                        artifact.stats.setValidationBytes(metaJson.optLong("validationBytes", 0));
                    } catch (Exception ignored) {}
                } else if ("data.jsonl".equals(normalizedName) || "val_data.jsonl".equals(normalizedName)) {
                    boolean isVal = "val_data.jsonl".equals(normalizedName);
                    byte[] entryBytes = zis.readAllBytes();
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
                                String inst = json.has("instruction") && !json.isNull("instruction") ? json.optString("instruction") : null;
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

        if (loadedStatus != null) {
            artifact.status = loadedStatus;
        } else {
            artifact.status = Status.READY;
        }
        return artifact;
    }

    private String buildMetadataJson() {
        long now = System.currentTimeMillis();
        long acceptedRecs = stats != null ? stats.getAcceptedRecords() : (trainSamples.size() + valSamples.size());
        long rejectedRecs = stats != null ? stats.getRejectedRecords() : 0;
        long duplicateRecs = stats != null ? stats.getDuplicateRecords() : 0;
        long acceptedB = stats != null ? stats.getAcceptedBytes() : 0;
        long downloadedB = stats != null ? stats.getDownloadedBytes() : 0;
        long estTokens = stats != null && stats.getEstimatedTokens() > 0 ? stats.getEstimatedTokens() : (totalTrainTokens + totalValTokens);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"formatVersion\": \"1.0\",\n");
        sb.append("  \"datasetId\": \"").append(name != null ? name : "evodata").append("\",\n");
        sb.append("  \"name\": \"").append(name != null ? name : "evodata").append("\",\n");
        sb.append("  \"createdTimestamp\": ").append(now).append(",\n");
        sb.append("  \"timestamp\": ").append(now).append(",\n");
        sb.append("  \"status\": \"").append(status.name()).append("\",\n");
        sb.append("  \"source\": \"").append(sourceConfig != null ? sourceConfig.getSourceType() : "UNKNOWN").append("\",\n");
        sb.append("  \"repository\": \"").append(sourceConfig != null ? sourceConfig.getRepository() : "").append("\",\n");
        sb.append("  \"sources\": [\n");
        sb.append("    {\n");
        sb.append("      \"sourceType\": \"").append(sourceConfig != null ? sourceConfig.getSourceType() : "UNKNOWN").append("\",\n");
        sb.append("      \"target\": \"").append(sourceConfig != null ? sourceConfig.getRepository() : "").append("\",\n");
        sb.append("      \"downloadedBytes\": ").append(downloadedB).append(",\n");
        sb.append("      \"acceptedBytes\": ").append(acceptedB).append("\n");
        sb.append("    }\n");
        sb.append("  ],\n");
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
            sb.append("  \"estimatedTokens\": ").append(stats.getEstimatedTokens()).append(",\n");
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
        sb.append("  - Noise, Corrupted Text & Invalid Q&A Filtering: APPLIED\n");
        sb.append("  - Remaining Limitations: High-level semantic overlap or specialized domain nuances may persist.\n");
        sb.append("------------------------------------------\n");
        return sb.toString();
    }

    public File getArtifactFile() { return artifactFile; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<NormalizedSample> getTrainSamples() { return trainSamples; }
    public List<NormalizedSample> getValSamples() { return valSamples; }
    public List<NormalizedSample> getSamples() {
        List<NormalizedSample> all = new ArrayList<>(trainSamples);
        all.addAll(valSamples);
        return all;
    }
    public long getTotalTrainTokens() { return totalTrainTokens; }
    public long getTotalValTokens() { return totalValTokens; }
}
