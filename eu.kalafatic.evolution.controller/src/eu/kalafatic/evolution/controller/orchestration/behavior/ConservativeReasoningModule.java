package eu.kalafatic.evolution.controller.orchestration.behavior;

public class ConservativeReasoningModule implements InstructionModule {
    @Override
    public String getInstructions() {
        return "REASONING: CONSERVATIVE\n" +
               "→ Generate only 1 or 2 very safe, low-risk variants.\n" +
               "→ Avoid complex architectural changes or risky refactoring.\n" +
               "→ Focus on direct, minimal fulfillment of the target goal.";
    }
}
