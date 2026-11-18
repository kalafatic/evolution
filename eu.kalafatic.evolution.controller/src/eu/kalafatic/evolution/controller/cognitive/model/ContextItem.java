package eu.kalafatic.evolution.controller.cognitive.model;

import org.json.JSONObject;

/**
 * One context contribution with provenance: what it says, where it came from,
 * how much it can be trusted, and how fresh it is.
 */
public class ContextItem {
    private String id;
    private String category;
    private String content;
    private String sourceRef;
    private double confidence = 1.0;
    private long freshness;
    private int tokens;
    private boolean stale;
    private JSONObject extra;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public long getFreshness() { return freshness; }
    public void setFreshness(long freshness) { this.freshness = freshness; }
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }
    public JSONObject getExtra() { return extra; }
    public void setExtra(JSONObject extra) { this.extra = extra; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        if (id != null) json.put("id", id);
        if (category != null) json.put("category", category);
        if (content != null) json.put("content", content);
        if (sourceRef != null) json.put("sourceRef", sourceRef);
        json.put("confidence", confidence);
        json.put("freshness", freshness);
        json.put("tokens", tokens);
        json.put("stale", stale);
        if (extra != null) json.put("extra", extra);
        return json;
    }

    public static ContextItem fromJsonObject(JSONObject json) {
        if (json == null) return null;
        ContextItem item = new ContextItem();
        if (json.has("id")) item.setId(json.getString("id"));
        if (json.has("category")) item.setCategory(json.getString("category"));
        if (json.has("content")) item.setContent(json.getString("content"));
        if (json.has("sourceRef")) item.setSourceRef(json.optString("sourceRef", null));
        if (json.has("confidence")) item.setConfidence(json.getDouble("confidence"));
        if (json.has("freshness")) item.setFreshness(json.getLong("freshness"));
        if (json.has("tokens")) item.setTokens(json.getInt("tokens"));
        if (json.has("stale")) item.setStale(json.getBoolean("stale"));
        if (json.has("extra")) item.setExtra(json.getJSONObject("extra"));
        return item;
    }
}
