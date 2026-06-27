package eu.kalafatic.evolution.controller.supervision;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
            // Reduced penalty: 0.02 instead of 0.05 to avoid suppressing progress
            double penalty = Math.min(0.2, steps * 0.02);
            score -= penalty;
            trace.append("Projected steps penalty (-").append(String.format("%.2f", penalty)).append("). ");
        }
        if (variant.getTradeoffs() != null && !variant.getTradeoffs().isEmpty()) {
            score += 0.1;
            trace.append("Explicit tradeoffs rewarded (+0.1). ");
        }
        
        // Boost for variants that actually provide an implementation strategy
        if ("IMPLEMENTATION".equals(variant.getStrategyType())) {
            score += 0.05;
            trace.append("Implementation strategy bonus (+0.05). ");
        }

        // Fitness Horizon Axes (side-effects for metadata tracking)
        variant.setShortTermFitness(score);
        variant.setLongTermStability(1.0 - (steps * 0.05)); // Reduced stability penalty
        
        return new PolicyResult(score, 0.9, trace.toString().trim());
    }

    @Override
    public String getName() {
        return "ComplexityCostPolicy";
    }
}
