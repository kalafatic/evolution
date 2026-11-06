package eu.kalafatic.evolution.controller.teip.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution result DTO for TEIP 1.0.
 */
public class TeipExecutionResult {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String executionId;
    private String correlationId;
    private TeipExecutionStatus status = TeipExecutionStatus.COMPLETED;
    private String summary = "";
    private String completedAt;
    private List<Map<String, Object>> artifacts = new ArrayList<>();
    private Map<String, Object> metrics = new HashMap<>();

    public TeipExecutionResult() {}

    public TeipExecutionResult(String executionId, String correlationId, TeipExecutionStatus status, String summary) {
        this.executionId = executionId;
        this.correlationId = correlationId;
        this.status = status;
        this.summary = summary;
        this.completedAt = java.time.Instant.now().toString();
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public TeipExecutionStatus getStatus() { return status; }
    public void setStatus(TeipExecutionStatus status) { this.status = status; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public List<Map<String, Object>> getArtifacts() { return artifacts; }
    public void setArtifacts(List<Map<String, Object>> artifacts) { this.artifacts = artifacts != null ? artifacts : new ArrayList<>(); }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics != null ? metrics : new HashMap<>(); }
}
