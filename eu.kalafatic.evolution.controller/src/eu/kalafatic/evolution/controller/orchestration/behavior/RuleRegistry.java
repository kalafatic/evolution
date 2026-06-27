package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized registry for semantic orchestration rules.
 */
public class RuleRegistry {
    private static final List<PolicyRule> rules = new ArrayList<>();

    static {
        // 1. Mediated + Darwin => Hybrid Supervision + Step Preferred
        rules.add(policy -> {
            if (policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.MEDIATED &&
                policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.DARWIN) {
                policy.setSupervisionLevel(ExecutionPolicy.SupervisionLevel.HYBRID);
                policy.setInteractionMode(ExecutionPolicy.InteractionMode.STEP);
                policy.addConstraint("MEDIATED_DARWIN_GUARD: Human must review all mutation variants.");
            }
        });

        // 2. Local Mode => Isolated Repository
        rules.add(policy -> {
            if (policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.LOCAL) {
                policy.setRepositoryMode(ExecutionPolicy.RepositoryMode.ISOLATED);
            }
        });

        // 3. Exploratory Reasoning => High Exploration Level
        rules.add(policy -> {
            if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.EXPLORATORY) {
                policy.setExplorationLevel(0.9);
            }
        });

        // 4. Conservative Reasoning => Low Exploration Level + Continuous Step (Safety)
        rules.add(policy -> {
            if (policy.getReasoningStrategy() == ExecutionPolicy.ReasoningStrategy.CONSERVATIVE) {
                policy.setExplorationLevel(0.2);
                policy.setInteractionMode(ExecutionPolicy.InteractionMode.STEP);
                policy.addConstraint("CONSERVATIVE_SAFETY: Minimal changes enforced.");
            }
        });

        // 5. Remote Mode => Virtual Repository + Auto Supervision
        rules.add(policy -> {
            if (policy.getExecutionMode() == ExecutionPolicy.ExecutionMode.REMOTE) {
                policy.setRepositoryMode(ExecutionPolicy.RepositoryMode.VIRTUAL);
                policy.setSupervisionLevel(ExecutionPolicy.SupervisionLevel.AUTO);
            }
        });
    }

    public static List<PolicyRule> getRules() {
        return rules;
    }
}
