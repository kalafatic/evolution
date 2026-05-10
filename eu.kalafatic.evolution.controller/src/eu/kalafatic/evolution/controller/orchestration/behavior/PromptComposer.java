package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for merging instruction modules and injecting context to produce the final prompt.
 */
public class PromptComposer {

    public String compose(ExecutionPolicy policy, List<InstructionModule> modules, String stateContext) {
        StringBuilder sb = new StringBuilder();

        String instructionText = modules.stream()
                .map(m -> m.getInstructions(policy))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n\n"));

        if (instructionText.isEmpty()) {
            sb.append("Your task is to propose the best STRATEGY to fulfill the user's goal by reasoning over STATE and FEEDBACK.\n\n");
        } else {
            sb.append(instructionText).append("\n\n");
        }

        sb.append("PRIMARY OBJECTIVE:\n")
          .append("→ Propose 2-3 distinct candidate state transitions (strategies) to achieve the goal.\n")
          .append("→ CRITICAL: Fulfillment of the current goal is the HIGHEST priority.\n")
          .append("→ If the goal is ANALYTICAL (e.g., 'analyze project'), use ANALYZE operations in 'structure' or 'test' domains.\n\n");

        sb.append("STATE MODEL:\n")
          .append("→ Use the provided StateSnapshot (build, tests, coverage) to inform decisions.\n")
          .append("→ Analyze relationships between elements (files, modules, tests, failures, dependencies).\n")
          .append("→ Identify weak points (failures, instability, complexity).\n")
          .append("→ Propose actions that improve overall system health and achieve the target goal.\n\n")
          .append("FAILURE FINGERPRINTING & ANTI-LOOP:\n")
          .append("→ Avoid repeating actions that lead to the same failure fingerprints.\n")
          .append("→ If a failure is REPEATING (count >= 2), you MUST change your strategy.\n\n")
          .append("HYPOTHESIS-DRIVEN VARIANTS:\n")
          .append("→ Every variant MUST include a hypothesis: a causal explanation of why the proposed changes will lead to the expected effects.\n")
          .append("→ Expected effects must be measurable outcomes (e.g., 'build success', 'test X passes', 'new class prints text').\n\n")
          .append("TRAJECTORY AWARENESS:\n")
          .append("→ Consider the build/test trends. Prefer variants that improve ANY dimension.\n\n")
          .append("PRIORITY LOGIC:\n")
          .append("→ IF build == FAIL → focus on build fixes.\n")
          .append("→ ELSE IF tests failing → focus on test fixes.\n")
          .append("→ ELSE → fulfillment of the current goal.\n\n")
          .append("ITERATION STRATEGY (DARWINIAN):\n")
          .append("→ Generate 2–3 DIFFERENT candidate state transitions.\n")
          .append("→ Each candidate must represent a distinct strategy.\n")
          .append("→ Avoid cosmetic changes, repeated failed approaches, or low-impact modifications.");

        if (stateContext != null && !stateContext.isEmpty()) {
            sb.append("\n\n--- CURRENT CONTEXT ---\n").append(stateContext);
        }

        return sb.toString();
    }
}
