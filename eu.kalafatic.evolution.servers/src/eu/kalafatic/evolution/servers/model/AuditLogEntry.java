package eu.kalafatic.evolution.servers.model;

import java.time.LocalDateTime;

public class AuditLogEntry {
    private Long id;
    private LocalDateTime timestamp;
    private Long actorUserId;
    private String actorUsername;
    private String action;
    private String resourceType;
    private String resourceId;
    private String outcome;
    private String details;

    public AuditLogEntry() {}

    public AuditLogEntry(Long id, LocalDateTime timestamp, Long actorUserId, String actorUsername, String action, String resourceType, String resourceId, String outcome, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.details = details;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
