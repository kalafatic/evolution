package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.List;

/**
 * Result of a Cognitive Loop run.
 */
public class CognitiveResult {
    private final String sessionId;
    private final CognitiveGoal goal;
    private final CognitiveState finalState;
    private final List<CognitiveObservation> observations;
    private final List<CognitiveDecision> decisions;
    private final int attempts;
    private final int darwinInvocations;
    private final String summary;
    private final long totalExecutionTimeMs;

    public CognitiveResult(String sessionId, CognitiveGoal goal, CognitiveState finalState,
                           List<CognitiveObservation> observations, List<CognitiveDecision> decisions,
                           int attempts, int darwinInvocations, String summary, long totalExecutionTimeMs) {
        this.sessionId = sessionId;
        this.goal = goal;
        this.finalState = finalState;
        this.observations = observations != null ? List.copyOf(observations) : Collections.emptyList();
        this.decisions = decisions != null ? List.copyOf(decisions) : Collections.emptyList();
        this.attempts = attempts;
        this.darwinInvocations = darwinInvocations;
        this.summary = summary != null ? summary : "";
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }

    public boolean isSuccess() {
        return finalState == CognitiveState.SUCCESS;
    }

    public String getSessionId() {
        return sessionId;
    }

    public CognitiveGoal getGoal() {
        return goal;
    }

    public CognitiveState getFinalState() {
        return finalState;
    }

    public List<CognitiveObservation> getObservations() {
        return observations;
    }

    public List<CognitiveDecision> getDecisions() {
        return decisions;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getDarwinInvocations() {
        return darwinInvocations;
    }

    public String getSummary() {
        return summary;
    }

    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }

    @Override
    public String toString() {
        return "CognitiveResult{" +
                "sessionId='" + sessionId + '\'' +
                ", finalState=" + finalState +
                ", attempts=" + attempts +
                ", darwinInvocations=" + darwinInvocations +
                ", summary='" + summary + '\'' +
                ", timeMs=" + totalExecutionTimeMs +
                '}';
    }
}
