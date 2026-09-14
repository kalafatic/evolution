package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    public CognitiveFailureType getFailureType() {
        if (success) {
            return CognitiveFailureType.TARGET_REACHED;
        }
        Object failTypeObj = metadata.get("failureType");
        if (failTypeObj instanceof String) {
            return CognitiveFailureType.fromString((String) failTypeObj);
        }
        return CognitiveFailureType.fromString(structuredError);
    }

    public long getRequestedBytes() {
        Object val = metadata.get("requestedMinimumUsableBytes");
        if (val == null) val = metadata.get("targetUsableBytes");
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    public long getUsableBytes() {
        Object val = metadata.get("usableContentBytes");
        if (val == null) val = metadata.get("quantity");
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    public long getRemainingBytes() {
        Object val = metadata.get("remainingBytes");
        if (val instanceof Number) return ((Number) val).longValue();
        long req = getRequestedBytes();
        long usable = getUsableBytes();
        return req > usable ? (req - usable) : 0L;
    }

    public String getSource() {
        Object val = metadata.get("sourceType");
        return val != null ? val.toString() : "UNKNOWN";
    }

    public String getDataset() {
        Object val = metadata.get("repository");
        if (val == null) val = metadata.get("domain");
        return val != null ? val.toString() : "UNKNOWN";
    }

    public String getSplit() {
        Object val = metadata.get("split");
        return val != null ? val.toString() : "train";
    }

    public boolean isExhausted() {
        Object val = metadata.get("isSourceExhausted");
        if (val instanceof Boolean) return (Boolean) val;
        return !success;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRecommendedNextActions() {
        Object val = metadata.get("recommendedNextActions");
        if (val instanceof List) return (List<String>) val;
        return List.of();
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
