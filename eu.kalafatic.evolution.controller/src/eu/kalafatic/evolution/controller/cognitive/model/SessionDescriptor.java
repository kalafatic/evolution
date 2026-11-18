package eu.kalafatic.evolution.controller.cognitive.model;

import org.json.JSONObject;

/**
 * Durable registry descriptor of a cognitive session. The runtime session
 * container remains SessionManager/SessionContext; this is the persisted
 * registry row, not a second session manager.
 */
public class SessionDescriptor {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    public static final String KIND_USER = "USER";
    public static final String KIND_APPLICATION = "APPLICATION";
    public static final String KIND_CONVERSATION = "CONVERSATION";
    public static final String KIND_TASK = "TASK";

    private String id;
    private String legacyAlias;
    private String kind = KIND_CONVERSATION;
    private String ownerUserId;
    private String applicationId;
    private String projectRef;
    private String status = STATUS_ACTIVE;
    private long startedAt;
    private long lastActivityAt;
    private long endedAt;
    private String parentSessionId;
    private String metadata;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLegacyAlias() { return legacyAlias; }
    public void setLegacyAlias(String legacyAlias) { this.legacyAlias = legacyAlias; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getProjectRef() { return projectRef; }
    public void setProjectRef(String projectRef) { this.projectRef = projectRef; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public long getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(long lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public long getEndedAt() { return endedAt; }
    public void setEndedAt(long endedAt) { this.endedAt = endedAt; }
    public String getParentSessionId() { return parentSessionId; }
    public void setParentSessionId(String parentSessionId) { this.parentSessionId = parentSessionId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        if (legacyAlias != null) json.put("legacyAlias", legacyAlias);
        json.put("kind", kind);
        if (ownerUserId != null) json.put("ownerUserId", ownerUserId);
        if (applicationId != null) json.put("applicationId", applicationId);
        if (projectRef != null) json.put("projectRef", projectRef);
        json.put("status", status);
        json.put("startedAt", startedAt);
        json.put("lastActivityAt", lastActivityAt);
        json.put("endedAt", endedAt);
        if (parentSessionId != null) json.put("parentSessionId", parentSessionId);
        if (metadata != null) json.put("metadata", metadata);
        return json;
    }
}
