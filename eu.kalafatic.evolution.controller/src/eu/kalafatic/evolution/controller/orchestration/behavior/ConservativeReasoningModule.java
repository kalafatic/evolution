package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.stream.Collectors;

public class ConservativeReasoningModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.CONSERVATIVE) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("REASONING: CONSERVATIVE (Exploration Level: ").append(policy.getExplorationLevel()).append(")\n")
          .append("→ Prioritize safety and minimal changes.\n")
          .append("→ Avoid large-scale refactorings unless absolutely necessary.\n")
          .append("→ Ensure all changes are well-tested and have low risk.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
