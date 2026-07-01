package eu.kalafatic.evolution.controller.orchestration.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents one reasoning event inside the trace graph.
 */
public final class CausalNode {
    private final String nodeId;
    private final String nodeType;
    private final String sourceComponent;
    private final List<String> inputReferences;
    private final List<String> outputReferences;
    private final double confidence;
    private final String rationale;
    private long timestamp;
    private final Map<String, Object> metadata;

    @com.fasterxml.jackson.annotation.JsonCreator
    public CausalNode(@com.fasterxml.jackson.annotation.JsonProperty("nodeId") String nodeId,
                      @com.fasterxml.jackson.annotation.JsonProperty("nodeType") String nodeType,
                      @com.fasterxml.jackson.annotation.JsonProperty("sourceComponent") String sourceComponent,
                      @com.fasterxml.jackson.annotation.JsonProperty("inputReferences") List<String> inputReferences,
                      @com.fasterxml.jackson.annotation.JsonProperty("outputReferences") List<String> outputReferences,
                      @com.fasterxml.jackson.annotation.JsonProperty("confidence") double confidence,
                      @com.fasterxml.jackson.annotation.JsonProperty("rationale") String rationale) {
        this(nodeId, nodeType, sourceComponent, inputReferences, outputReferences, confidence, rationale, new HashMap<>());
    }

    public CausalNode(String nodeId, String nodeType, String sourceComponent,
                      List<String> inputReferences, List<String> outputReferences,
                      double confidence, String rationale, Map<String, Object> metadata) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.sourceComponent = sourceComponent;
        this.inputReferences = inputReferences != null ? Collections.unmodifiableList(new ArrayList<>(inputReferences)) : Collections.emptyList();
        this.outputReferences = outputReferences != null ? Collections.unmodifiableList(new ArrayList<>(outputReferences)) : Collections.emptyList();
        this.confidence = confidence;
        this.rationale = rationale;
        this.timestamp = System.currentTimeMillis();
        this.metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<>(metadata)) : Collections.emptyMap();
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNodeId() { return nodeId; }
    public String getNodeType() { return nodeType; }
    public String getSourceComponent() { return sourceComponent; }
    public List<String> getInputReferences() { return inputReferences; }
    public List<String> getOutputReferences() { return outputReferences; }
    public double getConfidence() { return confidence; }
    public String getRationale() { return rationale; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("nodeId", nodeId);
        json.put("nodeType", nodeType);
        json.put("sourceComponent", sourceComponent);
        json.put("inputReferences", new JSONArray(inputReferences));
        json.put("outputReferences", new JSONArray(outputReferences));
        json.put("confidence", confidence);
        json.put("rationale", rationale);
        json.put("timestamp", timestamp);
        json.put("metadata", new JSONObject(metadata));
        return json;
    }

    public static CausalNode fromJson(JSONObject json) {
        Map<String, Object> meta = new HashMap<>();
        JSONObject mObj = json.optJSONObject("metadata");
        if (mObj != null) {
            for (Object keyObj : mObj.keySet()) {
                String key = (String) keyObj;
                meta.put(key, mObj.get(key));
            }
        }

        List<String> inputs = new ArrayList<>();
        JSONArray iArr = json.optJSONArray("inputReferences");
        if (iArr != null) {
            for (int i = 0; i < iArr.length(); i++) inputs.add(iArr.getString(i));
        }

        List<String> outputs = new ArrayList<>();
        JSONArray oArr = json.optJSONArray("outputReferences");
        if (oArr != null) {
            for (int i = 0; i < oArr.length(); i++) outputs.add(oArr.getString(i));
        }

        return new CausalNode(
            json.getString("nodeId"),
            json.getString("nodeType"),
            json.getString("sourceComponent"),
            inputs,
            outputs,
            json.getDouble("confidence"),
            json.getString("rationale"),
            meta
        );
    }

    @Override
    public String toString() {
        return String.format("CausalNode[id=%s, type=%s, component=%s, confidence=%.2f, rationale=%s]",
                nodeId, nodeType, sourceComponent, confidence, rationale);
    }
}
