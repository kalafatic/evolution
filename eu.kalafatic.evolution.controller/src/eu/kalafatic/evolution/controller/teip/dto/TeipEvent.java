package eu.kalafatic.evolution.controller.teip.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard sequenced event DTO for TEIP 1.0.
 */
public class TeipEvent {
    private String eventId;
    private String executionId;
    private long sequence;
    private String type;
    private String timestamp;
    private String correlationId;
    private Map<String, Object> data = new HashMap<>();

    public TeipEvent() {}

    public TeipEvent(String eventId, String executionId, long sequence, String type, String correlationId, Map<String, Object> data) {
        this.eventId = eventId;
        this.executionId = executionId;
        this.sequence = sequence;
        this.type = type;
        this.correlationId = correlationId;
        this.timestamp = java.time.Instant.now().toString();
        this.data = data != null ? data : new HashMap<>();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data != null ? data : new HashMap<>(); }
}
