package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.HashMap;

public class RuntimeEvent {
    private final RuntimeEventType type;
    private final String sessionId;
    private final String branchId;
    private final String iterationId;
    private final String source;
    private final Object payload;
    private final long timestamp;
    private final Map<String, Object> metadata = new HashMap<>();

    public RuntimeEvent(RuntimeEventType type, String sessionId, String source, Object payload) {
        this(type, sessionId, null, null, source, payload, System.currentTimeMillis());
    }

    public RuntimeEvent(RuntimeEventType type, String sessionId, String branchId, String iterationId, String source, Object payload, long timestamp) {
        this.type = type;
        this.sessionId = sessionId;
        this.branchId = branchId;
        this.iterationId = iterationId;
        this.source = source;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public RuntimeEventType getType() { return type; }
    public String getSessionId() { return sessionId; }
    public String getBranchId() { return branchId; }
    public String getIterationId() { return iterationId; }
    public String getSource() { return source; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }

    public RuntimeEvent withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public RuntimeEvent withParent(String parentId) {
        this.metadata.put("parentId", parentId);
        return this;
    }

    public RuntimeEvent withEntityId(String entityId) {
        this.metadata.put("entityId", entityId);
        return this;
    }

    public String getEntityId() {
        return (String) metadata.get("entityId");
    }

    public String getAction() {
        return (String) payload;
    }
}
