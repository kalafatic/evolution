package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.stream.Collectors;

public class StepModeInstructionModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getInteractionMode() != ExecutionPolicy.InteractionMode.STEP) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("INTERACTION: STEP MODE (Supervision: ").append(policy.getSupervisionLevel()).append(")\n")
          .append("→ Execution is paused after each step for human review.\n")
          .append("→ Provide clear, granular actions that can be easily verified.\n")
          .append("→ Assume each action will be explicitly approved or rejected.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
