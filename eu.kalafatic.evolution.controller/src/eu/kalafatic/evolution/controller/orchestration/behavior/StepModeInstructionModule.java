package eu.kalafatic.evolution.controller.orchestration.behavior;

public class StepModeInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getInteractionMode() != ExecutionPolicy.InteractionMode.STEP) {
            return "";
        }

        return "INTERACTION: STEP MODE\n" +
               "→ Execution is paused after each step for human review.\n" +
               "→ Provide clear, granular actions that can be easily verified.\n" +
               "→ Assume each action will be explicitly approved or rejected.";
    }
}
