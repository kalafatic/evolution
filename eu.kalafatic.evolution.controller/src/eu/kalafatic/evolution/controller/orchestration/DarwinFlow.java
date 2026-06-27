package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.execution.ExecutionBudget;
import eu.kalafatic.evolution.controller.execution.ScheduledExecutionPlan;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.ISchedulingContract;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceDeltaAnalyzer;
import eu.kalafatic.evolution.controller.supervision.ActivationResolver;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.DecisionSnapshot;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.trajectory.ResultSynthesizer;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

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
    private final SessionContainer sessionContainer;

    public DarwinFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
        this.sessionContainer = SessionManager.getInstance().getOrCreateSession(manager.getContext().getSessionId());
        if (this.sessionContainer == null) {
            throw new IllegalStateException("DarwinFlow: sessionContainer is null for sessionId: " + manager.getContext().getSessionId());
        }
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        return manager.getDarwinEngine().orchestrateEvolution(new TaskRequest(request, context.getProjectRoot()), manager);
    }

    @Deprecated
    public List<BranchVariant> generateProposals(TaskContext context, GoalModel goal) throws Exception {
	return manager.getDarwinEngine().generateProposals(context, goal, manager);
    }

    @Deprecated
    public EvaluationResult executeWinner(TaskContext context, eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision, List<BranchVariant> variants, GoalModel goal) throws Exception {
	return manager.getDarwinEngine().executeWinner(context, decision, variants, goal, manager);
    }

    public VariantExecutionContext evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context, String baseCommit, eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure) {
        File tempDir = null;
        AuthorityController authority = context.getKernelContext().getAuthority();
        VariantExecutionContext variantExecContext = new VariantExecutionContext(variant.getId());
        boolean isMediated = context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);

        try {
            if (context.getMetadata().containsKey("testMode") || isMediated) {
                tempDir = context.getProjectRoot();
            } else {
                tempDir = Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
                // Ensure worktree is clean before starting
                try {
                    manager.getGitManager().removeWorktree(tempDir.getAbsolutePath());
                    manager.getGitManager().pruneWorktrees();
                } catch (Exception e) {}

                manager.getBranchManager().createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            }
            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
            variantContext.setSessionId(context.getSessionId());
            variantContext.setKernelContext(context.getKernelContext());
            variantContext.getMetadata().put("variantId", variant.getId());
            variantContext.getMetadata().put("variantExecContext", variantExecContext);
            variantContext.setPlatformMode(context.getPlatformMode());
            variantContext.setAutoApprove(true);
            variantContext.setAiService(aiService);

            // PROPAGATE LISTENERS: Ensure sub-task execution logs are visible in the UI
            context.getLogListeners().forEach(variantContext::addLogListener);
            context.getApprovalListeners().forEach(variantContext::addApprovalListener);
            context.getInputListeners().forEach(variantContext::addInputListener);

            List<Task> tasks = planner.generateTasksFromVariant(variantContext, variant);
            context.log("[DARWIN] Generated " + tasks.size() + " tasks for variant: " + variant.getId());
            IterationManager variantManager = KernelFactory.create(variantContext, sessionContainer, aiService);

            boolean success = true;
            manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.EXECUTING, context);

            if (context.getMetadata().containsKey("testMode") || isMediated) {
                variant.setSuccess(true);
                variant.setScore(0.95);

                // Mediated mode does NOT execute tasks that modify source code
                if (!isMediated) {
                    for (Task task : tasks) {
                        try {
                            variantManager.getTaskExecutor().getOrchestrator().executeTask(task, variantContext);
                        } catch (Exception e) {
                            context.log("[KERNEL] [TEST_MODE] Execution failed but continuing: " + e.getMessage());
                        }
                    }
                } else {
                    context.log("[DARWIN] Mediated Mode: Skipping task execution to prevent source modification.");
                }

                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.VERIFIED, context);
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING, context);

                variant.setMutationTrace(isMediated ? "Cognitive evolution in mediated mode" : "Mocked in test mode");
                return variantExecContext;
            }

            for (Task task : tasks) {
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
                    DecisionSnapshot intermediateDecision = resolver.resolve(variantContext.getOrchestrationState().getCurrentIterationId(), List.of(variant), sessionContainer.getSignalBus().getSignalsForVariant(variant.getId()), variantContext);

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

            EvaluationResult result = manager.getFitnessEngine().evaluate(tempDir, variantContext, pressure);
            variant.setSuccess(result.isSuccess());
            if (result.isSuccess()) {
                manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.SCORING, context);

                // CAPTURE IMPLEMENTATION: Update EvolutionNode with ACTUAL file contents after successful execution
                EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
                EvolutionNode node = tree.getNode(variant.getId());
                if (node != null) {
                    for (BranchVariant.Action action : variant.getActions()) {
                        if (("WRITE".equals(action.getOperation()) || "CREATE".equals(action.getOperation())) && action.getTarget() != null) {
                            File file = new File(tempDir, action.getTarget());
                            if (file.exists() && file.isFile()) {
                                try {
                                    String content = Files.readString(file.toPath());
                                    node.getCodeSnapshots().put(action.getTarget(), content);
                                    action.setImplementation(content); // Sync back to variant
                                } catch (Exception e) {
                                    context.log("[DARWIN] Failed to read implemented file: " + action.getTarget());
                                }
                            }
                        }
                    }
                }
            }

            GitTool deltaTool = new GitTool();
            try {
                variant.setMutationTrace(deltaTool.execute("diff " + baseCommit + " HEAD", tempDir, variantContext));
            } catch (Exception e) {
                context.log("[DARWIN] Failed to capture mutation trace: " + e.getMessage());
            }
            variant.setScore(result.isSuccess() ? 0.8 + (result.getTestPassRate() * 0.2) : result.getTestPassRate() * 0.5);

            return variantExecContext;
        } catch (Exception e) {
            context.log("[DARWIN] Parallel evaluation failed for variant " + variant.getId() + ": " + e.getMessage());
            variant.setSuccess(false);
            variant.setScore(0.0);
            variant.setErrorMessage(e.getMessage());
            manager.updateVariantLifecycle(List.of(variant), variant.getId(), BranchVariant.ActivationState.REJECTED, context);
            return variantExecContext;
        } finally {
            if (tempDir != null && !context.getMetadata().containsKey("testMode") && !isMediated) {
                try {
                    manager.getGitManager().removeWorktree(tempDir.getAbsolutePath());
                } catch (Exception e) {
                    context.log("[DARWIN] Worktree removal failed: " + e.getMessage());
                }
                try {
                    deleteDirectory(tempDir);
                } catch (Exception e) {
                    context.log("[DARWIN] Temporary directory deletion failed: " + e.getMessage());
                }
            }
        }
    }

    private BranchVariant createUserVariant(String input, GoalModel goal, TaskContext context) {
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

        v.setScore(0.95);
        v.setBranchName("exp/user/" + sanitize(v.getStrategy()));

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
        if (variants == null || variants.isEmpty()) return false;

        eu.kalafatic.evolution.controller.trajectory.StabilityAnalyzer analyzer = new eu.kalafatic.evolution.controller.trajectory.StabilityAnalyzer();

        IterationRecord lastWinner = context.getKernelContext().getMemoryService().getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastWinner != null && lastWinner.getBranchId() != null) {
             Trajectory trajectory = context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(lastWinner.getBranchId());
             if (trajectory != null) {
                 return analyzer.isConverged(trajectory, context);
             }
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
