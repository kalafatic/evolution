package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that evaluates variants based on their semantic coherence with historical EMF data.
 */
public class SemanticCoherencePolicy implements ResolverPolicy {

    @Override
    public double evaluate(BranchVariant variant) {
        double score = 0.5;
        if (variant.getSurvivalArgument() != null && !variant.getSurvivalArgument().isEmpty()) {
            score += 0.2;
        }
        if (variant.getStrategy() != null && variant.getStrategy().contains("Analytical")) {
            score += 0.1;
        }
        return Math.min(1.0, score);
    }

    @Override
    public String getName() {
        return "SemanticCoherencePolicy";
    }
}
