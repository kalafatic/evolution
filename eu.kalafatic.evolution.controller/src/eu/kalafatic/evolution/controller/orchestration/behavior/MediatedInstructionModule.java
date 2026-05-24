package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.stream.Collectors;

public class MediatedInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getExecutionMode() != ExecutionPolicy.ExecutionMode.MEDIATED) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SUPERVISION: MEDIATED (").append(policy.getSupervisionLevel()).append(")\n")
          .append("→ CORE ROLE: You are a cognitive Darwinian evolution engine specialized in repository understanding.\n")
          .append("→ CORE PURPOSE: Evolve repository comprehension and external cognition preparation through iterative Darwinian branching.\n")
          .append("→ GOAL: Converge toward the BEST POSSIBLE external LLM package (prompt + metadata + optimized context).\n")
          .append("→ DARWINIAN COMPETITION: Branches compete on repository comprehension, architectural insight, context efficiency, and prompt clarity.\n")
          .append("→ MANDATORY: You DO NOT implement code changes. Your physical artifact is a cognition export (ZIP), not a code merge.\n")
          .append("→ OUTPUT: 1. Refined architectural understanding, 2. Optimized context selection, 3. Synthesized high-signal prompt.\n\n")
          .append("MEDIATED OPERATIONAL GUIDELINES:\n")
          .append("→ Strictly evolve repository interpretations through competing futures (Architecture Mapping vs. Dependency Exploration, etc.).\n")
          .append("→ Determine what the external LLM SHOULD see to maximize its reasoning density.\n")
          .append("→ Prioritize high-signal files and structural metadata over bulk repository inclusion.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\n\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
