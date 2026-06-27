package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Policy that rejects variants with critical failures.
 */
public class CriticalFailurePolicy implements ResolverPolicy {

    @Override
    public PolicyResult evaluate(BranchVariant variant) {
        if (!variant.isSuccess() || (variant.getErrorMessage() != null && !variant.getErrorMessage().isEmpty())) {
            return new PolicyResult(0.0, 1.0, "Critical failure or error message detected: " + variant.getErrorMessage());
        }
        return new PolicyResult(1.0, 1.0, "No critical failures detected.");
    }

    @Override
    public String getName() {
        return "CriticalFailurePolicy";
    }
}
