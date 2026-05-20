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
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceDeltaAnalyzer;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

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
        SystemState currentState = context.getStateHolder().getState();
        if (currentState == SystemState.DONE || currentState == SystemState.FAILED) {
            manager.transition(SystemState.INIT, context);
        }

        String goal = context.getOrchestrator().getSelfDevSession() != null ? context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";
        OrchestrationState state = context.getOrchestrationState();

        EvolutionPhaseMachine phaseMachine = new EvolutionPhaseMachine();
        EvolutionPhase phase = state.getCurrentPhase() != null ? EvolutionPhase.fromString(state.getCurrentPhase()) : phaseMachine.getInitialPhase();

        state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(phase));
        Iteration currentIterationModel = manager.getCurrentIterationModel();
        if (currentIterationModel != null) {
            currentIterationModel.setPhase(state.getCurrentPhase());
        }

        context.log("[KERNEL] Darwin Evolution Phase: " + state.getCurrentPhase());

        if (phase == EvolutionPhase.INTENT_EXPANSION) {
            manager.transition(SystemState.ANALYZING, context);
            IntentExpansionResult expansion = manager.getIntentExpansionEngine().expand(goal, context);
            state.setIntentAnalysis(null);
            state.getMetadata().put("intentExpansion", expansion);

            context.log("[KERNEL] Intent Interpretation: " + expansion.getState());
            if (expansion.getDominantIntent() != null) {
                context.log("[KERNEL] Dominant Intent: " + expansion.getDominantIntent());
            }

            ClarificationPlanner planner = manager.getClarificationPlanner();
            ClarificationPlanner.Strategy strategy = planner.determineStrategy(expansion, context);
            context.log("[KERNEL] Clarification Strategy: " + strategy);

            if (strategy == ClarificationPlanner.Strategy.BRANCH_PARALLEL) {
                context.log("[KERNEL] Ambiguity detected but evolvable. Spawning parallel implementation branches.");
                state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(phaseMachine.next(phase)));
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(SelfDevDecision.CONTINUE);
                return res;
            }

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER) {
                String clarificationRequest = planner.formatClarificationRequest(expansion);
                context.log(clarificationRequest);
                String userResponse = context.requestInput(clarificationRequest).get();
                if ("Rejected".equalsIgnoreCase(userResponse)) {
                    manager.recordRejection(goal, "User rejected clarification request.");
                    manager.transition(SystemState.FAILED, context);
                    return manager.failedResult();
                }

                if (userResponse.equalsIgnoreCase("Approved") || userResponse.equalsIgnoreCase("Proceed") || userResponse.equalsIgnoreCase("Yes") || userResponse.equalsIgnoreCase("OK")) {
                    context.log("[KERNEL] User approved intent expansion. Advancing phase.");
                    state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(phaseMachine.next(phase)));
                    EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                    res.setSuccess(true);
                    res.setDecision(SelfDevDecision.CONTINUE);
                    return res;
                } else {
                    goal = goal + " (Clarification: " + userResponse + ")";
                    context.getOrchestrator().getSelfDevSession().setInitialRequest(goal);
                    EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                    res.setSuccess(true);
                    res.setDecision(SelfDevDecision.CONTINUE);
                    return res;
                }
            }
            state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(phaseMachine.next(phase)));

            // If intent is clear, proceed immediately to next phase in same iteration
            if (strategy != ClarificationPlanner.Strategy.AUTO_INFER) {
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(SelfDevDecision.CONTINUE);
                return res;
            }
            context.log("[KERNEL] Intent clear. Proceeding to architectural exploration.");
        }

        // Implementation phases
        if (manager.getGitManager().isGitRepository()) {
            manager.getGitManager().ensureInitialCommit();
        }
        String originalBranch = manager.getGitManager().getCurrentBranch();
        String baseCommit = manager.getGitManager().getHeadCommit();
        Iteration currentIterationModelImpl = manager.getCurrentIterationModel();
        String iterId = currentIterationModelImpl != null ? currentIterationModelImpl.getId() : "default";
        String snapshotBranch = "snapshot/" + iterId + "-" + System.currentTimeMillis();

        try {
            manager.getGitManager().createBranchFrom(originalBranch, snapshotBranch);
            manager.transition(SystemState.ANALYZING, context);
            Evaluator.Evaluation initialEval = manager.getEvaluator().evaluateWithSnapshot();
            StateSnapshot snapshot = initialEval.snapshot;
            Trajectory trajectory = new Trajectory();
            FailureMemory failureMemory = context.getKernelContext().getMemoryService().getFailureMemory();

            manager.transition(SystemState.MUTATING, context);
            List<BranchVariant> rawVariants;
            try {
                rawVariants = manager.getDarwinEngine().generateVariants(goal, snapshot, failureMemory, trajectory);
            } catch (Exception e) {
                context.log("[DARWIN] FATAL ERROR: Mutation engine failed: " + e.getMessage());
                manager.transition(SystemState.FAILED, context);
                return manager.failedResult();
            }

            if (rawVariants.isEmpty()) {
                context.log("[DARWIN] ERROR: No variants generated for goal. Evolution blocked.");
                manager.transition(SystemState.FAILED, context);
                return manager.failedResult();
            }

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "darwin-mutation-" + System.currentTimeMillis(),
                "MUTATION",
                "DarwinEngine",
                List.of(goal),
                rawVariants.stream().map(v -> v.getId()).collect(Collectors.toList()),
                1.0,
                "Generated " + rawVariants.size() + " variants."
            ));

            ISchedulingContract scheduler = CapabilityRegistry.getInstance().getContractImplementation(ISchedulingContract.ID, ISchedulingContract.class);
            ScheduledExecutionPlan executionPlan = scheduler.schedule(rawVariants, context);
            List<BranchVariant> variants = executionPlan.getScheduledVariants();
            context.getOrchestrationState().getMetadata().put("executionPlan", executionPlan);

            BehaviorProfile profile = context.getBehaviorProfile();

            manager.transition(SystemState.PLAN_LOCKED, context);
            manager.getGitManager().forceCheckout(snapshotBranch);
            manager.transition(SystemState.EXECUTING, context);

            List<BranchVariant> activeVariants = variants.stream()
                    .filter(v -> v.getActivationState() == BranchVariant.ActivationState.ACTIVE)
                    .collect(Collectors.toList());

            if (!activeVariants.isEmpty()) {
                evaluateVariantsInternal(activeVariants, manager.getTaskPlanner(), currentIterationModelImpl, context, executionPlan, baseCommit);
            } else {
                evaluateVariantsInternal(variants, manager.getTaskPlanner(), currentIterationModelImpl, context, executionPlan, baseCommit);
            }

            BranchVariant selectedVariant = null;
            String manualId = null;
            if (profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED) || !context.isAutoApprove()) {
                context.log("[KERNEL] Darwin Evolution: Stopping for user review (Auto-Approve: " + context.isAutoApprove() + ")");
                StringBuilder sb = new StringBuilder("Darwin generated " + variants.size() + " evaluated proposals:\n");
                for (BranchVariant v : variants) {
                    sb.append(String.format("- [%s] %s (Score: %.2f)\n", v.getId(), v.getStrategy(), v.getScore()));
                }
                sb.append("\nReview and select one to proceed (e.g. 'Select v0'), or reject to refine.");

                String input = context.requestInput(sb.toString()).get();
                if ("Rejected".equalsIgnoreCase(input)) {
                    manager.recordRejection(goal, "Darwin " + state.getCurrentPhase() + " proposals rejected by user.");
                    EvaluationResult res = manager.failedResult();
                    res.setDecision(SelfDevDecision.CONTINUE);
                    manager.transition(SystemState.FAILED, context);
                    return res;
                }

                if (input.startsWith("Select ")) {
                    manualId = input.substring(7).trim();
                } else if (input.startsWith("Approve variant ")) {
                    manualId = input.substring(16).trim();
                }

                if (manualId != null) {
                    context.log("[KERNEL] User selected variant: " + manualId);
                }
            }

            // SINGLE AUTHORITY DECISION CALL
            eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision = manager.decide(iterId, variants, context, manualId);

            String finalWinnerId = decision.getSelectedVariantId();
            if (finalWinnerId != null) {
                selectedVariant = variants.stream()
                        .filter(v -> v.getId().equals(finalWinnerId))
                        .findFirst().orElse(null);

                // STAMP APPROVED FOR UI
                if (selectedVariant != null) {
                    JSONArray updatedVariants = new JSONArray();
                    for (BranchVariant v : variants) {
                        JSONObject vObj = new JSONObject();
                        vObj.put("id", v.getId());
                        vObj.put("strategy", v.getStrategy());
                        vObj.put("strategy_type", v.getStrategyType());
                        vObj.put("score", v.getScore());
                        if (v.getId().equals(finalWinnerId)) {
                            vObj.put("approved", true);
                        } else {
                            vObj.put("approved", false);
                        }
                        updatedVariants.put(vObj);
                    }
                    context.log("[APPROVED:" + finalWinnerId + "] [DARWIN_BRANCHES] " + updatedVariants.toString());
                }
            }

            if (selectedVariant != null && currentIterationModelImpl != null) {
                currentIterationModelImpl.setSurvivalArgument(selectedVariant.getSurvivalArgument());
                currentIterationModelImpl.setTradeoffs(selectedVariant.getTradeoffs());
                currentIterationModelImpl.setFailureRisks(selectedVariant.getFailureRisks());
                currentIterationModelImpl.setJustification(selectedVariant.getStrategy());
            }

            if (selectedVariant == null || (selectedVariant.getActivationState() != BranchVariant.ActivationState.ACTIVE && selectedVariant.getScore() < 0.5)) {
                manager.getGitManager().forceCheckout(originalBranch);
                manager.getGitManager().rollback();
                manager.transition(SystemState.FAILED, context);
                return manager.failedResult();
            }

            manager.getGitManager().forceCheckout(originalBranch);
            manager.getGitManager().merge(selectedVariant.getBranchName());

            WorkspaceDeltaAnalyzer analyzer = new WorkspaceDeltaAnalyzer(context.getProjectRoot(), context);
            WorkspaceDeltaAnalyzer.DeltaAnalysis reality = analyzer.analyze(baseCommit);
            context.log("[KERNEL] Reality Check: Winner variant applied. Analysis: " + reality.toString());

            // Record physical changes for Final Response
            reality.getChangedFileMap().forEach((path, type) -> {
                context.getFileChangeTracker().recordChange(path, type);
            });

            boolean isSynthesis = context.getOrchestrationState().getCurrentPhase() != null && context.getOrchestrationState().getCurrentPhase().contains("SYNTHESIS");
            if (!reality.isSignificant() && !isSynthesis) {
                context.log("[KERNEL] Reality Check WARNING: Winner variant resulted in NO physical changes in phase " + context.getOrchestrationState().getCurrentPhase());
                // In early implementation phases, we might want to allow this if it's purely structural/analytical,
                // but we should signal to the next iteration that more pressure is needed.
                context.getOrchestrationState().getMetadata().put("lastRealityCheckSignificant", false);
            } else {
                context.getOrchestrationState().getMetadata().put("lastRealityCheckSignificant", true);
            }

            manager.transition(SystemState.VERIFYING, context);
            IEvaluationContract evaluator = CapabilityRegistry.getInstance().getContractImplementation(IEvaluationContract.ID, IEvaluationContract.class);
            EvaluationResult result = evaluator.evaluate(context.getProjectRoot(), context, manager.getEvaluator() != null ? manager.getEvaluator().getMavenTool() : null);

            // HARDENING: Non-final phases succeed even with failing builds (e.g. initial empty project)
            boolean isFinalPhase = EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(state.getCurrentPhase());
            if (result.isSuccess() || (!isFinalPhase && selectedVariant != null)) {
                String completedPhase = state.getCurrentPhase();
                IterationRecord record = new IterationRecord();
                record.setIteration(state.getIterationCount());
                record.setGoal(goal);
                record.setStrategy(selectedVariant.getStrategy());
                record.setBranchId(selectedVariant.getId());
                record.setResult(result.isSuccess() ? "SUCCESS" : "SUCCESS_WITH_BUILD_ERROR");
                record.setActivationState("ACTIVE");
                record.setTimestamp(System.currentTimeMillis());
                context.getKernelContext().getMemoryService().saveRecord(record);

                manager.getGitManager().commit("Darwin Evolution Phase " + completedPhase, context);
                manager.transition(SystemState.DONE, context);

                String nextPhase = EvolutionPhaseMachine.toLegacyString(phaseMachine.next(phase));
                state.setCurrentPhase(nextPhase);

                result.setDecision(isFinalPhase ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);
                result.setSuccess(true); // Treat phase as successful to allow progression

                if (!isFinalPhase && !context.isAutoApprove()) {
                    context.log("[KERNEL] Darwin Evolution: Phase " + completedPhase + " completed. Pausing for user confirmation before next phase: " + nextPhase);
                    try {
                        String userResponse = context.requestInput("Phase " + completedPhase + " completed successfully. Proceed to " + nextPhase + "? (Yes/No)").get();
                        if ("No".equalsIgnoreCase(userResponse) || "Reject".equalsIgnoreCase(userResponse)) {
                             context.log("[KERNEL] User stopped evolution after phase " + completedPhase);
                             result.setDecision(SelfDevDecision.STOP);
                        }
                    } catch (Exception e) {
                        context.log("[KERNEL] Error during phase confirmation: " + e.getMessage());
                    }
                }

                return result;
            } else {
                manager.getGitManager().rollback();
                manager.transition(SystemState.FAILED, context);
                return result;
            }
        } catch (Exception e) {
            manager.getGitManager().forceCheckout(originalBranch);
            manager.getGitManager().rollback();
            manager.transition(SystemState.FAILED, context);
            throw e;
        }
    }

    public void evaluateVariantsInternal(List<BranchVariant> variants, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, Iteration iteration, TaskContext context, ScheduledExecutionPlan executionPlan, String baseCommit) throws Exception {
        String baseBranch = manager.getGitManager().getCurrentBranch();
        for (BranchVariant variant : variants) {
            // IMMUTABLE BRANCH PROVISIONING
            manager.getGitManager().createBranchFrom(baseBranch, variant.getBranchName());
        }

        boolean parallelDisabled = "true".equalsIgnoreCase(System.getProperty("evolution.darwin.parallel.disabled"));
        try {
            if (parallelDisabled) {
                context.log("[KERNEL] Darwin parallel execution disabled. Evaluating variants sequentially.");
                for (BranchVariant variant : variants) {
                    evaluateVariantParallel(variant, planner, context, baseCommit);
                }
            } else {
                List<CompletableFuture<BranchVariant>> futures = variants.stream()
                    .map(variant -> CompletableFuture.supplyAsync(() -> evaluateVariantParallel(variant, planner, context, baseCommit), variantExecutor))
                    .collect(Collectors.toList());
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        } catch (Exception e) {
            context.log("[KERNEL] Variant evaluation error: " + e.getMessage());
        }

        ResultSynthesizer synthesizer = new ResultSynthesizer();
        synthesizer.synthesize(variants, context);

        for (BranchVariant v : variants) {
            TrajectoryAnalysisRecord tar = new TrajectoryAnalysisRecord();
            tar.setIterationId(iteration.getId());
            tar.setBranchId(v.getId());
            tar.setStrategy(v.getStrategy());
            tar.setFitnessScore(v.getScore());
            context.getKernelContext().getMemoryService().saveTrajectoryAnalysis(tar);
        }

        context.getKernelContext().getMemoryService().flush();

        // SELECTED VARIANT DETERMINATION IS NOW DEFERRED TO SINGLE AUTHORITY CALL IN runDarwin
    }

    private BranchVariant evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context, String baseCommit) {
        File tempDir = null;
        AuthorityController authority = context.getKernelContext().getAuthority();
        VariantExecutionContext variantExecContext = new VariantExecutionContext(variant.getId());

        try {
            tempDir = Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
            manager.getBranchManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
            variantContext.setKernelContext(context.getKernelContext());
            variantContext.getMetadata().put("variantId", variant.getId());
            variantContext.getMetadata().put("variantExecContext", variantExecContext);
            variantContext.setPlatformMode(context.getPlatformMode());
            variantContext.setAutoApprove(true);
            variantContext.setAiService(aiService);

            List<Task> tasks = planner.generateTasksFromVariant(variantContext, variant);
            IterationManager variantManager = KernelFactory.create(variantContext, aiService);

            boolean success = true;
            manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.EXECUTING, context);
            for (Task task : tasks) {
                boolean taskSuccess = variantManager.executeTasksWithRetries(List.of(task));
                if (!taskSuccess) {
                    success = false;
                    break;
                }

                try {
                    GitTool gitTool = new GitTool();
                    String diff = gitTool.execute("diff HEAD", tempDir, variantContext);

                    RuntimeEvent event = new RuntimeEvent(
                            eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TOOL_EXECUTION_SUCCEEDED,
                            "DarwinFlow", "GitTool", diff);
                    variantExecContext.recordEvent(event);

                    ActivationResolver resolver = new ActivationResolver(context.getSemanticWorkspace().getTrajectoryMemory());
                    DecisionSnapshot intermediateDecision = resolver.resolve(variantContext.getOrchestrationState().getCurrentIterationId(), List.of(variant), SignalBus.getInstance().getSignalsForVariant(variant.getId()), variantContext);

                    Trajectory t = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(variant.getTrajectoryId());
                    if (t != null) {
                        double currentFitness = intermediateDecision.getAggregatedScores().getOrDefault(variant.getId(), 0.5);
                        t.setFitnessScore(currentFitness);
                        t.getFitnessHistory().add(currentFitness);
                        t.setStabilityScore(intermediateDecision.getAvgLongTermStability());
                    }
                } catch (Exception e) {
                    context.log("[DARWIN] Error during dynamic re-evaluation for variant " + variant.getId() + ": " + e.getMessage());
                }
            }

            if (success) {
                variantManager.getGitManager().commit("Darwin Variant Execution: " + variant.getStrategy(), variantContext);
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.VERIFIED, context);
            } else {
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.REJECTED, context);
            }
            variant.setSuccess(success);

            IEvaluationContract evaluator = CapabilityRegistry.getInstance().getContractImplementation(IEvaluationContract.ID, IEvaluationContract.class);
            EvaluationResult result = evaluator.evaluate(tempDir, variantContext, manager.getEvaluator() != null ? manager.getEvaluator().getMavenTool() : null);
            variant.setSuccess(result.isSuccess());
            if (result.isSuccess()) {
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING, context);
            }

            GitTool deltaTool = new GitTool();
            variant.setMutationTrace(deltaTool.execute("diff " + baseCommit + " HEAD", tempDir, variantContext));
            variant.setScore(result.isSuccess() ? 0.8 + (result.getTestPassRate() * 0.2) : result.getTestPassRate() * 0.5);

            return variant;
        } catch (Exception e) {
            variant.setScore(0.0);
            return variant;
        } finally {
            if (tempDir != null) {
                try {
                    manager.getBranchManager().removeWorktree(tempDir.getAbsolutePath());
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
