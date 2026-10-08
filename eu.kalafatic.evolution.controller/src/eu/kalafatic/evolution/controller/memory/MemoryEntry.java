package eu.kalafatic.evolution.controller.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;

/**
 * Represents a single persistent user or project memory entry.
 */
public class MemoryEntry {
    private String id;
    private String content;
    private MemoryScope scope;
    private MemoryType type;
    private MemorySource source;
    private MemoryImportance importance;
    private double confidence;
    private long createdAt;
    private long updatedAt;
    private String associatedId;
    private Map<String, String> metadata;

    public MemoryEntry() {
        this.id = UUID.randomUUID().toString();
        this.scope = MemoryScope.USER;
        this.type = MemoryType.FACT;
        this.source = MemorySource.USER;
        this.importance = MemoryImportance.HIGH;
        this.confidence = 1.0;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.metadata = new HashMap<>();
    }

    public MemoryEntry(String content, MemoryScope scope, MemoryType type, MemorySource source, MemoryImportance importance) {
        this();
        this.content = content;
        this.scope = scope != null ? scope : MemoryScope.USER;
        this.type = type != null ? type : MemoryType.FACT;
        this.source = source != null ? source : MemorySource.USER;
        this.importance = importance != null ? importance : MemoryImportance.HIGH;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemoryScope getScope() {
        return scope;
    }

    public void setScope(MemoryScope scope) {
        this.scope = scope;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemoryType getType() {
        return type;
    }

    public void setType(MemoryType type) {
        this.type = type;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemorySource getSource() {
        return source;
    }

    public void setSource(MemorySource source) {
        this.source = source;
        this.updatedAt = System.currentTimeMillis();
    }

    public MemoryImportance getImportance() {
        return importance;
    }

    public void setImportance(MemoryImportance importance) {
        this.importance = importance;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAssociatedId() {
        return associatedId;
    }

    public void setAssociatedId(String associatedId) {
        this.associatedId = associatedId;
    }

    public Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("content", content != null ? content : "");
        json.put("scope", scope != null ? scope.name() : MemoryScope.USER.name());
        json.put("type", type != null ? type.name() : MemoryType.FACT.name());
        json.put("source", source != null ? source.name() : MemorySource.USER.name());
        json.put("importance", importance != null ? importance.name() : MemoryImportance.HIGH.name());
        json.put("confidence", confidence);
        json.put("createdAt", createdAt);
        json.put("updatedAt", updatedAt);
        if (associatedId != null) {
            json.put("associatedId", associatedId);
        }
        if (metadata != null && !metadata.isEmpty()) {
            json.put("metadata", new JSONObject(metadata));
        }
        return json;
    }

    public static MemoryEntry fromJsonObject(JSONObject json) {
        if (json == null) return null;
        MemoryEntry entry = new MemoryEntry();
        if (json.has("id")) entry.setId(json.getString("id"));
        if (json.has("content")) entry.setContent(json.getString("content"));
        if (json.has("scope")) {
            try { entry.setScope(MemoryScope.valueOf(json.getString("scope"))); } catch (Exception ignored) {}
        }
        if (json.has("type")) {
            try { entry.setType(MemoryType.valueOf(json.getString("type"))); } catch (Exception ignored) {}
        }
        if (json.has("source")) {
            try { entry.setSource(MemorySource.valueOf(json.getString("source"))); } catch (Exception ignored) {}
        }
        if (json.has("importance")) {
            try { entry.setImportance(MemoryImportance.valueOf(json.getString("importance"))); } catch (Exception ignored) {}
        }
        if (json.has("confidence")) entry.setConfidence(json.getDouble("confidence"));
        if (json.has("createdAt")) entry.setCreatedAt(json.getLong("createdAt"));
        if (json.has("updatedAt")) entry.setUpdatedAt(json.getLong("updatedAt"));
        if (json.has("associatedId")) entry.setAssociatedId(json.getString("associatedId"));
        if (json.has("metadata")) {
            JSONObject metaObj = json.getJSONObject("metadata");
            Map<String, String> metaMap = new HashMap<>();
            for (Object keyObj : metaObj.keySet()) {
                String key = String.valueOf(keyObj);
                metaMap.put(key, metaObj.getString(key));
            }
            entry.setMetadata(metaMap);
        }
        return entry;
    }
}
