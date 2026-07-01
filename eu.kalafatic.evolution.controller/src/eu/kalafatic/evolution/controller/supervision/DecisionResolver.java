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
 */
@EvolutionComponent(
    domain = "supervision",
    role = "decision-authority",
    purpose = "Sole authority for variant activation and winner selection",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.CRITICAL
)
public class DecisionResolver {

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
        if (context == null) {
            throw new IllegalStateException("DecisionResolver: context is null. Cannot resolve winner.");
        }
        List<EvaluationSignal> signals = context.getKernelContext().getSignalBus().getAllSignals();

        // Authority over selection engine
        ActivationResolver activationResolver = new ActivationResolver(context.getSemanticWorkspace().getTrajectoryMemory());
        
        // If manual selection is provided, we bypass standard resolution for the winner
        if (manualSelectionId != null) {
            String resolvedId = resolveFuzzyId(manualSelectionId, variants, context);
            String logMsg = context.getExecutionProfile().requireUserSelection() ?
                "[AUTHORITY] Manual selection override: " + manualSelectionId :
                "[AUTHORITY] Trajectory automatically selected: " + manualSelectionId;

            context.log(logMsg + (resolvedId.equals(manualSelectionId) ? "" : " (Resolved to: " + resolvedId + ")"));
            return createManualDecision(iterationId, resolvedId, variants, signals);
        }

        DecisionSnapshot decision = activationResolver.resolve(
            iterationId,
            variants,
            signals,
            context
        );

        // PERSISTENCE: Save the decision snapshot to metadata so DarwinFlow can react (e.g. exploration trigger)
        context.getOrchestrationState().getMetadata().put("lastDecisionSnapshot", decision);

        return decision;
    }

    private String resolveFuzzyId(String manualId, List<BranchVariant> variants, TaskContext context) {
        if (manualId == null) return null;

        // 1. Exact match
        for (BranchVariant v : variants) {
            if (v.getId().equalsIgnoreCase(manualId)) return v.getId();
        }

        String lowerManualId = manualId.toLowerCase();

        // 2. Strategy type match (e.g. "PROBABLE_SURVIVOR")
        for (BranchVariant v : variants) {
            if (v.getStrategyType() != null && !v.getStrategyType().isEmpty() &&
                lowerManualId.contains(v.getStrategyType().toLowerCase())) {
                return v.getId();
            }
        }

        // 3. Proposal number match (e.g. "1", "variant 1", "0.1", "1.1")
        String numeric = lowerManualId.replaceAll("[^0-9.]", " ").trim();
        if (!numeric.isEmpty()) {
            String[] parts = numeric.split("\\s+");
            for (String part : parts) {
                int index = -1;
                if (part.contains(".")) {
                    String[] subparts = part.split("\\.");
                    try {
                        index = Integer.parseInt(subparts[subparts.length - 1]) - 1;
                    } catch (NumberFormatException e) {}
                } else {
                    try {
                        index = Integer.parseInt(part) - 1;
                    } catch (NumberFormatException e) {}
                }

                if (index >= 0 && index < variants.size()) {
                    return variants.get(index).getId();
                }
            }
        }

        return manualId;
    }

    private DecisionSnapshot createManualDecision(String iterationId, String manualId, List<BranchVariant> variants, List<EvaluationSignal> signals) {
        List<String> ranked = new ArrayList<>();
        // Prioritize the selected variant in the ranking
        ranked.add(manualId);
        for (BranchVariant v : variants) {
            if (!v.getId().equals(manualId)) {
                ranked.add(v.getId());
            }
        }

        return new DecisionSnapshot(
            iterationId,
            manualId,
            ranked,
            new java.util.HashMap<>(),
            new ArrayList<>(),
            "Manual user selection override",
            "ManualSelectionPolicy",
            1.0,
            "User explicitly selected variant " + manualId,
            0.0
        );
    }

}
