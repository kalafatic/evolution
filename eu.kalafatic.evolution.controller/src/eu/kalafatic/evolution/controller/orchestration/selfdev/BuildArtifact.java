package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BuildArtifact {
    private final ArtifactType type;
    private final File path;
    private final String sourceRevision;
    private final long createdAt;
    private final String platform;
    private final Map<String, String> metadata;

    public BuildArtifact(ArtifactType type, File path, String sourceRevision, String platform, Map<String, String> metadata) {
        this.type = type;
        this.path = path;
        this.sourceRevision = sourceRevision;
        this.createdAt = System.currentTimeMillis();
        this.platform = platform != null ? platform : System.getProperty("os.name");
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
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

    @Override
    public String toString() {
        return "BuildArtifact{" +
                "type=" + type +
                ", path=" + (path != null ? path.getAbsolutePath() : "null") +
                ", sourceRevision='" + sourceRevision + '\'' +
                ", createdAt=" + createdAt +
                ", platform='" + platform + '\'' +
                '}';
    }
}
