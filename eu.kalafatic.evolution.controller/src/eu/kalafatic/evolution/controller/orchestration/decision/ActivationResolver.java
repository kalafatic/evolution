package eu.kalafatic.evolution.controller.orchestration.decision;

import java.util.List;
import java.util.ArrayList;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;

/**
 * Central deterministic decision authority for Darwin branch activation.
 * It consumes signals and recommendations to produce a DecisionSnapshot.
 *
 * IT MUST NOT:
 * - execute tasks
 * - mutate git state
 * - call LLMs
 * - generate variants
 */
public class ActivationResolver {

    /**
     * Resolves the activation decision using a list of policies.
     * Policies are evaluated in order; the first one that produces a selected variant wins.
     *
     * @param iterationId current iteration ID
     * @param signals all collected signals
     * @param recommendations all recommendations from the ActivationGate
     * @param policies ordered list of policies to evaluate
     * @return the resulting DecisionSnapshot
     */
    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals,
                                    List<ActivationRecommendation> recommendations,
                                    List<ResolverPolicy> policies) {

        DecisionSnapshot finalDecision = null;

        for (ResolverPolicy policy : policies) {
            DecisionSnapshot decision = policy.resolve(iterationId, signals, recommendations);
            if (decision.getSelectedVariantId() != null) {
                return decision;
            }
            // Keep the last decision if none select a variant
            finalDecision = decision;
        }

        // Default if no policies or no selection
        if (finalDecision == null) {
            finalDecision = new DecisionSnapshot(
                iterationId, null, new ArrayList<>(), null, new ArrayList<>(),
                "No variant selected by any policy.", "None", 0.0, "Empty policy list or no match."
            );
        }

        return finalDecision;
    }
}
