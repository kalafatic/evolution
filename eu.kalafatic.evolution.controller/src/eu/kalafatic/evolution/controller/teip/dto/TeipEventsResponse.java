package eu.kalafatic.evolution.controller.teip.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Event list response DTO for TEIP 1.0.
 */
public class TeipEventsResponse {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String executionId;
    private List<TeipEvent> events = new ArrayList<>();
    private boolean hasMore = false;
    private long nextSequence = 0;

    public TeipEventsResponse() {}

    public TeipEventsResponse(String executionId, List<TeipEvent> events, boolean hasMore, long nextSequence) {
        this.executionId = executionId;
        this.events = events != null ? events : new ArrayList<>();
        this.hasMore = hasMore;
        this.nextSequence = nextSequence;
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public List<TeipEvent> getEvents() { return events; }
    public void setEvents(List<TeipEvent> events) { this.events = events != null ? events : new ArrayList<>(); }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public long getNextSequence() { return nextSequence; }
    public void setNextSequence(long nextSequence) { this.nextSequence = nextSequence; }
}
