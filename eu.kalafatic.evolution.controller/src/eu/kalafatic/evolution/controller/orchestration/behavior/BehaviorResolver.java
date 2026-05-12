package eu.kalafatic.evolution.controller.orchestration.behavior;

import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class BehaviorResolver {

    public long resolveBitState(TaskContext context) {
        Orchestrator orchestrator = context.getOrchestrator();

        // 1. MODE (Strictly from model)
        int mode = BitState.MODE_LOCAL;
        if (orchestrator.getAiMode() != null) {
            switch (orchestrator.getAiMode()) {
                case LOCAL:    mode = BitState.MODE_LOCAL; break;
                case HYBRID:   mode = BitState.MODE_HYBRID; break;
                case REMOTE:   mode = BitState.MODE_REMOTE; break;
                case PROXY:    mode = BitState.MODE_PROXY; break;
                case MEDIATED: mode = BitState.MODE_MEDIATED; break;
            }
        }

        // 2. SUPERVISION (Default based on mode, but overridable)
        int supervision = BitState.SUPERVISION_AUTO;
        if (mode == BitState.MODE_MEDIATED) {
            supervision = BitState.SUPERVISION_MANUAL;
        } else if (mode == BitState.MODE_HYBRID) {
            supervision = BitState.SUPERVISION_HYBRID;
        }

        // 3. REASONING
        int reasoning = BitState.REASONING_ATOMIC;
        PlatformType type = context.getPlatformMode() != null ? context.getPlatformMode().getType() : PlatformType.SIMPLE_CHAT;

        switch (type) {
            case SELF_DEV_MODE:
                reasoning = BitState.REASONING_EXPLORATORY;
                break;
            case DARWIN_MODE:
                reasoning = BitState.REASONING_DARWIN;
                break;
            case ASSISTED_CODING:
                reasoning = BitState.REASONING_CONSERVATIVE;
                break;
            case HYBRID_MANUAL_EXPORT:
                reasoning = BitState.REASONING_ANALYTICAL;
                break;
            case SIMPLE_CHAT:
            default:
                reasoning = BitState.REASONING_ATOMIC;
                break;
        }

        if (orchestrator.isDarwinMode()) {
            reasoning = BitState.REASONING_DARWIN;
        }

        // 4. WORKFLOW
        int workflow = BitState.WORKFLOW_TASK_ORIENTED;
        if (type == PlatformType.SELF_DEV_MODE) {
            workflow = BitState.WORKFLOW_SELF_DEV;
        } else if (type == PlatformType.HYBRID_MANUAL_EXPORT) {
            workflow = BitState.WORKFLOW_EXPORT_ONLY;
        }

        // 5. INTERACTION & ITERATIVE Overrides
        int interaction = BitState.INTERACTION_CONTINUOUS;
        if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
            var instructions = orchestrator.getAiChat().getPromptInstructions();
            if (instructions.isStepMode()) {
                interaction = BitState.INTERACTION_STEP;
            }
            if (instructions.isIterativeMode()) {
                // If iterative but NOT darwin, use ITERATIVE reasoning
                if (reasoning != BitState.REASONING_DARWIN) {
                    reasoning = BitState.REASONING_ITERATIVE;
                }
            }
            if (instructions.isSelfIterativeMode()) {
                workflow = BitState.WORKFLOW_SELF_DEV;
            }
        }

        return BitState.encode(mode, supervision, interaction, reasoning, workflow);
    }

    public BehaviorProfile resolve(TaskContext context) {
        // Legacy support - can be removed later if not needed
        BehaviorProfile profile = new BehaviorProfile();
        Orchestrator orchestrator = context.getOrchestrator();

        // 1. SUPERVISION
        if (orchestrator.getAiMode() == AiMode.MEDIATED) {
            profile.addTrait(BehaviorTrait.SUPERVISION_MEDIATED);
            profile.addTrait(BehaviorTrait.EXECUTION_MANUAL);
        } else {
            profile.addTrait(BehaviorTrait.SUPERVISION_AUTONOMOUS);
            profile.addTrait(BehaviorTrait.EXECUTION_AUTOMATIC);
        }

        // 2. WORKFLOW & REASONING
        PlatformType type = context.getPlatformMode() != null ? context.getPlatformMode().getType() : PlatformType.SIMPLE_CHAT;

        switch (type) {
            case SELF_DEV_MODE:
                profile.addTrait(BehaviorTrait.WORKFLOW_SELF_DEV);
                profile.addTrait(BehaviorTrait.REASONING_EXPLORATORY);
                break;
            case DARWIN_MODE:
                profile.addTrait(BehaviorTrait.WORKFLOW_TASK_ORIENTED);
                profile.addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
                profile.addTrait(BehaviorTrait.REASONING_EXPLORATORY);
                break;
            case ASSISTED_CODING:
                profile.addTrait(BehaviorTrait.WORKFLOW_TASK_ORIENTED);
                profile.addTrait(BehaviorTrait.REASONING_CONSERVATIVE);
                break;
            case HYBRID_MANUAL_EXPORT:
                profile.addTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);
                profile.addTrait(BehaviorTrait.WORKFLOW_HYBRID);
                break;
            case SIMPLE_CHAT:
            default:
                profile.addTrait(BehaviorTrait.WORKFLOW_TASK_ORIENTED);
                profile.addTrait(BehaviorTrait.REASONING_ATOMIC);
                break;
        }

        // Apply Orchestrator-level overrides
        if (orchestrator.isDarwinMode()) {
            profile.addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
        }

        // 4. INTERACTION
        if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
            var instructions = orchestrator.getAiChat().getPromptInstructions();
            if (instructions.isStepMode()) {
                profile.addTrait(BehaviorTrait.INTERACTION_STEP_MODE);
                profile.addTrait(BehaviorTrait.SUPERVISION_STEP_LOCKED);
            }
            if (instructions.isIterativeMode()) {
                profile.addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
            }
        }

        return profile;
    }
}
