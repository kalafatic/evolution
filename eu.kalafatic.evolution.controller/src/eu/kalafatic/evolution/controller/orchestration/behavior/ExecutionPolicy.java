package eu.kalafatic.evolution.controller.orchestration.behavior;

import eu.kalafatic.evolution.model.orchestration.AiMode;

/**
 * Structured semantic orchestration policy.
 */
public class ExecutionPolicy {
    public enum ExecutionMode { LOCAL, HYBRID, REMOTE, PROXY, MEDIATED }
    public enum SupervisionLevel { AUTO, MANUAL, HYBRID }
    public enum InteractionMode { CONTINUOUS, STEP, GUIDED }
    public enum ReasoningStrategy { ATOMIC, DARWIN, CONSERVATIVE, EXPLORATORY, ANALYTICAL }

    private ExecutionMode executionMode;
    private SupervisionLevel supervisionLevel;
    private InteractionMode interactionMode;
    private ReasoningStrategy reasoningStrategy;

    public ExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(ExecutionMode executionMode) { this.executionMode = executionMode; }

    public SupervisionLevel getSupervisionLevel() { return supervisionLevel; }
    public void setSupervisionLevel(SupervisionLevel supervisionLevel) { this.supervisionLevel = supervisionLevel; }

    public InteractionMode getInteractionMode() { return interactionMode; }
    public void setInteractionMode(InteractionMode interactionMode) { this.interactionMode = interactionMode; }

    public ReasoningStrategy getReasoningStrategy() { return reasoningStrategy; }
    public void setReasoningStrategy(ReasoningStrategy reasoningStrategy) { this.reasoningStrategy = reasoningStrategy; }
}
