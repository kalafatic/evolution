package eu.kalafatic.evolution.controller.orchestration.behavior;

public class ConservativeReasoningModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.CONSERVATIVE) {
            return "";
        }

        return "REASONING: CONSERVATIVE\n" +
               "→ Prioritize safety and minimal changes.\n" +
               "→ Avoid large-scale refactorings unless absolutely necessary.\n" +
               "→ Ensure all changes are well-tested and have low risk.";
    }
}
