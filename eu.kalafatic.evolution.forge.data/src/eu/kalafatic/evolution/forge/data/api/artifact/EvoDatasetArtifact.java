package eu.kalafatic.evolution.forge.data.api.artifact;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

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
        PREPARING,
        VALIDATING,
        FINALIZING,
        READY,
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

        this.status = Status.READY;
    }

    public static EvoDatasetArtifact load(File file) throws Exception {
        EvoDatasetArtifact artifact = new EvoDatasetArtifact(file);
        artifact.trainSamples.clear();
        artifact.valSamples.clear();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("data.jsonl".equals(entry.getName())) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            artifact.trainSamples.add(NormalizedSample.createTextSample(line.trim(), file.getName()));
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
        sb.append("Total Processed Samples: ").append(stats != null ? stats.getTotalSamplesRead() : trainSamples.size() + valSamples.size()).append("\n");
        sb.append("Accepted Samples: ").append(trainSamples.size() + valSamples.size()).append("\n");
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
