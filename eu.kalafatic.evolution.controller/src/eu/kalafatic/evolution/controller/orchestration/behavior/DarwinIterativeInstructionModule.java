package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.stream.Collectors;

public class DarwinIterativeInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.DARWIN) {
            return "";
        }

        return "REASONING: DARWIN ITERATIVE\n" +
               "→ Use a Darwinian approach: generate multiple competing variants.\n" +
               "→ Each variant should explore a different hypothesis or strategy.\n" +
               "→ MANDATORY: Follow the 5-phase evolution lifecycle (Intent → Architecture → Refinement → Planning → Synthesis).\n" +
               "→ Do NOT attempt final implementation until Phase 5.\n" +
               "→ Evaluate variants based on predicted success and phase-specific alignment.";
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
