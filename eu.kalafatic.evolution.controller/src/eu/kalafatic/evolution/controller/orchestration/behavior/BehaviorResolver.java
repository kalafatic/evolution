package eu.kalafatic.evolution.controller.orchestration.behavior;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class BehaviorResolver {

    public BehaviorProfile resolve(TaskContext context) {
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
