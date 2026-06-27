package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Interface for deterministic decision policies used by the ActivationResolver.
 */
public interface ResolverPolicy {

    /**
     * Evaluates a single variant and returns a structured PolicyResult.
     */
    PolicyResult evaluate(BranchVariant variant);

    /**
     * @return the unique name of the policy
     */
    String getName();
}
