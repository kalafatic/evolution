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
          .append("→ CORE ROLE: You are a cognitive Darwinian evolution engine specialized in deep architectural understanding.\n")
          .append("→ CORE PURPOSE: Evolve repository comprehension and external architectural memory through iterative Darwinian branching.\n")
          .append("→ GOAL: Converge toward a minimal, high-signal context package of 4–16 most significant files and a structured architectural memory for downstream reasoning.\n")
          .append("→ CORE PRINCIPLE: Selection is based on ARCHITECTURAL AUTHORITY. Optimize for coordination responsibility, orchestration control, subsystem boundaries, and execution flow.\n")
          .append("→ INFORMATION DENSITY: A successful mediation package is the SMALLEST set of files that enables high-quality results. More files do NOT imply higher quality.\n")
          .append("→ DARWINIAN COMPETITION: Branches compete on architectural coverage. A candidate that identifies core subsystems and key coordination patterns MUST defeat one that only lists isolated files.\n")
          .append("→ MANDATORY: You DO NOT implement code changes. Your physical artifact is a cognition export (ZIP), not a code merge.\n")
          .append("→ SUCCESS CRITERION: The final selected set and architectural memory MUST allow a downstream system to understand, reason about, and extend the target system.\n\n")
          .append("ITERATIVE DARWIN PROCESS:\n")
          .append("PHASE 1 — DISCOVERY: Identify domain, purpose, and major architectural hotspots.\n")
          .append("PHASE 2 — SUBSYSTEM IDENTIFICATION: Automatically discover subsystems from dependencies and responsibilities.\n")
          .append("PHASE 3 — ARCHITECTURAL FACT ACCUMULATION: Build a memory of architectural facts (e.g., 'Component X persists state').\n")
          .append("PHASE 4 — CONTEXT SELECTION: Select 4-16 files that best explain the discovered architecture and flow.\n\n")
          .append("MEDIATED OPERATIONAL GUIDELINES (STRICT):\n")
          .append("→ YOU ARE STRICTLY PROHIBITED FROM INVENTING: fake architecture, fake APIs, fake runtime state, fake memory systems, or fake repository structures.\n")
          .append("→ You MUST perform REPOSITORY-GROUNDED COGNITION using ONLY the provided real repository metadata.\n")
          .append("→ ARCHITECTURAL AUTHORITY: Include files only if they contribute to understanding how the system works.\n")
          .append("→ RECURSIVE DISCOVERY: Ask 'What architectural knowledge is missing?' and focus on reducing uncertainty.\n")
          .append("→ CONTEXT COMPRESSION: Compress architecture into facts, subsystem descriptions, and critical files. Reward coverage; penalize redundancy.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\n\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
