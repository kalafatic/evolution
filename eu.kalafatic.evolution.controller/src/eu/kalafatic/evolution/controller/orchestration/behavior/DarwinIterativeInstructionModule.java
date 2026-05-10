package eu.kalafatic.evolution.controller.orchestration.behavior;

public class DarwinIterativeInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.DARWIN) {
            return "";
        }

        return "REASONING: DARWIN ITERATIVE\n" +
               "→ Use a Darwinian approach: generate multiple competing variants.\n" +
               "→ Each variant should explore a different hypothesis or strategy.\n" +
               "→ MANDATORY: Follow the 5-phase evolution lifecycle (Intent → Architecture → Refinement → Planning → Synthesis).\n" +
               "→ Do NOT attempt final implementation until Phase 5.\n" +
               "→ Evaluate variants based on predicted success and phase-specific alignment.";
    }
}
