package eu.kalafatic.evolution.controller.supervision;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * AuthorityController is the single authority for execution decisions.
 * It manages the lifecycle of BranchVariants and produces AuthorityDecisions.
 */
@EvolutionComponent(
    domain = "supervision",
    role = "authority-controller",
    purpose = "Central authority for branch lifecycle and execution decisions",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.CRITICAL
)
public class AuthorityController {

    private final DecisionResolver decisionResolver;

    public AuthorityController() {
        this.decisionResolver = new DecisionResolver();
    }

    public enum DecisionType {
        ACTIVATE,
        REJECT,
        REQUEST_CLARIFICATION,
        KEEP_AS_RUNNER_UP,
        EXECUTE,
        STOP
    }

    public static class AuthorityDecision {
        private final DecisionType type;
        private final String variantId;
        private final String reason;

        public AuthorityDecision(DecisionType type, String variantId, String reason) {
            this.type = type;
            this.variantId = variantId;
            this.reason = reason;
        }

        public DecisionType getType() { return type; }
        public String getVariantId() { return variantId; }
        public String getReason() { return reason; }
    }

    /**
     * Decisions on which variant to activate and move to the next stage.
     */
    public EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId) {
        DecisionSnapshot decision = decisionResolver.resolveWinner(iterationId, variants, context, manualSelectionId);

        String winnerId = decision.getSelectedVariantId();

        DecisionType type;
        if (winnerId != null && !winnerId.equals("NONE")) {
            type = DecisionType.ACTIVATE;
        } else {
            type = DecisionType.REJECT;
        }

        List<String> rejectedIds = decision.getRankedVariants().stream()
                .filter(id -> !id.equals(winnerId))
                .collect(java.util.stream.Collectors.toList());

        EvolutionDecision evolutionDecision = new EvolutionDecision(
            type,
            winnerId,
            rejectedIds,
            decision.getActivationReason(),
            decision.getAggregatedScores(),
            java.util.Collections.emptyMap() // Metadata can be expanded
        );

        // AUDIT TRAIL
        if (context != null) {
            IterationManager manager = KernelFactory.create(context);
            if (manager.getMemoryService() != null) {
                AuditRecord audit = new AuditRecord(
                    iterationId,
                    winnerId,
                    evolutionDecision.getType().name(),
                    "MULTIPLE_CANDIDATES",
                    (winnerId != null ? "APPROVED" : "REJECTED"),
                    "AuthorityController",
                    decision.getActivationReason(),
                    context.getSessionId()
                );
                manager.getMemoryService().appendAuditRecord(audit);
            }
        }

        return evolutionDecision;
    }


    /**
     * Updates the lifecycle state of variants.
     */
    public void updateLifecycle(List<BranchVariant> variants, String targetId, BranchVariant.ActivationState newState, TaskContext context) {
        for (BranchVariant v : variants) {
            if (v.getId().equals(targetId)) {
                BranchVariant.ActivationState oldState = v.getActivationState();
                v.setActivationState(newState);
                if (context != null) {
                    context.log("[AUTHORITY] Lifecycle transition: " + v.getId() + " -> " + newState);
                    auditTransition(context, v, oldState, newState, "Flow-driven transition");
                }
            }
        }
    }

    private void auditTransition(TaskContext context, BranchVariant variant, BranchVariant.ActivationState oldState, BranchVariant.ActivationState newState, String reason) {
        IterationManager manager = KernelFactory.create(context);
        if (manager.getMemoryService() != null) {
            AuditRecord audit = new AuditRecord(
                context.getOrchestrationState().getCurrentIterationId(),
                variant.getId(),
                "LIFECYCLE_TRANSITION",
                oldState.name(),
                newState.name(),
                "AuthorityController",
                reason,
                context.getSessionId()
            );
            manager.getMemoryService().appendAuditRecord(audit);
        }
    }

    /**
     * Transitions all variants to a specific state.
     */
    public void transitionAll(List<BranchVariant> variants, BranchVariant.ActivationState newState) {
        for (BranchVariant v : variants) {
            v.setActivationState(newState);
        }
    }
}
