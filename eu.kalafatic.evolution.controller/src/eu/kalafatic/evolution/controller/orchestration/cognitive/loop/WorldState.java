package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-scoped representation of the system's understanding of world state,
 * known facts, artifacts, goal progress, and capability availability.
 */
public class WorldState {

    private final String sessionId;
    private final Map<String, Object> facts = new ConcurrentHashMap<>();
    private final Map<String, Object> artifacts = new ConcurrentHashMap<>();
    private final List<CognitiveObservation> observations = Collections.synchronizedList(new ArrayList<>());
    private final List<CognitiveDecision> decisionHistory = Collections.synchronizedList(new ArrayList<>());
    private double currentProgress = 0.0;
    private String activeStrategy = "DEFAULT";

    public WorldState(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setFact(String key, Object value) {
        if (key != null && value != null) {
            facts.put(key, value);
        }
    }

    public Object getFact(String key) {
        return facts.get(key);
    }

    public Map<String, Object> getFacts() {
        return Collections.unmodifiableMap(facts);
    }

    public void addArtifact(String name, Object artifact) {
        if (name != null && artifact != null) {
            artifacts.put(name, artifact);
        }
    }

    public Map<String, Object> getArtifacts() {
        return Collections.unmodifiableMap(artifacts);
    }

    public void addObservation(CognitiveObservation obs) {
        if (obs != null) {
            observations.add(obs);
        }
    }

    public List<CognitiveObservation> getObservations() {
        return Collections.unmodifiableList(new ArrayList<>(observations));
    }

    public void addDecision(CognitiveDecision decision) {
        if (decision != null) {
            decisionHistory.add(decision);
        }
    }

    public List<CognitiveDecision> getDecisionHistory() {
        return Collections.unmodifiableList(new ArrayList<>(decisionHistory));
    }

    public double getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(double currentProgress) {
        this.currentProgress = Math.max(0.0, Math.min(1.0, currentProgress));
    }

    public String getActiveStrategy() {
        return activeStrategy;
    }

    public void setActiveStrategy(String activeStrategy) {
        this.activeStrategy = activeStrategy != null ? activeStrategy : "DEFAULT";
    }

    @Override
    public String toString() {
        return "WorldState{" +
                "sessionId='" + sessionId + '\'' +
                ", progress=" + String.format("%.2f", currentProgress) +
                ", activeStrategy='" + activeStrategy + '\'' +
                ", factsCount=" + facts.size() +
                ", observationsCount=" + observations.size() +
                '}';
    }
}
