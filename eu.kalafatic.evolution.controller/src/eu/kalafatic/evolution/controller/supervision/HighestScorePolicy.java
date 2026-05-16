package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that selects the variant with the highest aggregated score.
 * Integrated into ActivationResolver via signalBoost and weighted averaging.
 */
public class HighestScorePolicy implements ResolverPolicy {

    @Override
    public double evaluate(BranchVariant variant) {
        // The core score is already present on the variant from evaluateVariantParallel
        return variant.getScore();
    }

    @Override
    public String getName() {
        return "HighestScorePolicy";
    }
}
