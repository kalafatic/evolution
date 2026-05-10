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

        // 2. WORKFLOW
        if (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE) {
            profile.addTrait(BehaviorTrait.WORKFLOW_SELF_DEV);
        } else {
            profile.addTrait(BehaviorTrait.WORKFLOW_TASK_ORIENTED);
        }

        // 3. REASONING
        if (orchestrator.isDarwinMode() || (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.DARWIN_MODE)) {
            profile.addTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);
        } else {
            profile.addTrait(BehaviorTrait.REASONING_ATOMIC);
        }

        // 4. INTERACTION
        if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null
            && orchestrator.getAiChat().getPromptInstructions().isStepMode()) {
            profile.addTrait(BehaviorTrait.INTERACTION_STEP_MODE);
        }

        return profile;
    }
}
