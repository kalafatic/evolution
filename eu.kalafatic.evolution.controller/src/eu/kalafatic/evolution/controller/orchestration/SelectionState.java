package eu.kalafatic.evolution.controller.orchestration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages metadata and decisions for a specific session.
 */
public class SelectionState {
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private final Map<String, String> decisions = new ConcurrentHashMap<>();

    public void putMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Map<String, Object> getAllMetadata() {
        return metadata;
    }

    public void recordDecision(String decisionId, String result) {
        decisions.put(decisionId, result);
    }

    public String getDecision(String decisionId) {
        return decisions.get(decisionId);
    }

    public Map<String, String> getAllDecisions() {
        return decisions;
    }
}
