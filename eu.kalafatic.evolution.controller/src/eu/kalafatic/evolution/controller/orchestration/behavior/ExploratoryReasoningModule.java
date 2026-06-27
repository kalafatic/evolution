package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.stream.Collectors;

public class ExploratoryReasoningModule implements InstructionModule {
    @Override
    public String getInstructions(ExecutionPolicy policy) {
        if (policy.getReasoningStrategy() != ExecutionPolicy.ReasoningStrategy.EXPLORATORY) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("REASONING: EXPLORATORY (Exploration Level: ").append(policy.getExplorationLevel()).append(")\n")
          .append("→ You are encouraged to explore novel solutions and architectural improvements.\n")
          .append("→ Focus on discovery and expanding system capabilities.\n")
          .append("→ Be proactive in identifying areas for optimization.");

        if (!policy.getConstraints().isEmpty()) {
            sb.append("\nCONSTRAINTS:\n")
              .append(policy.getConstraints().stream().map(c -> "→ " + c).collect(Collectors.joining("\n")));
        }

        return sb.toString();
    }
}
