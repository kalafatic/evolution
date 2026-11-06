package eu.kalafatic.evolution.controller.teip.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution status response DTO for TEIP 1.0.
 */
public class TeipExecutionStatusResponse {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String executionId;
    private TeipExecutionStatus status = TeipExecutionStatus.QUEUED;
    private String correlationId;
    private double progress = 0.0;
    private String statusMessage = "Created";
    private String createdAt;
    private String startedAt;
    private String completedAt;
    private Map<String, Object> details = new HashMap<>();

    public TeipExecutionStatusResponse() {}

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public TeipExecutionStatus getStatus() { return status; }
    public void setStatus(TeipExecutionStatus status) { this.status = status; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details != null ? details : new HashMap<>(); }
}
