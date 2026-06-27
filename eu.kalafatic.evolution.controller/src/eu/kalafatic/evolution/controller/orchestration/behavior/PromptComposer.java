package eu.kalafatic.evolution.controller.orchestration.behavior;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for modular prompt composition following the EVO Platform Vision.
 */
public class PromptComposer {

    public String compose(ExecutionPolicy policy, List<InstructionModule> modules, String stateContext) {
        StringBuilder sb = new StringBuilder();

        sb.append(composeSystem(policy)).append("\n\n");
        sb.append(composeBehavior(policy, modules)).append("\n\n");
        sb.append(composeContext(stateContext)).append("\n\n");

        return sb.toString();
    }

    public String composeSystem(ExecutionPolicy policy) {
        return "🔴 SYSTEM / ROLE\n\n" +
               "You are an evolutionary engineering agent. Your goal is to mutate the system state to achieve a target objective while maintaining architectural integrity and lineage continuity.";
    }

    public String composeGoal(String goal) {
        return "🎯 PRIMARY GOAL\n\n" + goal;
    }

    public String composeBehavior(ExecutionPolicy policy, List<InstructionModule> modules) {
        StringBuilder sb = new StringBuilder();
        sb.append("🧠 BEHAVIOR PROFILE\n\n");

        String instructionText = modules.stream()
                .map(m -> m.getInstructions(policy))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n\n"));

        if (instructionText.isEmpty()) {
            sb.append("Propose a precise engineering strategy to achieve the user's goal.\n");
        } else {
            sb.append(instructionText);
        }

        sb.append("\n\nPROPOSAL GUIDELINES:\n")
          .append("→ CRITICAL: Fulfillment of the current goal is the HIGHEST priority.\n")
          .append("→ IF build == FAIL → focus on build fixes.\n")
          .append("→ ELSE IF tests failing → focus on test fixes.\n")
          .append("→ Avoid repeating failed approaches (anti-loop).\n")
          .append("→ Propose structurally distinct transitions.");

        return sb.toString();
    }

    public String composeContext(String stateContext) {
        if (stateContext == null || stateContext.isEmpty()) return "";
        return "📂 CURRENT CONTEXT / REALITY\n\n" + stateContext;
    }

    public String composeLineage(String lineageMemory) {
        if (lineageMemory == null || lineageMemory.isEmpty()) return "";
        return "🧬 CUMULATIVE LINEAGE MEMORY\n\n" + lineageMemory;
    }

    public String composeSiblingMemory(String siblingMemory) {
        if (siblingMemory == null || siblingMemory.isEmpty()) return "";
        return "🚫 SIBLING REJECTION MEMORY (HARD CONSTRAINT)\n\n" +
               "The following architectural regions are already occupied by siblings in this iteration. YOU MUST NOT OVERLAP WITH THESE.\n\n" +
               siblingMemory;
    }

    public String composeConstraints(String constraints) {
        if (constraints == null || constraints.isEmpty()) return "";
        return "⚠️ EVOLUTION CONSTRAINTS\n\n" + constraints;
    }

    public String composeJsonSchema(String schema) {
        return "🎯 OUTPUT FORMAT (STRICT JSON)\n\n" +
               "Return ONLY a JSON object within <BEGIN_DARWIN_JSON> and <END_DARWIN_JSON> tags.\n\n" +
               schema;
    }
}
