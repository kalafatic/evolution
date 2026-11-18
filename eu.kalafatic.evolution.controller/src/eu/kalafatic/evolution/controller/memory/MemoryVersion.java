package eu.kalafatic.evolution.controller.memory;

import org.json.JSONObject;

/**
 * One immutable historical version of a memory entry. Appended (JSONL) on
 * every mutation; the current entry always references the latest version.
 */
public class MemoryVersion {
    private String entryId;
    private int version;
    private String contentSnapshot;
    private String validity;
    private long changedAt;
    private String changedBy;

    public MemoryVersion() {}

    public MemoryVersion(String entryId, int version, String contentSnapshot, String validity, long changedAt, String changedBy) {
        this.entryId = entryId;
        this.version = version;
        this.contentSnapshot = contentSnapshot;
        this.validity = validity;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getContentSnapshot() { return contentSnapshot; }
    public void setContentSnapshot(String contentSnapshot) { this.contentSnapshot = contentSnapshot; }
    public String getValidity() { return validity; }
    public void setValidity(String validity) { this.validity = validity; }
    public long getChangedAt() { return changedAt; }
    public void setChangedAt(long changedAt) { this.changedAt = changedAt; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("entryId", entryId);
        json.put("version", version);
        json.put("contentSnapshot", contentSnapshot != null ? contentSnapshot : "");
        json.put("validity", validity != null ? validity : "VALID");
        json.put("changedAt", changedAt);
        if (changedBy != null) json.put("changedBy", changedBy);
        return json;
    }

    public static MemoryVersion fromJsonObject(JSONObject json) {
        if (json == null) return null;
        MemoryVersion v = new MemoryVersion();
        if (json.has("entryId")) v.setEntryId(json.getString("entryId"));
        if (json.has("version")) v.setVersion(json.getInt("version"));
        if (json.has("contentSnapshot")) v.setContentSnapshot(json.getString("contentSnapshot"));
        if (json.has("validity")) v.setValidity(json.getString("validity"));
        if (json.has("changedAt")) v.setChangedAt(json.getLong("changedAt"));
        if (json.has("changedBy")) v.setChangedBy(json.getString("changedBy"));
        return v;
    }
}
