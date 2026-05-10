package eu.kalafatic.evolution.controller.orchestration.behavior;

public class ExploratoryReasoningModule implements InstructionModule {
    @Override
    public String getInstructions() {
        return "REASONING: EXPLORATORY\n" +
               "→ Generate 2-3 competing variants with different strategic directions.\n" +
               "→ Explore alternative architectures and innovative solutions.\n" +
               "→ Evaluate tradeoffs between approaches in your hypotheses.";
    }
}
