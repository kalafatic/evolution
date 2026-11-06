package eu.kalafatic.evolution.controller.teip.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured TEIP error representation.
 */
public class TeipError {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String code;
    private String message;
    private String requestId;
    private String correlationId;
    private boolean retryable = false;
    private String timestamp;
    private Map<String, Object> details = new HashMap<>();

    public TeipError() {}

    public TeipError(String code, String message, String requestId, String correlationId, boolean retryable) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.retryable = retryable;
        this.timestamp = java.time.Instant.now().toString();
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details != null ? details : new HashMap<>(); }
}
