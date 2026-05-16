package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that selects based on confidence thresholds.
 */
public class ConfidenceThresholdPolicy implements ResolverPolicy {
    private final double threshold;

    public ConfidenceThresholdPolicy() {
        this(0.6);
    }

    public ConfidenceThresholdPolicy(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public double evaluate(BranchVariant variant) {
        return variant.getScore() >= threshold ? 1.0 : 0.0;
    }

    @Override
    public String getName() {
        return "ConfidenceThresholdPolicy";
    }
}
