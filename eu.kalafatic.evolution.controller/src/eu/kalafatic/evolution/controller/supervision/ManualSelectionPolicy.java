package eu.kalafatic.evolution.controller.supervision;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that respects explicit user selection.
 */
public class ManualSelectionPolicy implements ResolverPolicy {
    private final String manualSelectionId;

    public ManualSelectionPolicy(String manualSelectionId) {
        this.manualSelectionId = manualSelectionId;
    }

    @Override
    public PolicyResult evaluate(BranchVariant variant) {
        double score = variant.getId().equals(manualSelectionId) ? 1.0 : 0.0;
        return new PolicyResult(score, 1.0, "Manual selection override for ID: " + manualSelectionId);
    }

    @Override
    public String getName() {
        return "ManualSelectionPolicy";
    }
}
