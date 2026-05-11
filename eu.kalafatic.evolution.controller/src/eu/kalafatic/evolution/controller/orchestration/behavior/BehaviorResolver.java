package eu.kalafatic.evolution.controller.orchestration.behavior;

import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class BehaviorResolver {

    public long resolveBitState(TaskContext context) {
        int mode = BitState.MODE_LOCAL;
        int supervision = BitState.SUPERVISION_AUTO;
        int interaction = BitState.INTERACTION_CONTINUOUS;
        int reasoning = BitState.REASONING_ATOMIC;
        int workflow = BitState.WORKFLOW_TASK_ORIENTED;

        Orchestrator orchestrator = context.getOrchestrator();

        // 1. MODE & SUPERVISION
        if (orchestrator.getAiMode() == AiMode.MEDIATED) {
            mode = BitState.MODE_MEDIATED;
            supervision = BitState.SUPERVISION_MANUAL;
        } else if (orchestrator.getAiMode() == AiMode.HYBRID) {
            mode = BitState.MODE_HYBRID;
            supervision = BitState.SUPERVISION_HYBRID;
        } else if (orchestrator.getAiMode() == AiMode.REMOTE) {
            mode = BitState.MODE_REMOTE;
        }

        // 2. REASONING
        PlatformType type = context.getPlatformMode() != null ? context.getPlatformMode().getType() : PlatformType.SIMPLE_CHAT;

        switch (type) {
            case SELF_DEV_MODE:
                reasoning = BitState.REASONING_EXPLORATORY;
                workflow = BitState.WORKFLOW_SELF_DEV;
                break;
            case DARWIN_MODE:
                reasoning = BitState.REASONING_DARWIN;
                workflow = BitState.WORKFLOW_TASK_ORIENTED;
                break;
            case ASSISTED_CODING:
                reasoning = BitState.REASONING_CONSERVATIVE;
                workflow = BitState.WORKFLOW_TASK_ORIENTED;
                break;
            case HYBRID_MANUAL_EXPORT:
                reasoning = BitState.REASONING_ANALYTICAL;
                workflow = BitState.WORKFLOW_EXPORT_ONLY;
                break;
            case SIMPLE_CHAT:
            default:
                reasoning = BitState.REASONING_ATOMIC;
                workflow = BitState.WORKFLOW_TASK_ORIENTED;
                break;
        }

        if (orchestrator.isDarwinMode()) {
            reasoning = BitState.REASONING_DARWIN;
        }

        // 3. INTERACTION
        if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
            var instructions = orchestrator.getAiChat().getPromptInstructions();
            if (instructions.isStepMode()) {
                interaction = BitState.INTERACTION_STEP;
            }
            if (instructions.isIterativeMode()) {
                reasoning = BitState.REASONING_DARWIN;
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
