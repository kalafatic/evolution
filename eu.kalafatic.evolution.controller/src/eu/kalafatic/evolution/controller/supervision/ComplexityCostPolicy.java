package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that evaluates variants based on their estimated complexity cost and stability.
 */
public class ComplexityCostPolicy implements ResolverPolicy {

    @Override
    public double evaluate(BranchVariant variant) {
        double score = 0.5;
        int steps = variant.getProjectedSteps().size();
        if (steps > 0) {
            score -= (steps * 0.05);
        }
        if (variant.getTradeoffs() != null && !variant.getTradeoffs().isEmpty()) {
            score += 0.1;
        }

        // Fitness Horizon Axes (side-effects for metadata tracking)
        variant.setShortTermFitness(score);
        variant.setLongTermStability(1.0 - (steps * 0.1));

        return Math.max(0.0, score);
    }

    @Override
    public String getName() {
        return "ComplexityCostPolicy";
    }
}
