package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.HashMap;
import eu.kalafatic.evolution.controller.trajectory.EventCategory;

/**
 * EvolutionEvent (EEL)
 *
 * This is NOT a replacement for logs.
 * It is a structured overlay extracted from existing events/logs where possible.
 */
public class EvolutionEvent {
    public enum EELType {
        RUNTIME,
        EVOLUTION,
        FITNESS,
        SYSTEM,
        COGNITIVE
    }

    public enum CausalityType {
        DIRECT,     // Explicit linkage (correlationId, parentEventId)
        STRUCTURAL, // Grouped by iteration/phase
        INFERRED    // Heuristic correlation
    }

    private final EELType eventType;
    private final String component;
    private final String correlationId;
    private final String sessionId;
    private final String iterationId;
    private final String taskId;
    private final String commitHash;

    private String severity;
    private String stateTransition;
    private String cause;
    private String impact;
    private String evolutionSignal;
    private CausalityType causalityType = CausalityType.INFERRED;
    private double confidenceScore = 0.5;

    private final long timestamp;
    private final Map<String, Object> metadata = new HashMap<>();

    public EvolutionEvent(EELType eventType, String component, String sessionId, String correlationId, String iterationId, String taskId, String commitHash) {
        this.eventType = eventType;
        this.component = component;
        this.sessionId = sessionId;
        this.correlationId = correlationId;
        this.iterationId = iterationId;
        this.taskId = taskId;
        this.commitHash = commitHash;
        this.timestamp = System.currentTimeMillis();
    }

    public EELType getEventType() { return eventType; }
    public String getComponent() { return component; }
    public String getCorrelationId() { return correlationId; }
    public String getSessionId() { return sessionId; }
    public String getIterationId() { return iterationId; }
    public String getTaskId() { return taskId; }
    public String getCommitHash() { return commitHash; }
    public long getTimestamp() { return timestamp; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStateTransition() { return stateTransition; }
    public void setStateTransition(String stateTransition) { this.stateTransition = stateTransition; }

    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }

    public String getEvolutionSignal() { return evolutionSignal; }
    public void setEvolutionSignal(String evolutionSignal) { this.evolutionSignal = evolutionSignal; }

    public CausalityType getCausalityType() { return causalityType; }
    public void setCausalityType(CausalityType causalityType) { this.causalityType = causalityType; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Map<String, Object> getMetadata() { return metadata; }

    public EvolutionEvent withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
}
