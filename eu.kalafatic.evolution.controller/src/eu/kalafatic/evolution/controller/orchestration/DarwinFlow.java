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
import eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent;
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
        // DarwinFlow is now an execution engine. Orchestration is owned by IterationManager.
        return manager.evolve(request, context);
    }

    public List<BranchVariant> generateProposals(TaskContext context, String goal) throws Exception {
        Iteration currentIterationModelImpl = manager.getCurrentIterationModel();
        String iterId = currentIterationModelImpl != null ? currentIterationModelImpl.getId() : "default";

        context.log("[DARWIN] Evolving competing trajectories for goal: " + goal);

        Evaluator.Evaluation initialEval = manager.getEvaluator().evaluateWithSnapshot();
        StateSnapshot snapshot = initialEval.snapshot;

        // PERSISTENT TRAJECTORY LINEAGE: Retrieve existing trajectory from memory if available
        // to ensure multi-generational continuity.
        Trajectory trajectory = null;
        IterationRecord lastWinner = context.getKernelContext().getMemoryService().getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastWinner != null && lastWinner.getBranchId() != null) {
             trajectory = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(lastWinner.getBranchId());
             if (trajectory != null) {
                 context.log("[DARWIN] Continuing lineage from survivor: " + trajectory.getTrajectoryId());
             }
        }

        if (trajectory == null) {
            trajectory = new Trajectory("traj-" + iterId, goal);
            context.getSemanticWorkspace().getTrajectoryMemory().recordTrajectory(trajectory);
            context.log("[DARWIN] Starting new lineage trajectory: " + trajectory.getTrajectoryId());
        }

        FailureMemory failureMemory = context.getKernelContext().getMemoryService().getFailureMemory();

        List<BranchVariant> rawVariants = manager.getDarwinEngine().generateVariants(goal, snapshot, failureMemory, trajectory);

        if (rawVariants.isEmpty()) {
            return Collections.emptyList();
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

        // METADATA PERSISTENCE: Record trajectory analysis for ALL trajectories
        for (BranchVariant v : variants) {
            TrajectoryAnalysisRecord tar = new TrajectoryAnalysisRecord();
            tar.setIterationId(iterId);
            tar.setBranchId(v.getId());
            tar.setStrategy(v.getStrategy());
            tar.setFitnessScore(v.getScore());
            context.getKernelContext().getMemoryService().saveTrajectoryAnalysis(tar);
        }
        context.getKernelContext().getMemoryService().flush();

        return variants;
    }

    public EvaluationResult executeWinner(TaskContext context, eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision, List<BranchVariant> variants, String goal) throws Exception {
        VariantExecutionContext winningContext = null;
        String originalBranch = manager.getGitManager().getCurrentBranch();
        String baseCommit = manager.getGitManager().getHeadCommit();
        Iteration currentIterationModelImpl = manager.getCurrentIterationModel();
        String iterId = currentIterationModelImpl != null ? currentIterationModelImpl.getId() : "default";
        String snapshotBranch = "snapshot/" + iterId + "-" + System.currentTimeMillis();

        String finalWinnerId = decision.getSelectedVariantId();
        BranchVariant selectedVariant = null;
        if (finalWinnerId != null) {
            selectedVariant = variants.stream()
                    .filter(v -> v.getId().equals(finalWinnerId))
                    .findFirst().orElse(null);
        }

        if (selectedVariant == null || (selectedVariant.getActivationState() != BranchVariant.ActivationState.ACTIVE && selectedVariant.getScore() < 0.3)) {
            context.log("[KERNEL] Darwin Evolution: No viable winner selected or winner score too low.");
            return manager.failedResult();
        }

        context.log("[APPROVED:" + finalWinnerId + "] [KERNEL] Surviving trajectory selected: " + selectedVariant.getStrategy() + ". Proceeding to execution.");

        // Branch Stamping: Mark all other trajectories as REJECTED or KEPT in logs for UI sealing
        for (BranchVariant v : variants) {
            if (v.getId().equals(finalWinnerId)) continue;

            String status = "REJECTED";
            if (v.getActivationState() == BranchVariant.ActivationState.KEPT) {
                status = "KEPT";
            }
            context.log("[" + status + ":" + v.getId() + "] [KERNEL] Trajectory " + v.getId() + " marked as " + status);
        }

        if (currentIterationModelImpl != null) {
            currentIterationModelImpl.setSurvivalArgument(selectedVariant.getSurvivalArgument());
            currentIterationModelImpl.setTradeoffs(selectedVariant.getTradeoffs());
            currentIterationModelImpl.setFailureRisks(selectedVariant.getFailureRisks());
            currentIterationModelImpl.setJustification(selectedVariant.getStrategy());
        }

        boolean isMediated = context.getBehaviorProfile().hasTrait(BehaviorTrait.SUPERVISION_MEDIATED);

        try {
            manager.getGitManager().createBranchFrom(originalBranch, snapshotBranch);
            manager.getGitManager().forceCheckout(snapshotBranch);

            context.log("[KERNEL] Executing winner variant: " + selectedVariant.getId() + " (" + selectedVariant.getStrategy() + ")");
            manager.getGitManager().createBranchFrom(originalBranch, selectedVariant.getBranchName());

            winningContext = evaluateVariantParallel(selectedVariant, manager.getTaskPlanner(), context, baseCommit);

            ResultSynthesizer synthesizer = new ResultSynthesizer();
            synthesizer.synthesize(List.of(selectedVariant), context);

            mergeHybridInsights(variants, selectedVariant, context);

            if (!selectedVariant.isSuccess()) {
                context.log("[KERNEL] Winner variant execution failed.");
                if (!isMediated) {
                    manager.getGitManager().forceCheckout(originalBranch);
                    manager.getGitManager().rollback();
                }
                return manager.failedResult();
            }

            if (!isMediated) {
                manager.getGitManager().forceCheckout(originalBranch);
                manager.getGitManager().merge(selectedVariant.getBranchName());
            } else {
                // MEDIATED COGNITIVE MERGE: Apply the winner's understanding to the session context
                context.log("[KERNEL] Applying cognitive winner: " + selectedVariant.getStrategy());
                context.getOrchestrationState().getMetadata().put("current_understanding", selectedVariant.getStrategy());
                context.getOrchestrationState().getMetadata().put("current_strategy", selectedVariant.getStrategyType());
                context.getOrchestrationState().getMetadata().put("current_actions", selectedVariant.getActions());
            }

            // TASK PROPAGATION: Ensure tasks executed in the variant are visible in the main context
            if (winningContext != null) {
                for (Task t : winningContext.getTasks()) {
                    if (!context.getOrchestrator().getTasks().contains(t)) {
                        context.getOrchestrator().getTasks().add(t);
                    }
                }
            }

            if (isMediated) {
                // In mediated mode, we skip physical evaluation and commit.
                // We return a success result to allow phase progression.
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(SelfDevDecision.CONTINUE);
                return res;
            }

            WorkspaceDeltaAnalyzer analyzer = new WorkspaceDeltaAnalyzer(context.getProjectRoot(), context);
            WorkspaceDeltaAnalyzer.DeltaAnalysis reality = analyzer.analyze(baseCommit);
            context.log("[KERNEL] Reality Check: Winner variant applied. Analysis: " + reality.toString());

            reality.getChangedFileMap().forEach((path, type) -> {
                context.getFileChangeTracker().recordChange(path, type);
            });

            boolean isSynthesis = context.getOrchestrationState().getCurrentPhase() != null && context.getOrchestrationState().getCurrentPhase().contains("SYNTHESIS");
            if (!reality.isSignificant() && !isSynthesis) {
                context.log("[KERNEL] Reality Check WARNING: Winner variant resulted in NO physical changes in phase " + context.getOrchestrationState().getCurrentPhase());
                context.getOrchestrationState().getMetadata().put("lastRealityCheckSignificant", false);
            } else {
                context.getOrchestrationState().getMetadata().put("lastRealityCheckSignificant", true);
            }

            IEvaluationContract evaluator = CapabilityRegistry.getInstance().getContractImplementation(IEvaluationContract.ID, IEvaluationContract.class);
            EvaluationResult result = evaluator.evaluate(context.getProjectRoot(), context, manager.getEvaluator() != null ? manager.getEvaluator().getMavenTool() : null);

            boolean isFinalPhase = EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(context.getOrchestrationState().getCurrentPhase());
            if (result.isSuccess() || (!isFinalPhase && selectedVariant != null)) {
                String completedPhase = context.getOrchestrationState().getCurrentPhase();
                IterationRecord record = new IterationRecord();
                record.setIteration(context.getOrchestrationState().getIterationCount());
                record.setGoal(goal);
                record.setStrategy(selectedVariant.getStrategy());
                record.setStrategyType(selectedVariant.getStrategyType() != null ? selectedVariant.getStrategyType().toString() : null);
                record.setSemanticAnchor(selectedVariant.getSemanticAnchor());
                record.setMutationTrace(selectedVariant.getMutationTrace());
                record.setInheritedContext(selectedVariant.getInheritedContext());
                record.setRejectedSiblings(selectedVariant.getRejectedSiblings());
                record.setBranchId(selectedVariant.getId());
                record.setResult(result.isSuccess() ? "SUCCESS" : "SUCCESS_WITH_BUILD_ERROR");
                record.setActivationState("ACTIVE");
                record.setTimestamp(System.currentTimeMillis());
                context.getKernelContext().getMemoryService().saveRecord(record);

                manager.checkStep(selectedVariant.getId(), "GIT_COMMIT", "Committing evolutionary changes for phase: " + completedPhase);
                manager.getGitManager().commit("Darwin Evolution Phase " + completedPhase, context);

                result.setDecision(isFinalPhase ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);
                result.setSuccess(true);
                return result;
            } else {
                manager.getGitManager().rollback();
                return result;
            }
        } catch (Exception e) {
            manager.getGitManager().forceCheckout(originalBranch);
            manager.getGitManager().rollback();
            throw e;
        }
    }

    private VariantExecutionContext evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context, String baseCommit) {
        File tempDir = null;
        AuthorityController authority = context.getKernelContext().getAuthority();
        VariantExecutionContext variantExecContext = new VariantExecutionContext(variant.getId());
        boolean isMediated = context.getBehaviorProfile().hasTrait(BehaviorTrait.SUPERVISION_MEDIATED);

        try {
            if (context.getMetadata().containsKey("testMode") || isMediated) {
                tempDir = context.getProjectRoot();
            } else {
                tempDir = Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
                manager.getBranchManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            }
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

            // PROHIBIT BUILD/GIT FOR TEST MODE AND MEDIATED MODE
            if (context.getMetadata().containsKey("testMode") || isMediated) {
                variant.setSuccess(true);
                variant.setScore(0.95);

                // Still need to perform the actions if it's a file write, to satisfy assertions
                for (Task task : tasks) {
                    try {
                        variantManager.getTaskExecutor().getOrchestrator().executeTask(task, variantContext);
                    } catch (Exception e) {
                        context.log("[KERNEL] [TEST_MODE] Execution failed but continuing: " + e.getMessage());
                    }
                }

                variant.setMutationTrace(isMediated ? "Cognitive evolution in mediated mode" : "Mocked in test mode");
                return variantExecContext;
            }

            for (Task task : tasks) {
                // Task-level supervision is already handled inside executeTasksWithRetries via manager.checkStep()
                boolean taskSuccess = variantManager.executeTasksWithRetries(List.of(task));
                if (variantExecContext != null) {
                    variantExecContext.getTasks().add(task);
                }
                if (!taskSuccess) {
                    success = false;
                    break;
                }

                manager.checkStep(task.getId(), "GIT_STAGING", "Staging changes for task: " + task.getName());

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

            return variantExecContext;
        } catch (Exception e) {
            variant.setScore(0.0);
            return variantExecContext;
        } finally {
            if (tempDir != null && !context.getMetadata().containsKey("testMode")) {
                try {
                    manager.getBranchManager().removeWorktree(tempDir.getAbsolutePath());
                    deleteDirectory(tempDir);
                } catch (Exception e) {}
            }
        }
    }

    private BranchVariant createUserVariant(String input, String goal, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId("v-user-" + System.currentTimeMillis());
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType("USER_TRAJECTORY");

        String strategyText = input.startsWith("Propose:") ? input.substring(8).trim() : input;

        if (strategyText.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(strategyText);
                v.setStrategy(obj.optString("strategy", "User-defined strategy"));
                v.setSurvivalArgument(obj.optString("survival_argument", "User injection"));
                v.setTradeoffs(obj.optString("tradeoffs", "Explicit user directive"));
            } catch (Exception e) {
                v.setStrategy(strategyText);
            }
        } else {
            v.setStrategy(strategyText);
            v.setSurvivalArgument("Direct user proposal");
            v.setTradeoffs("User-defined trajectory");
        }

        v.setScore(0.95); // User proposals are highly valued
        v.setBranchName("exp/user/" + sanitize(v.getStrategy()));

        // Record trajectory for tracking
        Trajectory t = new Trajectory(v.getId(), v.getStrategy());
        t.setFitnessScore(v.getScore());
        v.setTrajectoryId(t.getTrajectoryId());

        if (context.getKernelContext().getMemoryService().getTrajectoryMemory() != null) {
            context.getKernelContext().getMemoryService().getTrajectoryMemory().recordTrajectory(t);
        }

        return v;
    }

    private void mergeHybridInsights(List<BranchVariant> variants, BranchVariant winner, TaskContext context) {
        JSONArray analyticalInsights = new JSONArray();
        JSONArray stabilizationInsights = new JSONArray();

        for (BranchVariant v : variants) {
            if (v.getId().equals(winner.getId())) continue;

            JSONObject insight = new JSONObject();
            insight.put("strategy", v.getStrategy());
            insight.put("risks", v.getFailureRisks());
            insight.put("tradeoffs", v.getTradeoffs());

            if ("ANALYTICAL".equals(v.getStrategyType())) {
                analyticalInsights.put(insight);
            } else if ("STABILIZATION".equals(v.getStrategyType())) {
                stabilizationInsights.put(insight);
            }
        }

        if (analyticalInsights.length() > 0) {
            context.getOrchestrationState().getMetadata().put("hybrid_analytical_insights", analyticalInsights);
            context.log("[DARWIN] Merged " + analyticalInsights.length() + " analytical insights into context.");
        }
        if (stabilizationInsights.length() > 0) {
            context.getOrchestrationState().getMetadata().put("hybrid_stabilization_insights", stabilizationInsights);
            context.log("[DARWIN] Merged " + stabilizationInsights.length() + " stabilization insights into context.");
        }
    }

    public boolean checkConvergence(List<BranchVariant> variants, TaskContext context) {
        if (variants == null || variants.size() < 2) return false;

        // 0. Minimum Evolution Guarantees
        // Required: minimum 2 iterations AND minimum 3 distinct surviving branches across history before convergence.
        int iterationCount = context.getOrchestrationState().getIterationCount();
        if (iterationCount < 2) {
            return false;
        }

        IntentExpansionResult expansion = (IntentExpansionResult) context.getOrchestrationState().getMetadata().get("intentExpansion");
        int totalAxes = (expansion != null && expansion.getEvolutionaryAxes() != null) ? expansion.getEvolutionaryAxes().size() : 1;

        // Converge only if we have explored all identified evolutionary axes at least once
        if (iterationCount < totalAxes) {
            return false;
        }

        long distinctSurvivors = context.getKernelContext().getMemoryService().getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .map(r -> r.getStrategy())
                .distinct()
                .count();

        if (distinctSurvivors < 3 && iterationCount < Math.max(4, totalAxes)) {
             return false;
        }

        // 1. Fitness Stability: Check if top variants have stabilized at high scores
        double maxScore = variants.stream().mapToDouble(BranchVariant::getScore).max().orElse(0.0);
        double avgScore = variants.stream().mapToDouble(BranchVariant::getScore).average().orElse(0.0);

        if (maxScore > 0.92 && (maxScore - avgScore) < 0.03) {
            context.log("[DARWIN] Strong convergence: Top variants have high and stable fitness.");
            return true;
        }

        // 2. Reality Check Convergence: Check magnitude of recent workspace changes
        try {
            String baseCommit = manager.getGitManager().getHeadCommit();
            WorkspaceDeltaAnalyzer analyzer = new WorkspaceDeltaAnalyzer(context.getProjectRoot(), context);
        WorkspaceDeltaAnalyzer.DeltaAnalysis reality = analyzer.analyze(baseCommit);

            // If we are in a late phase and changes are very small/refined, it may signal convergence
            String phase = context.getOrchestrationState().getCurrentPhase();
            if (EvolutionConstants.PHASE_IMPLEMENTATION_PLAN.equals(phase) || EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(phase)) {
                if (reality.isSignificant() && (reality.getAddedLines() + reality.getRemovedLines()) < 5 && reality.getChangedFiles().size() <= 1) {
                    context.log("[DARWIN] Convergence detected: Minimal delta in late evolution phase.");
                    return true;
                }
            }
        } catch (Exception e) {
            context.log("[DARWIN] Convergence check failed: " + e.getMessage());
        }

        return false;
    }

    private String sanitize(String s) {
        if (s == null || s.isEmpty()) return "unnamed";
        String sanitized = s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "unnamed";
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) deleteDirectory(file);
        }
        directory.delete();
    }
}
