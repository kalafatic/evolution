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
    public PolicyResult evaluate(BranchVariant variant) {
        double score = variant.getScore() >= threshold ? 1.0 : 0.0;
        return new PolicyResult(score, 1.0, "Comparison against threshold: " + threshold);
    }

    @Override
    public String getName() {
        return "ConfidenceThresholdPolicy";
    }
}
