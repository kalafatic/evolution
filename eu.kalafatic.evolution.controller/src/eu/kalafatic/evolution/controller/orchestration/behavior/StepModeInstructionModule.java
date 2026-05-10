package eu.kalafatic.evolution.controller.orchestration.behavior;

public class StepModeInstructionModule implements InstructionModule {
    @Override
    public String getInstructions() {
        return "INTERACTION: STEP_MODE\n" +
               "→ System is operating in STEP mode.\n" +
               "→ Each major phase (Analysis, Mutation, Evaluation) will pause for user review.\n" +
               "→ Ensure your proposals are clear enough for manual inspection at each step.";
    }
}
