package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that evaluates variants based on their estimated complexity cost and stability.
 */
public class ComplexityCostPolicy implements ResolverPolicy {

    @Override
    public PolicyResult evaluate(BranchVariant variant) {
        double score = 0.5;
        StringBuilder trace = new StringBuilder("Base complexity score: 0.5. ");
        int steps = variant.getProjectedSteps().size();
        if (steps > 0) {
            score -= (steps * 0.05);
            trace.append("Projected steps penalty (-").append(steps * 0.05).append("). ");
        }
        if (variant.getTradeoffs() != null && !variant.getTradeoffs().isEmpty()) {
            score += 0.1;
            trace.append("Explicit tradeoffs rewarded (+0.1). ");
        }
        
        // Fitness Horizon Axes (side-effects for metadata tracking)
        variant.setShortTermFitness(score);
        variant.setLongTermStability(1.0 - (steps * 0.1));
        
        return new PolicyResult(score, 0.9, trace.toString().trim());
    }

    @Override
    public String getName() {
        return "ComplexityCostPolicy";
    }
}
