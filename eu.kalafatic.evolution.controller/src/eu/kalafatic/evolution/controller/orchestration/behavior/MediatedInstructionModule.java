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
          .append("→ You are in REPOSITORY-AWARE MEDIATED mode using ").append(policy.getRepositoryMode()).append(" repository.\n")
          .append("→ Focus on iterative understanding and improvement planning.\n")
          .append("→ Generate competing ANALYSES and PROPOSALS for architectural refinement.\n")
          .append("→ All execution remains human-supervised; do not assume automatic deployment.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
