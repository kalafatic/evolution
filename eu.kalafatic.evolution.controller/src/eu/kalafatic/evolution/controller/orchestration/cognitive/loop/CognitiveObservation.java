package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain-independent observation resulting from an action or state inspection.
 */
public class CognitiveObservation {
    private final String actionName;
    private final boolean success;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final String structuredError;
    private final long executionTimeMs;
    private final Map<String, Object> metadata;

    public CognitiveObservation(String actionName, boolean success, int exitCode, String stdout, String stderr, String structuredError, long executionTimeMs, Map<String, Object> metadata) {
        this.actionName = actionName;
        this.success = success;
        this.exitCode = exitCode;
        this.stdout = stdout != null ? stdout : "";
        this.stderr = stderr != null ? stderr : "";
        this.structuredError = structuredError;
        this.executionTimeMs = executionTimeMs;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static CognitiveObservation ofSuccess(String actionName, String stdout, long executionTimeMs) {
        return new CognitiveObservation(actionName, true, 0, stdout, "", null, executionTimeMs, null);
    }

    public static CognitiveObservation ofFailure(String actionName, int exitCode, String stdout, String stderr, String error, long executionTimeMs) {
        return new CognitiveObservation(actionName, false, exitCode, stdout, stderr, error, executionTimeMs, null);
    }

    public String getActionName() {
        return actionName;
    }

    public boolean isSuccess() {
        return success;
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

    public String getStructuredError() {
        return structuredError;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public String toString() {
        return "CognitiveObservation{" +
                "actionName='" + actionName + '\'' +
                ", success=" + success +
                ", exitCode=" + exitCode +
                ", stdout='" + (stdout.length() > 100 ? stdout.substring(0, 100) + "..." : stdout) + '\'' +
                ", stderr='" + (stderr.length() > 100 ? stderr.substring(0, 100) + "..." : stderr) + '\'' +
                ", error='" + structuredError + '\'' +
                ", timeMs=" + executionTimeMs +
                '}';
    }
}
