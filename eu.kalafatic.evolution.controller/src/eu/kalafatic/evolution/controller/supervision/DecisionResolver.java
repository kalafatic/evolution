package eu.kalafatic.evolution.controller.supervision;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationGate;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Centralized decision authority for orchestration.
 * This component is the ONLY ONE allowed to activate variants and select winners.
 *
 * <p><b>ARCHITECTURAL INVARIANT: DECISION AUTHORITY</b></p>
 * DecisionResolver is the sole authority for variant activation and winner selection.
 * It consumes signals from the SignalBus and applies deterministic policies
 * (HighestScore, TrajectoryStability, etc.) to resolve the system's next state.
 * No other component is permitted to modify variant activation states.
 */
@EvolutionComponent(
    domain = "supervision",
    role = "decision-authority",
    purpose = "Sole authority for variant activation and winner selection",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.CRITICAL
)
public class DecisionResolver {

    private final ActivationResolver activationResolver = new ActivationResolver();

    /**
     * Resolves the winner variant from the provided candidates and signals in the SignalBus.
     */
    public DecisionSnapshot resolveWinner(String iterationId, List<BranchVariant> variants, TaskContext context) {
        return resolveWinner(iterationId, variants, context, null);
    }

    /**
     * Resolves the winner variant, optionally with an explicit manual selection.
     */
    public DecisionSnapshot resolveWinner(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId) {
        List<EvaluationSignal> signals = SignalBus.getInstance().getAllSignals();

        ActivationGate gate = new ActivationGate();
        List<ActivationRecommendation> recommendations = gate.recommendActivations(variants);

        List<ResolverPolicy> policies = new ArrayList<>();

        // 1. Respect manual selection if present
        if (manualSelectionId != null) {
            policies.add(new ManualSelectionPolicy(manualSelectionId));
        }

        // 2. Trajectory stability
        policies.add(new TrajectoryStabilityPolicy(context.getSemanticWorkspace()));

        // 3. Highest score
        policies.add(new HighestScorePolicy());

        // 4. Semantic Darwin policies
        policies.add(new SemanticCoherencePolicy(variants));
        policies.add(new ComplexityCostPolicy(variants));
        policies.add(new StabilityImpactPolicy(variants));

        ScheduledExecutionPlan executionPlan = (ScheduledExecutionPlan) context.getOrchestrationState().getMetadata().get("executionPlan");

        DecisionSnapshot decision = activationResolver.resolve(
            iterationId,
            signals,
            recommendations,
            policies,
            context.getSemanticWorkspace(),
            executionPlan,
            context.getOrchestrationState().getCognitiveTrace()
        );

        // APPLY THE DECISION (AUTHORITY)
        applyDecision(decision, variants, context);

        // PERSISTENCE: Save the decision snapshot to metadata so DarwinFlow can react (e.g. exploration trigger)
        context.getOrchestrationState().getMetadata().put("lastDecisionSnapshot", decision);

        return decision;
    }

    /**
     * Strictly applies the decision to the variants.
     */
    private void applyDecision(DecisionSnapshot decision, List<BranchVariant> variants, TaskContext context) {
        String winnerId = decision.getSelectedVariantId();

        for (BranchVariant variant : variants) {
            if (variant.getId().equals(winnerId)) {
                variant.setActivationState(BranchVariant.ActivationState.ACTIVE);
                variant.setRank("winner");
                context.log("[AUTHORITY] Activated winner variant: " + winnerId + " (" + variant.getStrategy() + ")");

                // Store survival metadata in EMF if available
                if (context.getOrchestrator().getSelfDevSession() != null) {
                    eu.kalafatic.evolution.model.orchestration.Iteration it = context.getOrchestrator().getSelfDevSession().getIterations().stream()
                        .filter(i -> i.getId().equals(decision.getIterationId())).findFirst().orElse(null);
                    if (it != null) {
                        it.setSurvivalArgument(variant.getSurvivalArgument());
                        it.setTradeoffs(variant.getTradeoffs());
                        it.setFailureRisks(variant.getFailureRisks());
                    }
                }
            } else if (decision.getRankedVariants().contains(variant.getId())) {
                variant.setActivationState(BranchVariant.ActivationState.INACTIVE);
                variant.setRank("runner-up");
            } else {
                variant.setActivationState(BranchVariant.ActivationState.ARCHIVED);
                variant.setRank("noise");
            }
        }
    }
}
