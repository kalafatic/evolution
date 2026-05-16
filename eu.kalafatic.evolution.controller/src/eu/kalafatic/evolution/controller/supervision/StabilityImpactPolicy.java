package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that evaluates variants based on their impact on architectural stability.
 */
public class StabilityImpactPolicy implements ResolverPolicy {

    @Override
    public double evaluate(BranchVariant variant) {
        double score = 0.5;
        if (variant.getFailureRisks() != null) {
            if (variant.getFailureRisks().contains("high")) {
                score -= 0.3;
            } else if (variant.getFailureRisks().contains("low")) {
                score += 0.2;
            }
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    @Override
    public String getName() {
        return "StabilityImpactPolicy";
    }
}
