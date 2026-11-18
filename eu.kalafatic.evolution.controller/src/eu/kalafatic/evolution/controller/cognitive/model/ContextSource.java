package eu.kalafatic.evolution.controller.cognitive.model;

import org.json.JSONObject;

/**
 * Provenance of one contribution inside a ContextPackage.
 */
public class ContextSource {
    private String sourceType;
    private String sourceId;
    private String contribution;
    private double confidence = 1.0;
    private long freshness;

    public ContextSource() {}

    public ContextSource(String sourceType, String sourceId, String contribution, double confidence, long freshness) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.contribution = contribution;
        this.confidence = confidence;
        this.freshness = freshness;
    }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getContribution() { return contribution; }
    public void setContribution(String contribution) { this.contribution = contribution; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public long getFreshness() { return freshness; }
    public void setFreshness(long freshness) { this.freshness = freshness; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        if (sourceType != null) json.put("sourceType", sourceType);
        if (sourceId != null) json.put("sourceId", sourceId);
        if (contribution != null) json.put("contribution", contribution);
        json.put("confidence", confidence);
        json.put("freshness", freshness);
        return json;
    }

    public static ContextSource fromJsonObject(JSONObject json) {
        if (json == null) return null;
        ContextSource s = new ContextSource();
        if (json.has("sourceType")) s.setSourceType(json.getString("sourceType"));
        if (json.has("sourceId")) s.setSourceId(json.optString("sourceId", null));
        if (json.has("contribution")) s.setContribution(json.optString("contribution", null));
        if (json.has("confidence")) s.setConfidence(json.getDouble("confidence"));
        if (json.has("freshness")) s.setFreshness(json.getLong("freshness"));
        return s;
    }
}
