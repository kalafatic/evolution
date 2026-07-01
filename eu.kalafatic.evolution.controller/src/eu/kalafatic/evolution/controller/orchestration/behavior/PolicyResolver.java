package eu.kalafatic.evolution.controller.orchestration.behavior;

import eu.kalafatic.evolution.controller.orchestration.behavior.ExecutionPolicy.*;

/**
 * Resolves semantic ExecutionPolicy from raw bitfield state using a rule-based engine.
 */
public class PolicyResolver {

    private static final ExecutionMode[] MODES = {
        ExecutionMode.LOCAL, ExecutionMode.HYBRID, ExecutionMode.REMOTE, ExecutionMode.PROXY, ExecutionMode.MEDIATED
    };

    private static final SupervisionLevel[] SUPERVISIONS = {
        SupervisionLevel.AUTO, SupervisionLevel.MANUAL, SupervisionLevel.HYBRID
    };

    private static final InteractionMode[] INTERACTIONS = {
        InteractionMode.CONTINUOUS, InteractionMode.STEP, InteractionMode.GUIDED
    };

    private static final ReasoningStrategy[] REASONING = {
        ReasoningStrategy.ATOMIC, ReasoningStrategy.DARWIN, ReasoningStrategy.ITERATIVE, ReasoningStrategy.CONSERVATIVE, ReasoningStrategy.EXPLORATORY, ReasoningStrategy.ANALYTICAL
    };

    private static final WorkflowModel[] WORKFLOWS = {
        WorkflowModel.TASK_ORIENTED, WorkflowModel.SELF_DEV, WorkflowModel.HYBRID, WorkflowModel.EXPORT_ONLY
    };

    public ExecutionPolicy resolve(long bitState) {
        // STAGE 1: RAW DECODE (lookup-based mapping)
        ExecutionPolicy policy = decode(bitState);

        // STAGE 2: POLICY RULE ENGINE
        applyRules(policy);

        // STAGE 3: POLICY NORMALIZATION
        normalize(policy);

        return policy;
    }

    private ExecutionPolicy decode(long bitState) {
        ExecutionPolicy policy = new ExecutionPolicy();

        int modeIdx = BitState.getMode(bitState);
        policy.setExecutionMode(modeIdx >= 0 && modeIdx < MODES.length ? MODES[modeIdx] : ExecutionMode.LOCAL);

        int supIdx = BitState.getSupervision(bitState);
        policy.setSupervisionLevel(supIdx >= 0 && supIdx < SUPERVISIONS.length ? SUPERVISIONS[supIdx] : SupervisionLevel.AUTO);

        int intIdx = BitState.getInteraction(bitState);
        policy.setInteractionMode(intIdx >= 0 && intIdx < INTERACTIONS.length ? INTERACTIONS[intIdx] : InteractionMode.CONTINUOUS);

        int reasIdx = BitState.getReasoning(bitState);
        policy.setReasoningStrategy(reasIdx >= 0 && reasIdx < REASONING.length ? REASONING[reasIdx] : ReasoningStrategy.ATOMIC);

        int wfIdx = BitState.getWorkflow(bitState);
        policy.setWorkflowModel(wfIdx >= 0 && wfIdx < WORKFLOWS.length ? WORKFLOWS[wfIdx] : WorkflowModel.TASK_ORIENTED);

        return policy;
    }

    private void applyRules(ExecutionPolicy policy) {
        for (PolicyRule rule : RuleRegistry.getRules()) {
            rule.apply(policy);
        }
    }

    private void normalize(ExecutionPolicy policy) {
        // Enforce consistent supervision for Mediated mode
        if (policy.getExecutionMode() == ExecutionMode.MEDIATED) {
            if (policy.getSupervisionLevel() == SupervisionLevel.AUTO) {
                policy.setSupervisionLevel(SupervisionLevel.MANUAL);
            }
        }

        // Ensure DARWIN always has a minimum exploration level
        if (policy.getReasoningStrategy() == ReasoningStrategy.DARWIN) {
            if (policy.getExplorationLevel() < 0.4) {
                policy.setExplorationLevel(0.4);
            }
        }
    }
}
