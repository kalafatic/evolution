package eu.kalafatic.evolution.controller.orchestration.behavior;

import eu.kalafatic.evolution.controller.orchestration.behavior.ExecutionPolicy.*;

/**
 * Resolves semantic ExecutionPolicy from raw bitfield state.
 */
public class PolicyResolver {

    public ExecutionPolicy resolve(long bitState) {
        ExecutionPolicy policy = new ExecutionPolicy();

        // Resolve Execution Mode
        int mode = BitState.getMode(bitState);
        switch (mode) {
            case BitState.MODE_HYBRID: policy.setExecutionMode(ExecutionMode.HYBRID); break;
            case BitState.MODE_REMOTE: policy.setExecutionMode(ExecutionMode.REMOTE); break;
            case BitState.MODE_PROXY: policy.setExecutionMode(ExecutionMode.PROXY); break;
            case BitState.MODE_MEDIATED: policy.setExecutionMode(ExecutionMode.MEDIATED); break;
            case BitState.MODE_LOCAL:
            default: policy.setExecutionMode(ExecutionMode.LOCAL); break;
        }

        // Resolve Supervision Level
        int supervision = BitState.getSupervision(bitState);
        switch (supervision) {
            case BitState.SUPERVISION_MANUAL: policy.setSupervisionLevel(SupervisionLevel.MANUAL); break;
            case BitState.SUPERVISION_HYBRID: policy.setSupervisionLevel(SupervisionLevel.HYBRID); break;
            case BitState.SUPERVISION_AUTO:
            default: policy.setSupervisionLevel(SupervisionLevel.AUTO); break;
        }

        // Resolve Interaction Mode
        int interaction = BitState.getInteraction(bitState);
        switch (interaction) {
            case BitState.INTERACTION_STEP: policy.setInteractionMode(InteractionMode.STEP); break;
            case BitState.INTERACTION_GUIDED: policy.setInteractionMode(InteractionMode.GUIDED); break;
            case BitState.INTERACTION_CONTINUOUS:
            default: policy.setInteractionMode(InteractionMode.CONTINUOUS); break;
        }

        // Resolve Reasoning Strategy
        int reasoning = BitState.getReasoning(bitState);
        switch (reasoning) {
            case BitState.REASONING_DARWIN: policy.setReasoningStrategy(ReasoningStrategy.DARWIN); break;
            case BitState.REASONING_CONSERVATIVE: policy.setReasoningStrategy(ReasoningStrategy.CONSERVATIVE); break;
            case BitState.REASONING_EXPLORATORY: policy.setReasoningStrategy(ReasoningStrategy.EXPLORATORY); break;
            case BitState.REASONING_ANALYTICAL: policy.setReasoningStrategy(ReasoningStrategy.ANALYTICAL); break;
            case BitState.REASONING_ATOMIC:
            default: policy.setReasoningStrategy(ReasoningStrategy.ATOMIC); break;
        }

        return policy;
    }
}
