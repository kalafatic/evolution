package eu.kalafatic.evolution.controller.orchestration.decision;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.workspace.SemanticWorkspace;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CognitiveTrace;
import eu.kalafatic.evolution.controller.orchestration.scheduling.ScheduledExecutionPlan;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IResolverContract;
import java.util.Collections;

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
public class ActivationResolver implements ICapability, IResolverContract {

    private CapabilityStatus status = CapabilityStatus.STOPPED;

    @Override
    public String getCapabilityId() {
        return "capability.resolver";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CapabilityStatus getStatus() {
        return status;
    }

    @Override
    public void initialize(CapabilityContext context) throws CapabilityException {
        status = CapabilityStatus.INITIALIZED;
    }

    @Override
    public void start() throws CapabilityException {
        status = CapabilityStatus.STARTED;
    }

    @Override
    public void stop() throws CapabilityException {
        status = CapabilityStatus.STOPPED;
    }

    @Override
    public List<String> getSupportedContracts() {
        return Collections.singletonList(IResolverContract.ID);
    }

    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public CapabilityHealth getHealth() {
        return new CapabilityHealth(1.0, "Healthy", 0);
    }

    @Override
    public void resolve(DecisionSnapshot snapshot) {
        // Implementation for IResolverContract if needed,
        // but existing resolve methods are more specific.
        // For now, this is a placeholder to satisfy the contract.
    }

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
        return resolve(iterationId, signals, recommendations, policies, workspace, executionPlan, null);
    }

    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals,
                                    List<ActivationRecommendation> recommendations,
                                    List<ResolverPolicy> policies,
                                    SemanticWorkspace workspace,
                                    ScheduledExecutionPlan executionPlan,
                                    CognitiveTrace trace) {

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

            // DIAGNOSTICS: Record resolver decision in trace
            if (trace != null) {
                trace.addNode(new CausalNode(
                    "resolver-policy-" + policy.getClass().getSimpleName() + "-" + System.currentTimeMillis(),
                    "RESOLVER_POLICY",
                    "ActivationResolver",
                    signals.stream().map(s -> s.getVariantId()).distinct().collect(Collectors.toList()),
                    decision.getSelectedVariantId() != null ? List.of(decision.getSelectedVariantId()) : List.of(),
                    decision.getResolverConfidence(),
                    decision.getActivationReason()
                ));
            }

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
