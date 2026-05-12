package eu.kalafatic.evolution.controller.orchestration.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;

/**
 * Represents a complete causal chain of reasoning events for an orchestration lifecycle.
 */
public final class CognitiveTrace {
    private final String traceId;
    private final String iterationId;
    private final String lineageId;
    private final List<CausalNode> causalChain = new ArrayList<>();
    private final Map<String, Long> timestamps = new HashMap<>();
    private final List<String> involvedComponents = new ArrayList<>();
    private final List<EvaluationSignal> associatedSignals = new ArrayList<>();
    private String finalOutcome;
    private final Map<Long, Double> confidenceEvolution = new HashMap<>();

    public CognitiveTrace(String traceId, String iterationId, String lineageId) {
        this.traceId = traceId;
        this.iterationId = iterationId;
        this.lineageId = lineageId;
        this.timestamps.put("START", System.currentTimeMillis());
    }

    public String getTraceId() { return traceId; }
    public String getIterationId() { return iterationId; }
    public String getLineageId() { return lineageId; }

    public synchronized void addNode(CausalNode node) {
        this.causalChain.add(node);
        if (!involvedComponents.contains(node.getSourceComponent())) {
            involvedComponents.add(node.getSourceComponent());
        }
        confidenceEvolution.put(node.getTimestamp(), node.getConfidence());
    }

    public synchronized void addSignal(EvaluationSignal signal) {
        this.associatedSignals.add(signal);
    }

    public List<CausalNode> getCausalChain() {
        return Collections.unmodifiableList(new ArrayList<>(causalChain));
    }

    public List<String> getInvolvedComponents() {
        return Collections.unmodifiableList(new ArrayList<>(involvedComponents));
    }

    public List<EvaluationSignal> getAssociatedSignals() {
        return Collections.unmodifiableList(new ArrayList<>(associatedSignals));
    }

    public String getFinalOutcome() { return finalOutcome; }
    public void setFinalOutcome(String finalOutcome) {
        this.finalOutcome = finalOutcome;
        this.timestamps.put("END", System.currentTimeMillis());
    }

    public Map<Long, Double> getConfidenceEvolution() {
        return Collections.unmodifiableMap(new HashMap<>(confidenceEvolution));
    }

    public Map<String, Long> getTimestamps() {
        return Collections.unmodifiableMap(new HashMap<>(timestamps));
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("traceId", traceId);
        json.put("iterationId", iterationId);
        json.put("lineageId", lineageId);
        json.put("finalOutcome", finalOutcome);

        JSONArray nodes = new JSONArray();
        for (CausalNode node : causalChain) nodes.put(node.toJson());
        json.put("causalChain", nodes);

        json.put("timestamps", new JSONObject(timestamps));
        json.put("involvedComponents", new JSONArray(involvedComponents));

        JSONObject conf = new JSONObject();
        for (Map.Entry<Long, Double> entry : confidenceEvolution.entrySet()) {
            conf.put(entry.getKey().toString(), entry.getValue());
        }
        json.put("confidenceEvolution", conf);

        return json;
    }

    public static CognitiveTrace fromJson(JSONObject json) {
        CognitiveTrace trace = new CognitiveTrace(
            json.getString("traceId"),
            json.getString("iterationId"),
            json.getString("lineageId")
        );
        trace.setFinalOutcome(json.optString("finalOutcome"));

        JSONArray nodes = json.optJSONArray("causalChain");
        if (nodes != null) {
            for (int i = 0; i < nodes.length(); i++) {
                trace.addNode(CausalNode.fromJson(nodes.getJSONObject(i)));
            }
        }

        return trace;
    }
}
