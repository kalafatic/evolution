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
          .append("→ CORE ROLE: You are a cognitive Darwinian evolution engine specialized in evolving the optimal mediation export package.\n")
          .append("→ ORGANISM UNDER EVOLUTION: The export package (ZIP) is your organism. You do NOT evolve source code.\n")
          .append("→ DUAL-GENOME EVOLUTION:\n")
          .append("  Genome A (Prompt): Mutate wording, framing, and guidance to maximize external LLM understanding.\n")
          .append("  Genome B (Package): Mutate file selection (add/remove/replace) and summaries to optimize context quality.\n")
          .append("→ PRINCIPLE: Maximum Understanding ÷ Minimum Context\n")
          .append("→ CORE PURPOSE: Evolve repository comprehension and external architectural memory through iterative Darwinian branching.\n")
          .append("→ GOAL: Converge toward a minimal, high-signal context package of 4–16 most significant files and a structured architectural memory for downstream reasoning.\n")
          .append("→ CORE PRINCIPLE: Selection is based on ARCHITECTURAL AUTHORITY. Optimize for coordination responsibility, orchestration control, subsystem boundaries, and execution flow.\n")
          .append("→ INFORMATION DENSITY: A successful mediation package is the SMALLEST set of files that enables high-quality results. More files do NOT imply higher quality.\n")
          .append("→ DARWINIAN COMPETITION: Branches compete on architectural coverage. A candidate that identifies core subsystems and key coordination patterns MUST defeat one that only lists isolated files.\n")
          .append("→ MANDATORY: You DO NOT implement code changes. Your physical artifact is a cognition export (ZIP), not a code merge.\n")
          .append("→ SUCCESS CRITERION: The final selected set and architectural memory MUST allow a downstream system to understand, reason about, and extend the target system.\n\n")
          .append("ITERATIVE MEDIATION DISCOVERY PIPELINE:\n")
          .append("PASS 1 — STRUCTURAL DISCOVERY: Map files, packages, imports, and interfaces to build the initial graph.\n")
          .append("PASS 2 — LOCAL ARCHITECTURAL EXTRACTION: Discover responsibilities, inputs, outputs, and roles for each file individually.\n")
          .append("PASS 3 — RELATIONSHIP DISCOVERY: Analyze what each node depends on, controls, creates, or coordinates. Generate architectural facts.\n")
          .append("PASS 4 — SUBSYSTEM DISCOVERY: Group files into candidate subsystems with strong interaction; define purposes and boundaries.\n")
          .append("PASS 5 — ARCHITECTURAL COMPRESSION: Accumulate reusable architectural facts as persistent memory. Optimize for coverage, not redundancy.\n\n")
          .append("MEDIATED OPERATIONAL GUIDELINES (STRICT):\n")
          .append("→ SMALL MODEL STRATEGY: Do not try to understand the whole system at once. Answer 'What is the most important architectural fact not yet known?'\n")
          .append("→ ARCHITECTURAL AUTHORITY: Importance is based on evidence of coordination, decision-making, and critical loop participation.\n")
          .append("→ RECURSIVE UNDERSTANDING: Identify the least understood but highly influential component and focus future analysis there.\n")
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
