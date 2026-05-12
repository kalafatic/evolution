package eu.kalafatic.evolution.controller.orchestration.decision;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;

/**
 * Interface for deterministic decision policies used by the ActivationResolver.
 */
public interface ResolverPolicy {
    /**
     * Resolves an activation decision based on signals and recommendations.
     *
     * @param iterationId current iteration ID
     * @param signals all collected signals for the variants
     * @param recommendations all recommendations from the ActivationGate
     * @return a DecisionSnapshot representing the deterministic decision
     */
    DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals, List<ActivationRecommendation> recommendations);

    /**
     * @return the unique name of the policy
     */
    String getName();
}
