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
          .append("→ INFORMATION DENSITY: A successful mediation package is the SMALLEST set of files that enables high-quality results. More files do NOT imply higher quality.\n")
          .append("→ DARWINIAN COMPETITION: Branches compete on context quality. A candidate with 8 highly relevant files MUST defeat one with 40 partially relevant files.\n")
          .append("→ MANDATORY: You DO NOT implement code changes. Your physical artifact is a cognition export (ZIP), not a code merge.\n")
          .append("→ OUTPUT: 1. Refined architectural understanding, 2. Optimized context selection (4-16 files), 3. Synthesized high-signal prompt.\n\n")
          .append("MEDIATED OPERATIONAL GUIDELINES (STRICT):\n")
          .append("→ YOU ARE STRICTLY PROHIBITED FROM INVENTING: fake architecture, fake APIs, fake runtime state, fake memory systems, or fake repository structures.\n")
          .append("→ You MUST perform REPOSITORY-GROUNDED COGNITION using ONLY the provided real repository metadata.\n")
          .append("→ Strictly evolve repository interpretations through competing futures (Architecture Mapping vs. Dependency Exploration, etc.).\n")
          .append("→ FILE SELECTION PRINCIPLE: Select 4–16 highly relevant files that collectively explain the subsystem. Avoid bloat, redundancy, and low-signal utilities.\n")
          .append("→ CONTEXT COMPRESSION: Aggressively compress project knowledge into a high-signal context set. Reward relevance and architectural significance; penalize bloat.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\n\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
