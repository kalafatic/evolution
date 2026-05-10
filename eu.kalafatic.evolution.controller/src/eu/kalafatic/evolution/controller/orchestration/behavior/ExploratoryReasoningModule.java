package eu.kalafatic.evolution.controller.orchestration.behavior;

public class ExploratoryReasoningModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.EXPLORATORY) {
            return "";
        }

        return "REASONING: EXPLORATORY\n" +
               "→ You are encouraged to explore novel solutions and architectural improvements.\n" +
               "→ Focus on discovery and expanding system capabilities.\n" +
               "→ Be proactive in identifying areas for optimization.";
    }
}
