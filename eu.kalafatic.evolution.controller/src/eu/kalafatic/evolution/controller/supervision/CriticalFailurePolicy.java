package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that rejects variants with critical failures.
 */
public class CriticalFailurePolicy implements ResolverPolicy {

    @Override
    public double evaluate(BranchVariant variant) {
        if (!variant.isSuccess() || (variant.getErrorMessage() != null && !variant.getErrorMessage().isEmpty())) {
            return 0.0;
        }
        return 1.0;
    }

    @Override
    public String getName() {
        return "CriticalFailurePolicy";
    }
}
