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
          .append("→ ITERATION MODE: Recursively improve implementation or mediation quality through multi-dimensional refinement.\n")
          .append("→ DARWIN MODE: Generate multiple internal candidate interpretations representing different engineering trade-offs. Sibling variants MUST differ in multiple dimensions: class names, package organization, API design, method signatures, parameters, public vs private methods, inheritance vs composition, implementation strategy, naming conventions, coding style, comments/documentation, modularization, performance, readability, extensibility, dependency choices.\n")
          .append("→ MEDIATED GENOME MUTATION: When in Mediated mode, mutate both Genome A (Prompt) and Genome B (Package/Files) to discover the most implementation-ready external context.\n")
          .append("→ SELECTION CRITERIA: Highest fitness across all quality dimensions, lowest architectural risk, and highest consistency with existing patterns.\n")
          .append("→ SURVIVAL CRITERIA: Minimal context, maximal relevance, and deterministic execution path.\n")
          .append("→ MANDATORY: Follow the 5-phase evolution lifecycle (Intent → Architecture → Refinement → Planning → Synthesis).\n")
          .append("→ Stop iterating when the prompt becomes implementation-ready and architectural scope is coherent.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\n\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
