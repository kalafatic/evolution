package eu.kalafatic.evolution.controller.orchestration;

import java.util.Collections;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.intent.*;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.supervision.*;
import eu.kalafatic.evolution.controller.execution.*;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationGate;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
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
    private final KernelScheduler scheduler = new KernelScheduler();
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

        if (EvolutionConstants.PHASE_INTENT_EXPANSION.equals(state.getCurrentPhase())) {
            manager.transition(SystemState.ANALYZING, context);
            IntentExpansionResult expansion = manager.getIntentExpansionEngine().expand(goal, context);
            state.setIntentAnalysis(null); // Clear old simple analysis
            state.getMetadata().put("intentExpansion", expansion);

            ClarificationPlanner planner = manager.getClarificationPlanner();
            ClarificationPlanner.Strategy strategy = planner.determineStrategy(expansion);
            context.log("[KERNEL] Intent Expansion Strategy: " + strategy);

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER) {
                String clarificationRequest = planner.formatClarificationRequest(expansion);
                String userResponse = context.requestInput(clarificationRequest).get();
                if ("Rejected".equalsIgnoreCase(userResponse)) {
                    manager.recordRejection(goal, "User rejected clarification request.");
                    return manager.failedResult();
                }
                // Update goal with clarification and restart phase
                goal = goal + " (Clarification: " + userResponse + ")";
                context.getOrchestrator().getSelfDevSession().setInitialRequest(goal);
                return runDarwin(context);
            }
        }

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
            List<BranchVariant> rawVariants = manager.getDarwinEngine().generateVariants(goal, snapshot, failureMemory, trajectory);

            // Kernel Scheduling Layer
            ScheduledExecutionPlan executionPlan = scheduler.schedule(rawVariants, context);
            context.log("[KERNEL] Scheduler decision: " + executionPlan.getDecisionReason());
            List<BranchVariant> variants = executionPlan.getScheduledVariants();
            context.getOrchestrationState().getMetadata().put("executionPlan", executionPlan);
            eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().setBudget(executionPlan.getAppliedBudget());
            scheduler.getBackpressure().resetCounters();

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

                // Decision Authority: ActivationResolver handles user selection
                ActivationResolver userResolver = new ActivationResolver();
                List<ResolverPolicy> userPolicies = new ArrayList<>();
                if (input.startsWith("Select ")) {
                    String selectedId = input.substring(7).trim();
                    userPolicies.add(new ManualSelectionPolicy(selectedId));
                }

                DecisionSnapshot userDecision = userResolver.resolve(iterId, new ArrayList<>(), recommendations, userPolicies, context.getSemanticWorkspace());
                context.log("[KERNEL] Authority Decision (Mediated): " + userDecision.toString());

                if (userDecision.getSelectedVariantId() != null) {
                    variants.forEach(v -> {
                        if (v.getId().equals(userDecision.getSelectedVariantId())) {
                            v.setActivationState(BranchVariant.ActivationState.ACTIVE);
                            v.setRank("winner");
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
                selectedVariant = evaluateVariantsInternal(activeVariants, manager.getTaskPlanner(), currentIterationModel, context, executionPlan);
            } else {
                context.log("[KERNEL] Authority: No ACTIVE branches. Evaluating all proposals to determine best candidate for activation.");
                selectedVariant = evaluateVariantsInternal(variants, manager.getTaskPlanner(), currentIterationModel, context, executionPlan);

                // Decision Authority: Use ActivationResolver for auto-activation policy
                if (!profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED) && selectedVariant != null) {
                    List<ActivationRecommendation> recommendations = activationGate.recommendActivations(variants);
                    ActivationResolver autoResolver = new ActivationResolver();
                    List<ResolverPolicy> autoPolicies = new ArrayList<>();
                    autoPolicies.add(new TrajectoryStabilityPolicy(context.getSemanticWorkspace()));
                    autoPolicies.add(new ConfidenceThresholdPolicy(activationGate.getDefaultActivationThreshold()));

                    DecisionSnapshot autoDecision = autoResolver.resolve(iterId, new ArrayList<>(), recommendations, autoPolicies, context.getSemanticWorkspace());
                    context.log("[KERNEL] Authority Decision (Autonomous): " + autoDecision.toString());

                    if (autoDecision.getSelectedVariantId() != null && autoDecision.getSelectedVariantId().equals(selectedVariant.getId())) {
                        selectedVariant.setActivationState(BranchVariant.ActivationState.ACTIVE);
                        selectedVariant.setRank("winner");
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

    public BranchVariant evaluateVariantsInternal(List<BranchVariant> variants, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, Iteration iteration, TaskContext context, ScheduledExecutionPlan executionPlan) throws Exception {
        String baseBranch = manager.getGitManager().getCurrentBranch();
        for (BranchVariant variant : variants) {
            manager.getGitManager().createBranch(variant.getBranchName());
            manager.getGitManager().forceCheckout(baseBranch);
        }

        // Authority Layer: Collect signals during parallel execution
        List<EvaluationSignal> collectedSignals = Collections.synchronizedList(new ArrayList<>());
        RuntimeEventListener signalListener = event -> {
            if (event.getType() == RuntimeEventType.EVALUATION_SIGNAL_CREATED && event.getPayload() instanceof EvaluationSignal) {
                collectedSignals.add((EvaluationSignal) event.getPayload());
            }
        };
        eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().subscribe(signalListener);

        try {
            List<CompletableFuture<BranchVariant>> futures = variants.stream()
                .map(variant -> CompletableFuture.supplyAsync(() -> {
                    scheduler.getBackpressure().incrementEvaluations();
                    try {
                        return evaluateVariantParallel(variant, planner, context);
                    } finally {
                        scheduler.getBackpressure().decrementEvaluations();
                    }
                }, variantExecutor))
                .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().unsubscribe(signalListener);
        }

        // Decision Authority: Use ActivationResolver to determine winner from signals
        ActivationGate gate = new ActivationGate();
        List<ActivationRecommendation> recommendations = gate.recommendActivations(variants);

        ActivationResolver resolver = new ActivationResolver();
        List<ResolverPolicy> policies = new ArrayList<>();

        // Prioritize historical stability
        policies.add(new TrajectoryStabilityPolicy(context.getSemanticWorkspace()));
        policies.add(new HighestScorePolicy());

        DecisionSnapshot decision = resolver.resolve(iteration.getId(), collectedSignals, recommendations, policies, context.getSemanticWorkspace(), executionPlan);
        context.log("[KERNEL] Authority Decision: " + decision.toString());

        BranchVariant bestVariant = null;
        if (decision.getSelectedVariantId() != null) {
            bestVariant = variants.stream()
                .filter(v -> v.getId().equals(decision.getSelectedVariantId()))
                .findFirst().orElse(null);
        }

        if (bestVariant != null) {
            eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                    eu.kalafatic.evolution.controller.workflow.RuntimeEventType.VARIANT_EVALUATED,
                    context.getSessionId(), "Authority", bestVariant.getId())
                    .withMetadata("score", decision.getAggregatedScores().getOrDefault(bestVariant.getId(), 0.0)));
        }

        return bestVariant;
    }

    private BranchVariant evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context) {
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
            manager.getGitManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
            variantContext.getMetadata().put("variantId", variant.getId());
            variantContext.setPlatformMode(context.getPlatformMode());
            variantContext.setAutoApprove(true);
            variantContext.setAiService(aiService);
            List<Task> tasks = planner.generateTasksFromVariant(variantContext, variant);
            IterationManager variantManager = KernelFactory.create(variantContext, aiService);
            boolean success = variantManager.executeTasksWithRetries(tasks);
            if (success) {
                variantManager.getGitManager().commit("Variant " + variant.getId() + " execution");
            }
            variant.setSuccess(success);

            if (success) {
                variantManager.getGitManager().commit("Darwin Variant Execution: " + variant.getStrategy());
            }

            // Context Authority: Use a variant-specific evaluator bound to the temporary worktree
            Evaluator variantEvaluator = new Evaluator(tempDir, variantContext);
            EvaluationResult result = variantEvaluator.evaluate();
            variant.setSuccess(result.isSuccess());

            // The score is now produced via EvaluationSignal in Evaluator.emitSignal
            // For backward compatibility during this foundational step, we still set it on the variant,
            // but the source of truth for the logic is now mirrored in the signal.
            variant.setScore(result.isSuccess() ? 0.8 + (result.getTestPassRate() * 0.2) : result.getTestPassRate() * 0.5);

            // Record trajectory telemetry
            if (result.isSuccess()) {
                context.getSemanticWorkspace().getTrajectoryMemory().recordSuccessfulStrategy(variant.getStrategy());
            } else {
                context.getSemanticWorkspace().getTrajectoryMemory().recordFailureLoop(variant.getStrategy());
            }

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
