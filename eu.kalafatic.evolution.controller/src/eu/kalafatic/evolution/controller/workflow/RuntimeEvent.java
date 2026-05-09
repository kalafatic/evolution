package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.HashMap;

public class RuntimeEvent {
    private final RuntimeEventType type;
    private final String sessionId;
    private final String source;
    private final Object payload;
    private final Map<String, Object> metadata = new HashMap<>();

    public RuntimeEvent(RuntimeEventType type, String sessionId, String source, Object payload) {
        this.type = type;
        this.sessionId = sessionId;
        this.source = source;
        this.payload = payload;
    }

    public RuntimeEventType getType() { return type; }
    public String getSessionId() { return sessionId; }
    public String getSource() { return source; }
    public Object getPayload() { return payload; }
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
}
