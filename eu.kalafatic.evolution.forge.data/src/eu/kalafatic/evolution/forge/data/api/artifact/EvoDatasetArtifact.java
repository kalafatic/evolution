package eu.kalafatic.evolution.forge.data.api.artifact;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

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

        // Split samples after deduplication
        trainSamples.clear();
        valSamples.clear();

        int valCount = (int) (allSamples.size() * valSplit);
        int trainCount = allSamples.size() - valCount;

        for (int i = 0; i < allSamples.size(); i++) {
            NormalizedSample sample = allSamples.get(i);
            if (i < trainCount) {
                trainSamples.add(sample);
                totalTrainTokens += sample.getTokenCount() > 0 ? sample.getTokenCount() : (sample.getCharCount() / 4);
            } else {
                valSamples.add(sample);
                totalValTokens += sample.getTokenCount() > 0 ? sample.getTokenCount() : (sample.getCharCount() / 4);
            }
        }

        this.status = Status.VALIDATING;

        // Write to temporary archive first (transactional semantics)
        File tempFile = new File(artifactFile.getAbsolutePath() + ".tmp");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {

            // 1. data.jsonl
            zos.putNextEntry(new ZipEntry("data.jsonl"));
            for (NormalizedSample s : trainSamples) {
                zos.write((s.toJsonLine() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            zos.closeEntry();

            // 2. val_data.jsonl
            zos.putNextEntry(new ZipEntry("val_data.jsonl"));
            for (NormalizedSample s : valSamples) {
                zos.write((s.toJsonLine() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            zos.closeEntry();

            // 3. metadata.json
            zos.putNextEntry(new ZipEntry("metadata.json"));
            String metaJson = buildMetadataJson();
            zos.write(metaJson.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 4. report.txt
            zos.putNextEntry(new ZipEntry("report.txt"));
            String reportText = buildReportText();
            zos.write(reportText.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        this.status = Status.FINALIZING;

        if (artifactFile.exists()) {
            artifactFile.delete();
        }
        if (!tempFile.renameTo(artifactFile)) {
            java.nio.file.Files.move(tempFile.toPath(), artifactFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // Post-serialization structural and semantic validation before marking READY
        validateArtifact(artifactFile);

        this.status = Status.READY;
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

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if ("data.jsonl".equals(name) || "val_data.jsonl".equals(name)) {
                    boolean isVal = "val_data.jsonl".equals(name);
                    byte[] entryBytes = zis.readAllBytes();
                    String content = new String(entryBytes, StandardCharsets.UTF_8);
                    String[] lines = content.split("\n");

                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            try {
                                JSONObject json = new JSONObject(trimmed);
                                NormalizedSample sample = new NormalizedSample();
                                sample.setType(TrainingSampleType.fromString(json.optString("type", "TEXT")));
                                sample.setText(json.optString("text", ""));
                                sample.setInstruction(json.optString("instruction", null));
                                sample.setResponse(json.optString("response", null));
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
                                artifact.trainSamples.add(NormalizedSample.createTextSample(trimmed, file.getName()));
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        artifact.status = Status.READY;
        return artifact;
    }

    private String buildMetadataJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(name).append("\",\n");
        sb.append("  \"status\": \"").append(status.name()).append("\",\n");
        sb.append("  \"source\": \"").append(sourceConfig != null ? sourceConfig.getSourceType() : "UNKNOWN").append("\",\n");
        sb.append("  \"repository\": \"").append(sourceConfig != null ? sourceConfig.getRepository() : "").append("\",\n");
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
        sb.append("  \"valTokens\": ").append(totalValTokens).append(",\n");
        sb.append("  \"timestamp\": ").append(System.currentTimeMillis()).append("\n");
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
            sb.append("Accepted Records: ").append(stats.getAcceptedRecords()).append("\n");
            sb.append("Rejected Records: ").append(stats.getRejectedRecords()).append(" (").append(stats.getRejectedBytes()).append(" bytes)\n");
            sb.append("Duplicate Records: ").append(stats.getDuplicateRecords()).append(" (").append(stats.getDuplicateBytes()).append(" bytes)\n");
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
        sb.append("------------------------------------------\n");
        return sb.toString();
    }

    public File getArtifactFile() { return artifactFile; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public List<NormalizedSample> getTrainSamples() { return trainSamples; }
    public List<NormalizedSample> getValSamples() { return valSamples; }
    public long getTotalTrainTokens() { return totalTrainTokens; }
    public long getTotalValTokens() { return totalValTokens; }
}
