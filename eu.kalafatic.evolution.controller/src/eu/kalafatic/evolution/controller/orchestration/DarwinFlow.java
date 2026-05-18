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
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IEvaluationContract;
import eu.kalafatic.evolution.controller.orchestration.intent.*;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.supervision.ActivationResolver;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.DecisionResolver;
import eu.kalafatic.evolution.controller.supervision.DecisionSnapshot;
import eu.kalafatic.evolution.controller.supervision.ResolverPolicy;
import eu.kalafatic.evolution.controller.supervision.ManualSelectionPolicy;
import eu.kalafatic.evolution.controller.supervision.TrajectoryStabilityPolicy;
import eu.kalafatic.evolution.controller.supervision.HighestScorePolicy;
import eu.kalafatic.evolution.controller.execution.*;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationGate;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord;
import eu.kalafatic.evolution.controller.trajectory.ResultSynthesizer;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.controller.tools.GitTool;

/**
 * Evolutionary Darwin loop orchestration flow.
 */
@EvolutionComponent(
    domain = "orchestration",
    role = "exploration-orchestrator",
    purpose = "Coordinates multi-branch evolution proposals and evaluation",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.HIGH
)
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
        OrchestrationState state = context.getOrchestrationState();

        context.log("[KERNEL] Strategy-Driven Evolution: Starting Full Darwin Evolution.");
        state.getCognitiveTrace().addNode(new CausalNode(
            "darwin-start-" + System.currentTimeMillis(),
            "STRATEGY_SELECTION",
            "DarwinFlow",
            Collections.emptyList(),
            List.of("DarwinFlow"),
            1.0,
            "Executing dynamic branch strategy system."
        ));

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
            state.setIntentAnalysis(null);
            state.getMetadata().put("intentExpansion", expansion);

            ClarificationPlanner planner = manager.getClarificationPlanner();
            ClarificationPlanner.Strategy strategy = planner.determineStrategy(expansion, context);
            context.log("[KERNEL] Intent Expansion Strategy: " + strategy);

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER) {
                String clarificationRequest = planner.formatClarificationRequest(expansion);
                context.log(clarificationRequest);
                String userResponse = context.requestInput(clarificationRequest).get();
                if ("Rejected".equalsIgnoreCase(userResponse)) {
                    manager.recordRejection(goal, "User rejected clarification request.");
                    return manager.failedResult();
                }

                // Break recursion on approval
                if (userResponse.equalsIgnoreCase("Approved") || userResponse.equalsIgnoreCase("Proceed") || userResponse.equalsIgnoreCase("Yes") || userResponse.equalsIgnoreCase("OK")) {
                    context.log("[KERNEL] User approved intent expansion. Advancing phase.");
                    manager.advanceEvolutionPhase(state);
                } else {
                    goal = goal + " (Clarification: " + userResponse + ")";
                    context.getOrchestrator().getSelfDevSession().setInitialRequest(goal);
                    return runDarwin(context);
                }
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
            FailureMemory failureMemory = context.getKernelContext().getMemoryService().getFailureMemory();

            manager.transition(SystemState.MUTATING, context);
            List<BranchVariant> rawVariants = manager.getDarwinEngine().generateVariants(goal, snapshot, failureMemory, trajectory);

            // Record mutation in trace
            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "darwin-mutation-" + System.currentTimeMillis(),
                "MUTATION",
                "DarwinEngine",
                List.of(goal),
                rawVariants.stream().map(v -> v.getId()).collect(Collectors.toList()),
                1.0,
                "Generated " + rawVariants.size() + " variants."
            ));

            // Scheduling
            ISchedulingContract scheduler = CapabilityRegistry.getInstance().getContractImplementation(ISchedulingContract.ID, ISchedulingContract.class);
            ScheduledExecutionPlan executionPlan = scheduler.schedule(rawVariants, context);
            List<BranchVariant> variants = executionPlan.getScheduledVariants();
            context.getOrchestrationState().getMetadata().put("executionPlan", executionPlan);

            BehaviorProfile profile = context.getBehaviorProfile();
            AuthorityController authority = context.getKernelContext().getAuthority();

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

                String manualId = null;
                if (input.startsWith("Select ")) {
                    manualId = input.substring(7).trim();
                    context.log("[KERNEL] User selected variant: " + manualId);
                }

                authority.decide(iterId, variants, context, manualId);

                // Populate Iteration model with winning variant metadata
                final String finalManualId = manualId;
                BranchVariant manualWinner = variants.stream()
                        .filter(v -> v.getId().equals(finalManualId))
                        .findFirst().orElse(null);
                if (manualWinner != null && currentIterationModel != null) {
                    currentIterationModel.setSurvivalArgument(manualWinner.getSurvivalArgument());
                    currentIterationModel.setTradeoffs(manualWinner.getTradeoffs());
                    currentIterationModel.setFailureRisks(manualWinner.getFailureRisks());
                    currentIterationModel.setJustification("Manual selection: " + manualWinner.getStrategy());
                }

                manager.advanceEvolutionPhase(state);
                manager.transition(SystemState.DONE, context);
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(state.getCurrentPhase()) ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);
                return res;
            }

            manager.transition(SystemState.PLAN_LOCKED, context);
            manager.getGitManager().forceCheckout(snapshotBranch);
            manager.transition(SystemState.EXECUTING, context);

            // Filter already active branches
            List<BranchVariant> activeVariants = variants.stream()
                    .filter(v -> v.getActivationState() == BranchVariant.ActivationState.ACTIVE)
                    .collect(Collectors.toList());
            BranchVariant selectedVariant = null;

            if (!activeVariants.isEmpty()) {
                selectedVariant = evaluateVariantsInternal(activeVariants, manager.getTaskPlanner(), currentIterationModel, context, executionPlan);
            } else {
                selectedVariant = evaluateVariantsInternal(variants, manager.getTaskPlanner(), currentIterationModel, context, executionPlan);
                if (!profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED) && selectedVariant != null) {
                    authority.decide(iterId, variants, context, null);
                }
            }

            // Sync winner metadata to EMF Iteration model
            if (selectedVariant != null && currentIterationModel != null) {
                currentIterationModel.setSurvivalArgument(selectedVariant.getSurvivalArgument());
                currentIterationModel.setTradeoffs(selectedVariant.getTradeoffs());
                currentIterationModel.setFailureRisks(selectedVariant.getFailureRisks());
                currentIterationModel.setJustification(selectedVariant.getStrategy());
            }

            if (selectedVariant == null || (selectedVariant.getActivationState() != BranchVariant.ActivationState.ACTIVE && selectedVariant.getScore() < 0.5)) {
                manager.getGitManager().forceCheckout(originalBranch);
                manager.transition(SystemState.FAILED, context);
                return manager.failedResult();
            }

            manager.getGitManager().forceCheckout(originalBranch);
            manager.getGitManager().merge(selectedVariant.getBranchName());

            // ARCHITECTURAL CHANGE: Git-based Validation ("Reality Check")
            GitTool validationGit = new GitTool();
            String finalDiff = validationGit.execute("diff HEAD^ HEAD", context.getProjectRoot(), context);
            context.log("[KERNEL] Reality Check: Winner variant applied. Actual physical change size: " + finalDiff.length() + " chars.");

            boolean hasExpectedOutputs = !selectedVariant.getExpectedOutputs().isEmpty();
            boolean outputFulfilled = true;
            if (hasExpectedOutputs) {
                for (String expected : selectedVariant.getExpectedOutputs()) {
                    File f = new File(context.getProjectRoot(), expected);
                    if (!f.exists()) {
                        context.log("[KERNEL] Reality divergence: Expected output missing: " + expected);
                        outputFulfilled = false;
                    }
                }
            }

            if ((finalDiff.isEmpty() && !selectedVariant.getActions().isEmpty()) || !outputFulfilled) {
                context.log("[KERNEL] WARNING: Reality divergence detected. Physical outcome missing expected changes or files.");
                // Emit divergence signal to SignalBus
                eu.kalafatic.evolution.controller.trajectory.SignalBus.getInstance().publish(
                    new eu.kalafatic.evolution.controller.trajectory.EvaluationSignal(
                        selectedVariant.getId(), "RealityCheck", 0.0, 1.0, eu.kalafatic.evolution.controller.trajectory.SignalSeverity.CRITICAL, "Physical outcome missing expected changes.")
                );
            } else {
                context.log("[KERNEL] Reality check passed. Physical changes align with architectural hypothesis.");
            }

            manager.transition(SystemState.VERIFYING, context);
            IEvaluationContract evaluator = CapabilityRegistry.getInstance().getContractImplementation(IEvaluationContract.ID, IEvaluationContract.class);
            EvaluationResult result = evaluator.evaluate(context.getProjectRoot(), context, manager.getEvaluator() != null ? manager.getEvaluator().getMavenTool() : null);

            if (result.isSuccess()) {
                String completedPhase = state.getCurrentPhase();
                manager.advanceEvolutionPhase(state);

                // RECORD SUCCESSFUL EVOLUTION
                IterationRecord record = new IterationRecord();
                record.setIteration(state.getIterationCount());
                record.setGoal(goal);
                record.setStrategy(selectedVariant.getStrategy());
                record.setBranchId(selectedVariant.getId());
                record.setResult("SUCCESS");
                record.setActivationState("ACTIVE");
                record.setTimestamp(System.currentTimeMillis());
                context.getKernelContext().getMemoryService().saveRecord(record);

                if (result.getTestPassRate() >= 1.0) {
                    state.setCuriosityEnabled(true);
                    state.getMetadata().put("artifact_stable", true);
                }

                manager.getGitManager().commit("Darwin Evolution Phase " + completedPhase, context);
                manager.transition(SystemState.DONE, context);
                result.setDecision(EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(completedPhase) ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);
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

        boolean parallelDisabled = "true".equalsIgnoreCase(System.getProperty("evolution.darwin.parallel.disabled"));
        try {
            if (parallelDisabled) {
                context.log("[KERNEL] Darwin parallel execution disabled. Evaluating variants sequentially.");
                for (BranchVariant variant : variants) {
                    evaluateVariantParallel(variant, planner, context);
                }
            } else {
                List<CompletableFuture<BranchVariant>> futures = variants.stream()
                    .map(variant -> CompletableFuture.supplyAsync(() -> evaluateVariantParallel(variant, planner, context), variantExecutor))
                    .collect(Collectors.toList());
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        } catch (Exception e) {
            context.log("[KERNEL] Variant evaluation error: " + e.getMessage());
        }

        AuthorityController authority = context.getKernelContext().getAuthority();
        AuthorityController.AuthorityDecision decision = authority.decide(iteration.getId(), variants, context, null);

        ResultSynthesizer synthesizer = new ResultSynthesizer();
        synthesizer.synthesize(variants, context);

        // RECORD TRAJECTORY ANALYSIS
        for (BranchVariant v : variants) {
            TrajectoryAnalysisRecord tar = new TrajectoryAnalysisRecord();
            tar.setIterationId(iteration.getId());
            tar.setBranchId(v.getId());
            tar.setStrategy(v.getStrategy());
            tar.setFitnessScore(v.getScore());
            context.getKernelContext().getMemoryService().saveTrajectoryAnalysis(tar);
        }

        BranchVariant bestVariant = null;
        if (decision.getVariantId() != null) {
            bestVariant = variants.stream()
                .filter(v -> v.getId().equals(decision.getVariantId()))
                .findFirst().orElse(null);
        }

        // Ensure all async writes are committed
        context.getKernelContext().getMemoryService().flush();

        return bestVariant;
    }

    private BranchVariant evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context) {
        File tempDir = null;
        AuthorityController authority = context.getKernelContext().getAuthority();
        try {
            tempDir = Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
            manager.getGitManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
            variantContext.setKernelContext(context.getKernelContext());
            variantContext.getMetadata().put("variantId", variant.getId());
            variantContext.setPlatformMode(context.getPlatformMode());
            variantContext.setAutoApprove(true);
            variantContext.setAiService(aiService);
            List<Task> tasks = planner.generateTasksFromVariant(variantContext, variant);
            IterationManager variantManager = KernelFactory.create(variantContext, aiService);

            // ARCHITECTURAL CHANGE: Dynamic Re-Evaluation Loop
            // Sequential task execution with reality checks and re-evaluation
            final File finalTempDir = tempDir;
            final IterationManager finalVariantManager = variantManager;
            boolean success = true;
            authority.updateLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.EXECUTING, context);
            for (Task task : tasks) {
                boolean taskSuccess = finalVariantManager.executeTasksWithRetries(List.of(task));
                if (!taskSuccess) {
                    success = false;
                    break;
                }

                try {
                    // 1. Observe Git changes after each task (Reality Check)
                    GitTool gitTool = new GitTool();
                    String diff = gitTool.execute("diff HEAD", finalTempDir, variantContext);
                    context.log("[DARWIN] Observed Git changes for variant " + variant.getId() + ": " + diff.length() + " chars");

                    // 2. Trigger event-sourced mechanism for GitEmfReconciler
                    eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                        new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                            eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TOOL_EXECUTION_SUCCEEDED,
                            "DarwinFlow", "GitTool", diff));

                    // 3. Intermediate Re-Evaluation using ActivationResolver
                    ActivationResolver resolver = new ActivationResolver(context.getSemanticWorkspace().getTrajectoryMemory());
                    DecisionSnapshot intermediateDecision = resolver.resolve(variantContext.getOrchestrationState().getCurrentIterationId(), List.of(variant), SignalBus.getInstance().getSignalsForVariant(variant.getId()));

                    // 4. Update Trajectory metrics
                    Trajectory t = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(variant.getTrajectoryId());
                    if (t != null) {
                        double currentFitness = intermediateDecision.getAggregatedScores().getOrDefault(variant.getId(), 0.5);
                        t.setFitnessScore(currentFitness);
                        t.getFitnessHistory().add(currentFitness);
                        t.setStabilityScore(intermediateDecision.getAvgLongTermStability());

                        if (intermediateDecision.isExplorationTriggered()) {
                            context.log("[DARWIN] High policy disagreement detected for variant " + variant.getId() + ". Weakening trajectory.");
                            t.setPhase(Trajectory.Phase.COLLAPSE);
                        }
                    }

                    // Update EMF with new evidence
                    if (finalVariantManager.getCurrentIterationModel() != null) {
                        finalVariantManager.getCurrentIterationModel().setJustification("Step [" + task.getName() + "] completed. Fitness: " + String.format("%.2f", t != null ? t.getFitnessScore() : 0.0));
                    }

                } catch (Exception e) {
                    context.log("[DARWIN] Error during dynamic re-evaluation: " + e.getMessage());
                }
            }

            if (success) {
                variantManager.getGitManager().commit("Darwin Variant Execution: " + variant.getStrategy(), variantContext);
                authority.updateLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.VERIFIED, context);
            } else {
                authority.updateLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.REJECTED, context);
            }
            variant.setSuccess(success);

            IEvaluationContract evaluator = CapabilityRegistry.getInstance().getContractImplementation(IEvaluationContract.ID, IEvaluationContract.class);
            EvaluationResult result = evaluator.evaluate(tempDir, variantContext, manager.getEvaluator() != null ? manager.getEvaluator().getMavenTool() : null);
            variant.setSuccess(result.isSuccess());
            if (result.isSuccess()) {
                authority.updateLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING, context);
            }

            GitTool deltaTool = new GitTool();
            variant.setMutationTrace(deltaTool.execute("diff HEAD^ HEAD", tempDir, variantContext));
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
