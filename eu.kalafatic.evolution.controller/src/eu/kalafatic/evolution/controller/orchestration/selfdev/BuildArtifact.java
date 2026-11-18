package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable build artifact identity with full provenance.
 *
 * The manifest fields (runId, buildId, size, sha256, build/test/export results)
 * are assigned by the authoritative ArtifactManifestStore when an artifact is
 * recorded. Consumers must resolve artifacts through the store instead of
 * re-deriving identity from filesystem scans.
 */
public class BuildArtifact {
    private final ArtifactType type;
    private final File path;
    private final String sourceRevision;
    private final long createdAt;
    private final String platform;
    private final Map<String, String> metadata;

    // Provenance fields (populated by ArtifactManifestStore)
    private final String runId;
    private final String buildId;
    private final long size;
    private final String sha256;
    private final String buildResult;
    private final String testResult;
    private final String exportResult;

    public BuildArtifact(ArtifactType type, File path, String sourceRevision, String platform, Map<String, String> metadata) {
        this(type, path, sourceRevision, platform, metadata, null, null, -1, null, null, null, null);
    }

    public BuildArtifact(ArtifactType type, File path, String sourceRevision, String platform, Map<String, String> metadata,
                         String runId, String buildId, long size, String sha256,
                         String buildResult, String testResult, String exportResult) {
        this.type = type;
        this.path = path;
        this.sourceRevision = sourceRevision;
        this.createdAt = System.currentTimeMillis();
        this.platform = platform != null ? platform : System.getProperty("os.name");
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.runId = runId;
        this.buildId = buildId;
        this.size = size;
        this.sha256 = sha256;
        this.buildResult = buildResult;
        this.testResult = testResult;
        this.exportResult = exportResult;
    }

    public ArtifactType getType() {
        return type;
    }

    public File getPath() {
        return path;
    }

    public String getSourceRevision() {
        return sourceRevision;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getPlatform() {
        return platform;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public String getRunId() {
        return runId;
    }

    public String getBuildId() {
        return buildId;
    }

    public long getSize() {
        return size;
    }

    public String getSha256() {
        return sha256;
    }

    public String getBuildResult() {
        return buildResult;
    }

    public String getTestResult() {
        return testResult;
    }

    public String getExportResult() {
        return exportResult;
    }

    public String getProductId() {
        return metadata.get("productId");
    }

    public String getOs() {
        return metadata.get("os");
    }

    public String getArch() {
        return metadata.get("arch");
    }

    @Override
    public String toString() {
        return "BuildArtifact{" +
                "type=" + type +
                ", path=" + (path != null ? path.getAbsolutePath() : "null") +
                ", sourceRevision='" + sourceRevision + '\'' +
                ", createdAt=" + createdAt +
                ", platform='" + platform + '\'' +
                ", runId='" + runId + '\'' +
                ", buildId='" + buildId + '\'' +
                ", sha256='" + (sha256 != null ? sha256.substring(0, Math.min(12, sha256.length())) + "..." : "null") + '\'' +
                '}';
    }
}
