package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.File;

public class BuildResult {
    private final boolean success;
    private final String message;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final long executionTimeMs;
    private File producedArtifact;

    public BuildResult(boolean success, String message, int exitCode, String stdout, String stderr, long executionTimeMs) {
        this.success = success;
        this.message = message;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public File getProducedArtifact() {
        return producedArtifact;
    }

    public void setProducedArtifact(File producedArtifact) {
        this.producedArtifact = producedArtifact;
    }

    @Override
    public String toString() {
        return String.format("BuildResult{success=%s, exitCode=%d, executionTimeMs=%d, artifact=%s}",
                success, exitCode, executionTimeMs, producedArtifact);
    }
}
