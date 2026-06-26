package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.EvolutionStage;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionNode;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class DefaultWinnerService implements WinnerService {

    @Override
    public String handleVariantSelection(TaskContext context, List<BranchVariant> variants, String goal, IterationManager manager) throws Exception {
        return manager.getSelectionEngine().handleManualSelection(context, variants, goal, manager);
    }

    @Override
    public EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId, IterationManager manager) {
        EvolutionProgressPublisher.updateStage(context, EvolutionStage.SELECT_WINNER);
        EvolutionDecision decision = manager.getAuthorityEngine().decide(iterationId, variants, context, manualSelectionId);
        applyDecision(decision, variants, context, manager);

        // UI SYNC: Emit centralized [DARWIN_BRANCHES] message for variant status updates
        manager.getSessionContainer().getEventBus().publish(new RuntimeEvent(RuntimeEventType.DECISION_UPDATED, context.getSessionId(), manualSelectionId, iterationId, "Kernel", decision.getSelectedVariantId(), System.currentTimeMillis()));

        JSONObject json = new JSONObject();
        int iteration = context.getOrchestrationState().getIterationCount() + 1; // Sync to 1-based for UI
        json.put("iteration", iteration);
        JSONArray variantsArr = new JSONArray();

        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();

        for (BranchVariant v : variants) {
            JSONObject vObj = new JSONObject();
            vObj.put("id", v.getId());
            vObj.put("strategy", v.getStrategy());
            vObj.put("score", v.getScore());
            vObj.put("survival_argument", v.getSurvivalArgument());
            vObj.put("tradeoffs", v.getTradeoffs());
            vObj.put("status", v.getActivationState().name());

            EvolutionNode node = tree.getNode(v.getId());
            if (node != null) {
                vObj.put("mutation_identity", node.getMutationIdentity());
                vObj.put("parent_id", node.getParentId());
            }
            variantsArr.put(vObj);
        }
        json.put("variants", variantsArr);

        StringBuilder outcomeBuilder = new StringBuilder("[DARWIN_BRANCHES] ");
        outcomeBuilder.append("\nIteration ").append(iteration).append("\n");
        String winnerId = decision.getSelectedVariantId();
        outcomeBuilder.append("[APPROVED:").append(winnerId).append("] ");

        for (BranchVariant v : variants) {
            EvolutionNode node = tree.getNode(v.getId());
            String identity = (node != null && node.getMutationIdentity() != null) ? node.getMutationIdentity() : v.getId();

            if (v.getId().equals(winnerId)) {
                outcomeBuilder.append("\n  ├── ").append(identity).append(" (Winner) Strategy: ").append(v.getStrategy()).append("\n");
                continue;
            }

            // Survival Rule: If not winner and not manually kept, it is explicitly REJECTED
            String status = (v.getActivationState() == BranchVariant.ActivationState.KEPT) ? "KEPT" : "REJECTED";

            // Force state update for consistent UI stamping
            if (!"KEPT".equals(status)) {
                v.setActivationState(BranchVariant.ActivationState.REJECTED);
            }

            outcomeBuilder.append("  ├── ").append(identity).append(" (").append(status).append(") Strategy: ").append(v.getStrategy()).append("\n");
            outcomeBuilder.append("[").append(status).append(":").append(v.getId()).append("] ");
        }

        // Ensure decision type and variant metadata are logged for visual stamping
        String decisionType = (manualSelectionId != null) ? "MANUAL" : "AUTO";
        outcomeBuilder.append("[DECISION:").append(decisionType).append("] ");
        outcomeBuilder.append(json.toString());

        // STAMPING MANDATE: Always emit branch statuses for the UI to render stamps correctly
        context.log(outcomeBuilder.toString());

        return decision;
    }

    private void applyDecision(EvolutionDecision decision, List<BranchVariant> variants, TaskContext context, IterationManager manager) {
        String winnerId = decision.getSelectedVariantId();

        for (BranchVariant variant : variants) {
            if (variant.getId().equals(winnerId)) {
                manager.updateVariantLifecycle(variants, variant.getId(), BranchVariant.ActivationState.ACTIVE, context);
                variant.setRank("winner");
            } else {
                BranchVariant.ActivationState newState = (variant.getActivationState() == BranchVariant.ActivationState.KEPT) ? BranchVariant.ActivationState.KEPT : BranchVariant.ActivationState.REJECTED;
                manager.updateVariantLifecycle(variants, variant.getId(), newState, context);
                variant.setRank(decision.getRejectedVariantIds().contains(variant.getId()) ? "runner-up" : "noise");
            }
        }
    }

    @Override
    public eu.kalafatic.evolution.model.orchestration.EvaluationResult processWinners(TaskContext context, EvolutionDecision decision, List<BranchVariant> variants, eu.kalafatic.evolution.controller.orchestration.goal.GoalModel goal, IterationManager manager) throws Exception {
        return manager.getDarwinEngine().executeWinner(context, decision, variants, goal, manager);
    }
}
