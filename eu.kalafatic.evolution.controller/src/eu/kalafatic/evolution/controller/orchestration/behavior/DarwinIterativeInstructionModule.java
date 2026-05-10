package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.stream.Collectors;

public class DarwinIterativeInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.DARWIN) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("REASONING: DARWIN ITERATIVE (Exploration Level: ").append(policy.getExplorationLevel()).append(")\n")
          .append("→ Use a Darwinian approach: generate multiple competing variants.\n")
          .append("→ Each variant should explore a different hypothesis or strategy.\n")
          .append("→ Evaluate variants based on predicted success and system impact.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
