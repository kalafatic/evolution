package eu.kalafatic.evolution.controller.teip.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Accepted execution response DTO for TEIP 1.0 (HTTP 202).
 */
public class TeipAcceptedResponse {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String executionId;
    private TeipExecutionStatus status = TeipExecutionStatus.QUEUED;
    private String correlationId;
    private String createdAt = java.time.Instant.now().toString();
    private Map<String, String> links = new HashMap<>();

    public TeipAcceptedResponse() {}

    public TeipAcceptedResponse(String executionId, String correlationId) {
        this.executionId = executionId;
        this.correlationId = correlationId;
        this.links.put("status", "/teip/v1/executions/" + executionId);
        this.links.put("events", "/teip/v1/executions/" + executionId + "/events");
        this.links.put("result", "/teip/v1/executions/" + executionId + "/result");
        this.links.put("cancel", "/teip/v1/executions/" + executionId + "/cancel");
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
        if (executionId != null) {
            this.links.put("status", "/teip/v1/executions/" + executionId);
            this.links.put("events", "/teip/v1/executions/" + executionId + "/events");
            this.links.put("result", "/teip/v1/executions/" + executionId + "/result");
            this.links.put("cancel", "/teip/v1/executions/" + executionId + "/cancel");
        }
    }

    public TeipExecutionStatus getStatus() { return status; }
    public void setStatus(TeipExecutionStatus status) { this.status = status; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Map<String, String> getLinks() { return links; }
    public void setLinks(Map<String, String> links) { this.links = links != null ? links : new HashMap<>(); }
}
