package eu.kalafatic.evolution.controller.orchestration.dto;

import java.util.Map;
import java.util.HashMap;

public class EvolutionSession {
    private final String sessionId;
    private int iterationCount;
    private String currentPhase;
    private final Map<String, Object> metadata = new HashMap<>();

    public EvolutionSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }
    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }
    public Map<String, Object> getMetadata() { return metadata; }
}
