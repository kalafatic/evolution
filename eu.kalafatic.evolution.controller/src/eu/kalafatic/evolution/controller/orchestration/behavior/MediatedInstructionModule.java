package eu.kalafatic.evolution.controller.orchestration.behavior;

public class MediatedInstructionModule implements InstructionModule {
    @Override
    public String getInstructions() {
        return "SUPERVISION: MEDIATED\n" +
               "→ You are in REPOSITORY-AWARE MEDIATED mode.\n" +
               "→ Focus on iterative understanding and improvement planning.\n" +
               "→ Generate competing ANALYSES and PROPOSALS for architectural refinement.\n" +
               "→ All execution remains human-supervised; do not assume automatic deployment.";
    }
}
