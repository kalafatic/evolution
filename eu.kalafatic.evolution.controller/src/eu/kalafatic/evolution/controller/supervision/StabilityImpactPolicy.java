package eu.kalafatic.evolution.controller.supervision;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that evaluates variants based on their impact on architectural stability.
 */
public class StabilityImpactPolicy implements ResolverPolicy {

    @Override
    public PolicyResult evaluate(BranchVariant variant) {
        double score = 0.5;
        StringBuilder trace = new StringBuilder("Base stability score: 0.5. ");
        if (variant.getFailureRisks() != null) {
            if (variant.getFailureRisks().toLowerCase().contains("high")) {
                score -= 0.3;
                trace.append("High failure risk detected (-0.3). ");
            } else if (variant.getFailureRisks().toLowerCase().contains("low")) {
                score += 0.2;
                trace.append("Low failure risk detected (+0.2). ");
            }
        }
        return new PolicyResult(score, 0.7, trace.toString().trim());
    }

    @Override
    public String getName() {
        return "StabilityImpactPolicy";
    }
}
