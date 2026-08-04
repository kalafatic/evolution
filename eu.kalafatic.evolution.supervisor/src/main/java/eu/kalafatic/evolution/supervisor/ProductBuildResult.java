package eu.kalafatic.evolution.supervisor;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public final class ProductBuildResult {
    private boolean successful;
    private int exitCode;
    private Instant startedAt;
    private Instant finishedAt;
    private Duration duration;
    private Path artifactPath;
    private Path buildLogPath;
    private String failureSummary;

    public ProductBuildResult(boolean successful, int exitCode, Instant startedAt, Instant finishedAt, Path artifactPath, String failureSummary) {
        this.successful = successful;
        this.exitCode = exitCode;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        if (startedAt != null && finishedAt != null) {
            this.duration = Duration.between(startedAt, finishedAt);
        } else {
            this.duration = Duration.ZERO;
        }
        this.artifactPath = artifactPath;
        this.failureSummary = failureSummary;
    }

    public boolean isSuccessful() { return successful; }
    public int getExitCode() { return exitCode; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Duration getDuration() { return duration; }
    public Path getArtifactPath() { return artifactPath; }
    public Path getBuildLogPath() { return buildLogPath; }
    public void setBuildLogPath(Path buildLogPath) { this.buildLogPath = buildLogPath; }
    public String getFailureSummary() { return failureSummary; }
}
