package eu.kalafatic.evolution.controller.orchestration.flows;

import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationGate;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
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
    private static final ExecutorService variantExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
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

            // Activation Gate Integration
            ActivationGate activationGate = new ActivationGate();

            // MEDIATED mode behavior
            if (profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED)) {
                context.log("[KERNEL] Darwin in MEDIATED mode: Stopping for user review.");
                String input = context.requestInput("Darwin generated " + variants.size() + " proposals. Review and select one to proceed (e.g. 'Select v0'), or reject to refine.").get();
                if ("Rejected".equalsIgnoreCase(input)) {
                    manager.recordRejection(goal, "Darwin " + state.getCurrentPhase() + " proposals rejected by user.");
                    EvaluationResult res = manager.failedResult();
                    res.setDecision(SelfDevDecision.CONTINUE);
                    manager.transition(SystemState.FAILED, context);
                    return res;
                }

                // Activation Gate Recommendation
                List<ActivationRecommendation> recommendations = activationGate.recommendActivations(variants);
                recommendations.forEach(r -> context.log("[GATE] " + r.toString()));

                // Decision Authority: Decision made by User based on recommendations
                if (input.startsWith("Select ")) {
                    String selectedId = input.substring(7).trim();
                    variants.forEach(v -> {
                        if (v.getId().equals(selectedId)) {
                            v.setActivationState(BranchVariant.ActivationState.ACTIVE);
                            v.setRank("winner");
                            context.log("[KERNEL] Decision: Branch " + selectedId + " explicitly ACTIVATED by user.");
                        } else {
                            v.setActivationState(BranchVariant.ActivationState.INACTIVE);
                            v.setRank("runner-up");
                        }
                    });
                } else {
                    context.log("[KERNEL] WARNING: No explicit branch activation decision. Darwin evolution may stall.");
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

            // Filter already active branches (e.g. from previous steps)
            List<BranchVariant> activeVariants = variants.stream()
                    .filter(v -> v.getActivationState() == BranchVariant.ActivationState.ACTIVE)
                    .collect(Collectors.toList());
            BranchVariant selectedVariant = null;

            if (!activeVariants.isEmpty()) {
                context.log("[KERNEL] Activation Gate: Proceeding with " + activeVariants.size() + " ACTIVE branches.");
                selectedVariant = evaluateVariantsInternal(activeVariants, manager.getTaskPlanner(), currentIterationModel, context);
            } else {
                context.log("[KERNEL] Authority: No ACTIVE branches. Evaluating all proposals to determine best candidate for activation.");
                selectedVariant = evaluateVariantsInternal(variants, manager.getTaskPlanner(), currentIterationModel, context);

                // Decision Authority: Mode Controller / Policy Layer
                // ActivationGate only provides recommendations, it does NOT auto-activate.
                if (!profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED)) {
                    List<ActivationRecommendation> recommendations = activationGate.recommendActivations(variants);
                    ActivationRecommendation top = recommendations.isEmpty() ? null : recommendations.get(0);

                    // EXPERIMENT mode logic can auto-accept top recommendation, but decision is external to Gate
                    if (top != null && top.getConfidenceScore() >= top.getRecommendedActivationThreshold()) {
                        selectedVariant.setActivationState(BranchVariant.ActivationState.ACTIVE);
                        selectedVariant.setRank("winner");
                        context.log("[KERNEL] Policy Decision: Variant " + selectedVariant.getId() + " activated based on Gate Recommendation (Confidence >= " + top.getRecommendedActivationThreshold() + ")");
                    }
                }
            }

            manager.checkStep("evolution_loop", "BRANCH_COMPARISON", "Evaluation complete. Selected variant: " + (selectedVariant != null ? selectedVariant.getId() : "None"));

            if (selectedVariant == null || (selectedVariant.getActivationState() != BranchVariant.ActivationState.ACTIVE && selectedVariant.getScore() < 0.5)) {
                context.log("[KERNEL] Activation Gate: Evolution halted. No ACTIVE or high-quality variant available (Score: " + (selectedVariant != null ? selectedVariant.getScore() : "N/A") + ")");
                manager.getGitManager().forceCheckout(originalBranch);
                manager.transition(SystemState.FAILED, context);
                return manager.failedResult();
            }

            manager.getGitManager().forceCheckout(originalBranch);
            manager.getGitManager().merge(selectedVariant.getBranchName());
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

    public BranchVariant evaluateVariantsInternal(List<BranchVariant> variants, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, Iteration iteration, TaskContext context) throws Exception {
        String baseBranch = manager.getGitManager().getCurrentBranch();
        for (BranchVariant variant : variants) {
            manager.getGitManager().createBranch(variant.getBranchName());
            manager.getGitManager().forceCheckout(baseBranch);
        }

        // Parallel evaluation optimization: Bypass parallelism if only one variant or in specific test-friendly scenarios
        if (variants.size() == 1 || "true".equals(System.getProperty("evolution.darwin.parallel.disabled"))) {
             BranchVariant v = variants.get(0);
             return evaluateVariantParallel(v, planner, context);
        }

        List<CompletableFuture<BranchVariant>> futures = variants.stream()
            .map(variant -> CompletableFuture.supplyAsync(() -> evaluateVariantParallel(variant, planner, context), variantExecutor))
            .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        BranchVariant bestVariant = null;
        double bestScore = -1.0;
        for (CompletableFuture<BranchVariant> future : futures) {
            BranchVariant variant = future.join();
            if (variant.getScore() > bestScore) {
                bestScore = variant.getScore();
                bestVariant = variant;
            }
        }
        return bestVariant;
    }

    private BranchVariant evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context) {
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
            manager.getGitManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
            variantContext.setPlatformMode(context.getPlatformMode());
            variantContext.setAutoApprove(true);
            variantContext.setAiService(aiService);
            List<Task> tasks = planner.generateTasksFromVariant(variantContext, variant);
            IterationManager variantManager = KernelFactory.create(variantContext, aiService);
            boolean success = variantManager.executeTasksWithRetries(tasks);
            variant.setSuccess(success);

            if (success) {
                variantManager.getGitManager().commit("Darwin Variant Execution: " + variant.getStrategy());
            }

            // Context Authority: Use a variant-specific evaluator bound to the temporary worktree
            Evaluator variantEvaluator = new Evaluator(tempDir, variantContext);
            EvaluationResult result = variantEvaluator.evaluate();
            variant.setSuccess(result.isSuccess());
            variant.setScore(result.isSuccess() ? 0.8 + (result.getTestPassRate() * 0.2) : result.getTestPassRate() * 0.5);
            return variant;
        } catch (Exception e) {
            variant.setScore(0.0);
            return variant;
        } finally {
            if (tempDir != null) {
                try {
                    manager.getGitManager().removeWorktree(tempDir.getAbsolutePath());
                    deleteDirectory(tempDir);
                } catch (Exception e) {}
            }
        }
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) deleteDirectory(file);
        }
        directory.delete();
    }
}
