package eu.kalafatic.evolution.controller.memory;

import org.json.JSONObject;

/**
 * Directed relationship between two memory entries.
 */
public class MemoryRelationship {

    public enum Kind {
        SUPERSEDES,
        CONTRADICTS,
        SUPPORTS,
        DERIVES_FROM,
        REINFORCES
    }

    private String fromEntryId;
    private String toEntryId;
    private Kind kind;
    private long createdAt;

    public MemoryRelationship() {
        this.createdAt = System.currentTimeMillis();
    }

    public MemoryRelationship(String fromEntryId, String toEntryId, Kind kind) {
        this();
        this.fromEntryId = fromEntryId;
        this.toEntryId = toEntryId;
        this.kind = kind != null ? kind : Kind.SUPPORTS;
    }

    public String getFromEntryId() { return fromEntryId; }
    public void setFromEntryId(String fromEntryId) { this.fromEntryId = fromEntryId; }
    public String getToEntryId() { return toEntryId; }
    public void setToEntryId(String toEntryId) { this.toEntryId = toEntryId; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("fromEntryId", fromEntryId);
        json.put("toEntryId", toEntryId);
        json.put("kind", kind != null ? kind.name() : Kind.SUPPORTS.name());
        json.put("createdAt", createdAt);
        return json;
    }

    public static MemoryRelationship fromJsonObject(JSONObject json) {
        if (json == null) return null;
        MemoryRelationship rel = new MemoryRelationship();
        if (json.has("fromEntryId")) rel.setFromEntryId(json.getString("fromEntryId"));
        if (json.has("toEntryId")) rel.setToEntryId(json.getString("toEntryId"));
        if (json.has("kind")) {
            try { rel.setKind(Kind.valueOf(json.getString("kind"))); } catch (Exception ignored) {}
        }
        if (json.has("createdAt")) rel.setCreatedAt(json.getLong("createdAt"));
        return rel;
    }
}
