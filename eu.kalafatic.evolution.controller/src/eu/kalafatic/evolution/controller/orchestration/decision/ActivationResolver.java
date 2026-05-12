package eu.kalafatic.evolution.controller.orchestration.decision;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.workspace.SemanticWorkspace;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
import eu.kalafatic.evolution.controller.orchestration.scheduling.ScheduledExecutionPlan;

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
        return resolve(iterationId, signals, recommendations, policies, null);
    }

    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals,
                                    List<ActivationRecommendation> recommendations,
                                    List<ResolverPolicy> policies,
                                    SemanticWorkspace workspace) {
        return resolve(iterationId, signals, recommendations, policies, workspace, null);
    }

    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals,
                                    List<ActivationRecommendation> recommendations,
                                    List<ResolverPolicy> policies,
                                    SemanticWorkspace workspace,
                                    ScheduledExecutionPlan executionPlan) {

        DecisionSnapshot finalDecision = null;

        // Filtering based on ScheduledExecutionPlan
        if (executionPlan != null) {
            signals = signals.stream()
                .filter(s -> executionPlan.isApproved(s.getVariantId()))
                .collect(Collectors.toList());

            recommendations = recommendations.stream()
                .filter(r -> executionPlan.isApproved(r.getBranchId()))
                .collect(Collectors.toList());
        }

        // Reinforce workspace artifacts if they were used in selection (placeholder for logic)
        if (workspace != null) {
            // Future: Logic to reinforce artifacts based on signals
        }

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
