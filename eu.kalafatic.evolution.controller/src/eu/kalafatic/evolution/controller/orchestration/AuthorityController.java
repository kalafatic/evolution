package eu.kalafatic.evolution.controller.orchestration;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import eu.kalafatic.evolution.controller.orchestration.decision.DecisionResolver;
import eu.kalafatic.evolution.controller.orchestration.decision.DecisionSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

/**
 * The AuthorityController is the SINGLE SOLE authority for execution decisions
 * in the Evolutionary OS. It manages the proposal lifecycle and resolves
 * branch activations, winner selections, and supervision routing.
 *
 * <p><b>ARCHITECTURAL INVARIANT: CENTRAL AUTHORITY</b></p>
 * No other component (DarwinFlow, IterativeFlow, Agents) is permitted to
 * make execution-affecting decisions. They must all request a decision
 * from the AuthorityController.
 */
public class AuthorityController {

    public enum ProposalState {
        CREATED,
        ANALYZING,
        SCORING,
        RECOMMENDED,
        APPROVED,
        ACTIVE,
        EXECUTING,
        VERIFIED,
        REJECTED,
        ARCHIVED
    }

    private final DecisionResolver decisionResolver = new DecisionResolver();

    /**
     * Evaluates proposals and makes an authoritative decision on how to proceed.
     */
    public AuthorityDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context) throws Exception {
        BehaviorProfile profile = context.getBehaviorProfile();
        OrchestrationState state = context.getOrchestrationState();

        // 1. Handle MEDIATED mode (Manual Supervision)
        if (profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED)) {
            context.log("[AUTHORITY] Supervision Mode: MEDIATED. Requesting user selection.");
            String input = context.requestInput("Darwin generated " + variants.size() + " proposals. Review and select one to proceed (e.g. 'Select v0'), or reject to refine.").get();

            if ("Rejected".equalsIgnoreCase(input)) {
                return AuthorityDecision.reject("User rejected proposals in MEDIATED mode.");
            }

            String manualId = null;
            if (input.startsWith("Select ")) {
                manualId = input.substring(7).trim();
            }

            DecisionSnapshot snapshot = decisionResolver.resolveWinner(iterationId, variants, context, manualId);
            return AuthorityDecision.activate(snapshot.getSelectedVariantId(), snapshot);
        }

        // 2. Handle Auto-Approval logic
        if (!context.isAutoApprove()) {
            context.log("[AUTHORITY] Auto-Approve: OFF. Requesting user approval for exploration.");
            String input = context.requestInput("Darwin generated " + variants.size() + " variants. Review and approve to start evaluation.").get();

            if ("Rejected".equalsIgnoreCase(input)) {
                return AuthorityDecision.reject("User rejected exploration plan.");
            }
        }

        // 3. Autonomous Decision (using Signal-based Resolver)
        context.log("[AUTHORITY] Supervision Mode: AUTONOMOUS. Resolving winner via policies.");
        DecisionSnapshot snapshot = decisionResolver.resolveWinner(iterationId, variants, context);

        if (snapshot.getSelectedVariantId() == null) {
            return AuthorityDecision.reject("No high-quality variant identified for activation.");
        }

        return AuthorityDecision.activate(snapshot.getSelectedVariantId(), snapshot);
    }

    /**
     * DTO representing an authoritative decision.
     */
    public static class AuthorityDecision {
        private final boolean approved;
        private final String selectedVariantId;
        private final String reason;
        private final DecisionSnapshot snapshot;

        private AuthorityDecision(boolean approved, String selectedVariantId, String reason, DecisionSnapshot snapshot) {
            this.approved = approved;
            this.selectedVariantId = selectedVariantId;
            this.reason = reason;
            this.snapshot = snapshot;
        }

        public static AuthorityDecision activate(String variantId, DecisionSnapshot snapshot) {
            return new AuthorityDecision(true, variantId, "Decision Authority: ACTIVATE", snapshot);
        }

        public static AuthorityDecision reject(String reason) {
            return new AuthorityDecision(false, null, reason, null);
        }

        public boolean isApproved() { return approved; }
        public String getSelectedVariantId() { return selectedVariantId; }
        public String getReason() { return reason; }
        public DecisionSnapshot getSnapshot() { return snapshot; }
    }
}
