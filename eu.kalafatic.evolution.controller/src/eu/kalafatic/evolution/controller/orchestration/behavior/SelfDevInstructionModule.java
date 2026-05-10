package eu.kalafatic.evolution.controller.orchestration.behavior;

public class SelfDevInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        // This module is typically used when ReasoningStrategy is EXPLORATORY or ANALYTICAL
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.EXPLORATORY &&
            policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.ANALYTICAL) {
            return "";
        }

        return "WORKFLOW: SELF-DEVELOPMENT (Exploration Level: " + policy.getExplorationLevel() + ")\n" +
               "→ Your goal is to improve the task itself before execution.\n" +
               "→ Refine requirements, constraints, and architectural expectations.\n" +
               "→ Transform weak human requests into executable engineering objectives.\n" +
               "→ Prioritize structural integrity, long-term maintainability, and validation criteria.";
    }
}
