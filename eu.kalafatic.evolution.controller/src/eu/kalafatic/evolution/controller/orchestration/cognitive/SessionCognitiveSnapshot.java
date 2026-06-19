package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialized snapshot of the cognitive state for persistence and event broadcasting.
 */
public class SessionCognitiveSnapshot {
    private String sessionId;
    private CapabilityType capability;
    private SessionIntent intent;
    private CognitiveDirection direction;
    private double confidence;
    private double velocity;
    private double stability;
    private List<CapabilityType> trajectory;
    private int depth;
    private Map<CapabilityType, Double> scores;
    private List<CapabilitySignal> history;

    public SessionCognitiveSnapshot() {}

    public SessionCognitiveSnapshot(SessionCognitiveState state) {
        this.sessionId = state.getSessionId();
        this.capability = state.getCurrentCapability();
        this.intent = state.getCurrentIntent();
        this.direction = state.getCurrentDirection();
        this.confidence = state.getConfidence();
        this.velocity = state.getVelocity();
        this.stability = state.getTrendStability();
        this.trajectory = new ArrayList<>(state.getTrajectory());
        this.depth = state.getCognitiveDepth();
        this.scores = new HashMap<>(state.getCapabilityScores());
        this.history = new ArrayList<>(state.getCapabilityHistory());
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public CapabilityType getCapability() { return capability; }
    public void setCapability(CapabilityType capability) { this.capability = capability; }

    public SessionIntent getIntent() { return intent; }
    public void setIntent(SessionIntent intent) { this.intent = intent; }

    public CognitiveDirection getDirection() { return direction; }
    public void setDirection(CognitiveDirection direction) { this.direction = direction; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public double getVelocity() { return velocity; }
    public void setVelocity(double velocity) { this.velocity = velocity; }

    public double getStability() { return stability; }
    public void setStability(double stability) { this.stability = stability; }

    public List<CapabilityType> getTrajectory() { return trajectory; }
    public void setTrajectory(List<CapabilityType> trajectory) { this.trajectory = trajectory; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public Map<CapabilityType, Double> getScores() { return scores; }
    public void setScores(Map<CapabilityType, Double> scores) { this.scores = scores; }

    public List<CapabilitySignal> getHistory() { return history; }
    public void setHistory(List<CapabilitySignal> history) { this.history = history; }

    public org.json.JSONObject toJSON() {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("sessionId", sessionId);
        json.put("capability", capability != null ? capability.name() : "CHAT");
        json.put("intent", intent != null ? intent.name() : "LEARNING");
        json.put("direction", direction != null ? direction.name() : "STABLE");
        json.put("confidence", confidence);
        json.put("velocity", velocity);
        json.put("stability", stability);
        json.put("depth", depth);

        org.json.JSONArray trajArr = new org.json.JSONArray();
        if (trajectory != null) {
            for (CapabilityType t : trajectory) trajArr.put(t.name());
        }
        json.put("trajectory", trajArr);

        org.json.JSONObject scoresObj = new org.json.JSONObject();
        if (scores != null) {
            for (Map.Entry<CapabilityType, Double> entry : scores.entrySet()) {
                scoresObj.put(entry.getKey().name(), entry.getValue());
            }
        }
        json.put("scores", scoresObj);

        org.json.JSONArray historyArr = new org.json.JSONArray();
        if (history != null) {
            for (CapabilitySignal s : history) {
                org.json.JSONObject sObj = new org.json.JSONObject();
                sObj.put("capability", s.getCapability() != null ? s.getCapability().name() : "CHAT");
                sObj.put("weight", s.getWeight());
                sObj.put("intent", s.getIntent() != null ? s.getIntent().name() : "LEARNING");
                sObj.put("source", s.getSource());
                historyArr.put(sObj);
            }
        }
        json.put("history", historyArr);

        return json;
    }
}
