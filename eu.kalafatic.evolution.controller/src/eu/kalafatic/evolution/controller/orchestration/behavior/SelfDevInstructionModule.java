package eu.kalafatic.evolution.controller.orchestration.behavior;

public class SelfDevInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        // This module is typically used when ReasoningStrategy is EXPLORATORY or ANALYTICAL
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.EXPLORATORY &&
            policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.ANALYTICAL) {
            return "";
        }

        return "WORKFLOW: SELF-DEVELOPMENT\n" +
               "→ You are operating in a self-development lifecycle (Exploration Level: " + policy.getExplorationLevel() + ").\n" +
               "→ Your goal is to improve the system's own codebase, architecture, or documentation.\n" +
               "→ Prioritize structural integrity and long-term maintainability.";
    }
}
