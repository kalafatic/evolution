package eu.kalafatic.evolution.controller.supervision;

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
    public double evaluate(BranchVariant variant) {
        return variant.getId().equals(manualSelectionId) ? 1.0 : 0.0;
    }

    @Override
    public String getName() {
        return "ManualSelectionPolicy";
    }
}
