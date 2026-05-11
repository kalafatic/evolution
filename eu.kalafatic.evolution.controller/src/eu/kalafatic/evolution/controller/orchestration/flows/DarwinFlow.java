package eu.kalafatic.evolution.controller.orchestration.flows;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.evolution.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

/**
 * Evolutionary Darwin loop orchestration flow.
 */
public class DarwinFlow implements IOrchestrationFlow {
    private final AiService aiService;
    private final IterationManager manager;

    public DarwinFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        context.log("[KERNEL] Executing Darwin Flow.");
        runDarwin(context);
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        response.setSummary("Darwin evolution phase completed: " + context.getOrchestrationState().getCurrentPhase());
        return response;
    }

    public EvaluationResult runDarwin(TaskContext context) throws Exception {
        manager.transition(SystemState.INIT, context);
        String goal = context.getOrchestrator().getSelfDevSession() != null ? context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";

        OrchestrationState state = context.getOrchestrationState();
        if (state.getCurrentPhase() == null) {
            state.setCurrentPhase(EvolutionConstants.PHASE_INTENT_EXPANSION);
        }

        Iteration currentIterationModel = manager.getCurrentIterationModel();
        if (currentIterationModel != null) {
            currentIterationModel.setPhase(state.getCurrentPhase());
        }

        context.log("[KERNEL] Darwin Evolution Phase: " + state.getCurrentPhase());

        if (manager.getGitManager().isGitRepository()) {
            manager.getGitManager().ensureInitialCommit();
        }
        String originalBranch = manager.getGitManager().getCurrentBranch();
        String iterId = currentIterationModel != null ? currentIterationModel.getId() : "default";
        String snapshotBranch = "snapshot/" + iterId + "-" + System.currentTimeMillis();

        try {
            manager.getGitManager().createBranch(snapshotBranch);
            manager.transition(SystemState.ANALYZING, context);
            Evaluator.Evaluation initialEval = manager.getEvaluator().evaluateWithSnapshot();
            StateSnapshot snapshot = initialEval.snapshot;
            Trajectory trajectory = new Trajectory();
            FailureMemory failureMemory = manager.getMemoryService().getFailureMemory();

            manager.transition(SystemState.MUTATING, context);
            List<BranchVariant> variants = manager.getDarwinEngine().generateVariants(goal, snapshot, failureMemory, trajectory);

            // Publish variants for the graph
            JSONArray variantsJson = new JSONArray();
            for (BranchVariant v : variants) {
                JSONObject vObj = new JSONObject();
                vObj.put("id", v.getId());
                vObj.put("strategy", v.getStrategy());
                variantsJson.put(vObj);
            }
            eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                    eu.kalafatic.evolution.controller.workflow.RuntimeEventType.MUTATION_REVIEW,
                    context.getSessionId(), "Kernel", variantsJson)
                    .withParent("evolution_loop"));

            manager.checkStep("evolution_loop", "MUTATION", "Darwin variants generated. Review before approval.");

            BehaviorProfile profile = context.getBehaviorProfile();

            // MEDIATED mode behavior
            if (profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED)) {
                context.log("[KERNEL] Darwin in MEDIATED mode: Stopping for user review.");
                String input = context.requestInput("Darwin generated " + variants.size() + " proposals. Review and select one to proceed, or reject to refine.").get();
                if ("Rejected".equalsIgnoreCase(input)) {
                    manager.recordRejection(goal, "Darwin " + state.getCurrentPhase() + " proposals rejected by user.");
                    EvaluationResult res = manager.failedResult();
                    res.setDecision(SelfDevDecision.CONTINUE);
                    manager.transition(SystemState.FAILED, context);
                    return res;
                }
                manager.advanceEvolutionPhase(state);
                manager.transition(SystemState.DONE, context);
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(state.getCurrentPhase()) ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);
                return res;
            }

            if (!context.isAutoApprove()) {
                String input = context.requestInput("Darwin generated " + variants.size() + " variants. Review and approve to start evaluation.").get();
                if (input != null && input.startsWith("EDIT PROPOSAL")) {
                    manager.updateVariantFromInput(variants, input);
                } else if ("Rejected".equalsIgnoreCase(input)) {
                    manager.recordRejection(goal, "Darwin variants rejected by user.");
                    EvaluationResult res = manager.failedResult();
                    res.setDecision(SelfDevDecision.CONTINUE);
                    manager.transition(SystemState.FAILED, context);
                    return res;
                }
            }

            manager.transition(SystemState.PLAN_LOCKED, context);
            manager.getGitManager().forceCheckout(snapshotBranch);
            manager.transition(SystemState.EXECUTING, context);

            BranchVariant bestVariant = manager.evaluateVariantsInternal(variants, manager.getTaskPlanner(), currentIterationModel);

            manager.checkStep("evolution_loop", "BRANCH_COMPARISON", "Evaluation complete. Best variant selected: " + (bestVariant != null ? bestVariant.getId() : "None"));

            if (bestVariant == null || bestVariant.getScore() <= 0) {
                manager.getGitManager().forceCheckout(originalBranch);
                manager.transition(SystemState.FAILED, context);
                return manager.failedResult();
            }

            manager.getGitManager().forceCheckout(originalBranch);
            manager.getGitManager().merge(bestVariant.getBranchName());
            manager.transition(SystemState.VERIFYING, context);
            EvaluationResult result = manager.getEvaluator().evaluate();

            if (result.isSuccess()) {
                String completedPhase = state.getCurrentPhase();
                manager.advanceEvolutionPhase(state);
                if (!EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(completedPhase)) {
                    result.setDecision(SelfDevDecision.CONTINUE);
                }
                manager.getGitManager().commit("Darwin Evolution Phase " + completedPhase);
                manager.transition(SystemState.DONE, context);
            } else {
                manager.getGitManager().rollback();
                manager.transition(SystemState.FAILED, context);
            }
            return result;
        } catch (Exception e) {
            manager.getGitManager().forceCheckout(originalBranch);
            manager.getGitManager().rollback();
            manager.transition(SystemState.FAILED, context);
            throw e;
        }
    }
}
