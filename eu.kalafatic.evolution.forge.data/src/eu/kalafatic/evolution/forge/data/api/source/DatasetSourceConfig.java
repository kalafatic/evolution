package eu.kalafatic.evolution.forge.data.api.source;

public class DatasetSourceConfig {
    private String sourceType = "LOCAL"; // LOCAL or HUGGING_FACE
    private String repository; // dataset id or directory path
    private String configuration;
    private String split = "train";
    private String revision;
    private long maxBytes = 0; // 0 = unlimited
    private long maxTokens = 0; // 0 = unlimited
    private long maxSamples = 0; // 0 = unlimited
    private boolean streaming = true;
    private String runId;
    private String origin = "REQUEST";
    private String outputDir;

    public DatasetSourceConfig() {}

    public DatasetSourceConfig(String sourceType, String repository) {
        this.sourceType = sourceType;
        this.repository = repository;
    }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }

    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }

    public String getSplit() { return split; }
    public void setSplit(String split) { this.split = split; }

    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }

    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }

    public long getMaxTokens() { return maxTokens; }
    public void setMaxTokens(long maxTokens) { this.maxTokens = maxTokens; }

    public long getMaxSamples() { return maxSamples; }
    public void setMaxSamples(long maxSamples) { this.maxSamples = maxSamples; }

    public boolean isStreaming() { return streaming; }
    public void setStreaming(boolean streaming) { this.streaming = streaming; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public static java.io.File resolveDatasetOutputDir(String baseOutputDir, String repo) {
        if (baseOutputDir == null || baseOutputDir.trim().isEmpty()) {
            baseOutputDir = new java.io.File(System.getProperty("user.home"), "workspace/forge-input").getAbsolutePath();
        }
        java.io.File baseDir = new java.io.File(baseOutputDir.trim());
        if (repo == null || repo.trim().isEmpty()) {
            return baseDir;
        }

        String shortName = repo.trim();
        int lastSlash = shortName.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < shortName.length() - 1) {
            shortName = shortName.substring(lastSlash + 1);
        }
        shortName = shortName.trim();

        if (baseDir.getName().equalsIgnoreCase(shortName)) {
            return baseDir;
        }

        return new java.io.File(baseDir, shortName);
    }
}
