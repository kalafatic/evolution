package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Resolves behavior profiles and bit-states using dynamic policies instead of hardcoded rules.
 */
public class BehaviorResolver {

    public long resolveBitState(TaskContext context) {
        Orchestrator orchestrator = context.getOrchestrator();

        // Base encoding derived from model
        int mode = BitState.MODE_LOCAL;
        if (orchestrator.getAiMode() != null) {
            mode = orchestrator.getAiMode().ordinal(); // Assumes ordinal mapping for now
        }

        int supervision = BitState.SUPERVISION_AUTO;
        int interaction = BitState.INTERACTION_CONTINUOUS;
        int reasoning = BitState.REASONING_ATOMIC;
        int workflow = BitState.WORKFLOW_TASK_ORIENTED;
        long options = 0;

        // Apply policy-driven overrides
        for (IBehaviorPolicy policy : BehaviorPolicyRegistry.getPolicies()) {
            if (policy.applies(context)) {
                options |= policy.getBitOptions(context);
            }
        }

        // Handle instruction-based overrides (still dynamic via EMF model)
        if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
            var instructions = orchestrator.getAiChat().getPromptInstructions();
            if (instructions.isStepMode()) interaction = BitState.INTERACTION_STEP;
            if (instructions.isAutoApprove()) options |= BitState.OPTION_AUTO_APPROVE;
        }

        return BitState.encode(mode, supervision, interaction, reasoning, workflow, (int)options);
    }

    public BehaviorProfile resolve(TaskContext context) {
        BehaviorProfile profile = new BehaviorProfile();

        // Dynamic policy-driven trait assignment
        for (IBehaviorPolicy policy : BehaviorPolicyRegistry.getPolicies()) {
            if (policy.applies(context)) {
                policy.apply(profile, context);
            }
        }

        // Default traits if none assigned
        if (profile.getTraits().isEmpty()) {
            profile.addTrait(BehaviorTrait.SUPERVISION_AUTONOMOUS);
            profile.addTrait(BehaviorTrait.EXECUTION_AUTOMATIC);
            profile.addTrait(BehaviorTrait.REASONING_ATOMIC);
            profile.addTrait(BehaviorTrait.WORKFLOW_TASK_ORIENTED);
        }

        return profile;
    }
}
