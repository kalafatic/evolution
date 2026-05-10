package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.stream.Collectors;

public class DarwinIterativeInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.DARWIN) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("REASONING: DARWIN ITERATIVE / EVOLUTIONARY OPTIMIZATION\n")
          .append("→ ITERATION MODE: Recursively improve prompt quality through ambiguity detection, scope reduction, and dependency discovery.\n")
          .append("→ DARWIN MODE: Generate multiple internal candidate interpretations (e.g., conservative, architecture-preserving, minimal-delta).\n")
          .append("→ SELECTION CRITERIA: Lowest architectural risk, smallest implementation surface, and highest consistency with existing patterns.\n")
          .append("→ SURVIVAL CRITERIA: Minimal context, maximal relevance, and deterministic execution path.\n")
          .append("→ MANDATORY: Follow the 5-phase evolution lifecycle (Intent → Architecture → Refinement → Planning → Synthesis).\n")
          .append("→ Do NOT attempt final implementation until Phase 5.\n")
          .append("→ Stop iterating when the prompt becomes implementation-ready and architectural scope is coherent.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\n\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
