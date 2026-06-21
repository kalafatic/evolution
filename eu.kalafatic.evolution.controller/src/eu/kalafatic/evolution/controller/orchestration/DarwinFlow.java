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
        return manager.evolve(request, context);
    }

    public List<BranchVariant> generateProposals(TaskContext context, GoalModel goal) throws Exception {
        context.log("[DARWIN_FLOW] Entering generateProposals for goal: " + goal.getPrimaryAction());
        Iteration currentIterationModelImpl = manager.getCurrentIterationModel();
        String iterId = currentIterationModelImpl != null ? currentIterationModelImpl.getId() : "default";

        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        String originalBranch = null;
        String baseCommit = null;
        if (profile.requiresRepository() && manager.getGitManager().isGitRepository()) {
            originalBranch = manager.getGitManager().getCurrentBranch();
            baseCommit = manager.getGitManager().getHeadCommit();
        }

        context.log("[COGNITION] Discovering semantic trajectories to resolve goal: " + goal);
        EvolutionProgressPublisher.updateStage(context, EvolutionStage.ANALYZE_PARENT);

        Evaluator.Evaluation initialEval = manager.getEvaluator().evaluateWithSnapshot();
        StateSnapshot snapshot = initialEval.snapshot;

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
            trajectory = new Trajectory("traj-" + iterId, goal.getPrimaryAction());
            context.getSemanticWorkspace().getTrajectoryMemory().recordTrajectory(trajectory);
            context.log("[COGNITION] Starting new evolutionary lineage trajectory: " + trajectory.getTrajectoryId());

            // Initialize Root Node in EvolutionTree if empty
            EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
            if (tree.getRootId() == null) {
                EvolutionNode root = new EvolutionNode();
                root.setId("root-" + iterId);
                root.setStrategy("ROOT: " + goal.getPrimaryAction());
                root.setSemanticPhilosophy("Initial evolutionary root");
                root.setIteration(0);
                root.setStatus("ROOT");
                tree.addNode(root);
                context.getKernelContext().getMemoryService().saveEvolutionTree();
            }
        }

        FailureMemory failureMemory = context.getKernelContext().getMemoryService().getFailureMemory();

        sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.MUTATING, context.getSessionId(), "DarwinFlow", goal.getPrimaryAction()));

        eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure = null;
        if (trajectory != null) {
            pressure = manager.getPressureEngine().analyze(trajectory, context);
            trajectory.recordPressure(pressure);
        }

        // ORCHESTRATOR-OWNED TOPOLOGY: Discovery of territorial dimensions before spawning
        if (trajectory != null && trajectory.getGeneration() == 0) {
            context.log("[DARWIN] Orchestrator: Mapping evolutionary territory...");
            var graph = context.getKernelContext().getMemoryService().getEvolutionGraph();
            graph.recordTerritory("ARCHITECTURE", "Implementation Dimensions");
            graph.recordTerritory("ARCHITECTURE", "Divergent Blueprints");
            graph.recordTerritory("STABILITY", "Reliability Pressure");
            graph.recordTerritory("EXTENSIBILITY", "Service Orientation");
        }

        EvolutionProgressPublisher.updateStage(context, EvolutionStage.GENERATE_BRANCH);
        List<BranchVariant> rawVariants = manager.getDarwinEngine().generateVariants(goal, snapshot, failureMemory, trajectory, pressure);

        sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.BRANCH_CREATED, context.getSessionId(), "DarwinFlow", rawVariants.size()));

        if (rawVariants.isEmpty()) {
            return Collections.emptyList();
        }

        if (profile.useImplementation()) {
            context.log("[DARWIN] Parallel Evaluation: Triggering implementation validation for " + rawVariants.size() + " variants.");
            String baseCommitFinal = baseCommit;
            eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressureFinal = pressure;

            rawVariants.parallelStream().forEach(variant -> {
                try {
                    evaluateVariantParallel(variant, manager.getTaskPlanner(), context, baseCommitFinal, pressureFinal);
                } catch (Exception e) {
                    context.log("[DARWIN] Parallel Evaluation Failed for " + variant.getId() + ": " + e.getMessage());
                }
            });
        } else {
            context.log("[DARWIN] Adaptive Kernel: Implementation validation disabled for current profile.");
            for (BranchVariant variant : rawVariants) {
                variant.setSuccess(true);
                variant.setScore(0.95);
            }
        }

        context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
            "darwin-mutation-" + System.currentTimeMillis(),
            "MUTATION",
            "DarwinEngine",
            List.of(goal.getPrimaryAction()),
            rawVariants.stream().map(v -> v.getId()).collect(Collectors.toList()),
            1.0,
            "Generated and evaluated " + rawVariants.size() + " variants."
        ));

        ISchedulingContract scheduler = sessionContainer.getCapabilityRegistry().getContractImplementation(ISchedulingContract.ID, ISchedulingContract.class);

        ScheduledExecutionPlan executionPlan;
        if (scheduler != null) {
            executionPlan = scheduler.schedule(rawVariants, context);
        } else {
            context.log("[DARWIN] Scheduler unavailable. Entering manual continuation mode.");
            executionPlan = new ScheduledExecutionPlan(rawVariants, "Manual fallback (No scheduler)", ExecutionBudget.defaultProfile());
        }
        List<BranchVariant> variants = executionPlan.getScheduledVariants();
        context.getOrchestrationState().getMetadata().put("executionPlan", executionPlan);

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

    public EvaluationResult executeWinner(TaskContext context, eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision, List<BranchVariant> variants, GoalModel goal) throws Exception {
        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        context.log("[DARWIN_FLOW] Entering executeWinner for variant: " + decision.getSelectedVariantId());
        VariantExecutionContext winningContext = null;
        String originalBranch = null;
        String baseCommit = null;
        if (profile.requiresRepository() && manager.getGitManager().isGitRepository()) {
            originalBranch = manager.getGitManager().getCurrentBranch();
            baseCommit = manager.getGitManager().getHeadCommit();
        }
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

        double winnerScore = decision.getAggregatedScores().getOrDefault(finalWinnerId, 0.0);
        sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.VARIANT_EVALUATED, context.getSessionId(), finalWinnerId, iterId, "DarwinFlow", winnerScore, System.currentTimeMillis()));

        if (selectedVariant == null || (selectedVariant.getActivationState() != BranchVariant.ActivationState.ACTIVE && selectedVariant.getScore() < 0.3)) {
            context.log("[KERNEL] Darwin Evolution: No viable winner selected or winner score too low.");
            return manager.failedResult();
        }

        EvolutionProgressPublisher.updateStage(context, EvolutionStage.SAVE_LINEAGE);

        if (currentIterationModelImpl != null) {
            currentIterationModelImpl.setSurvivalArgument(selectedVariant.getSurvivalArgument());
            currentIterationModelImpl.setTradeoffs(selectedVariant.getTradeoffs());
            currentIterationModelImpl.setFailureRisks(selectedVariant.getFailureRisks());
            currentIterationModelImpl.setJustification(selectedVariant.getStrategy());
        }

        boolean isExportOnly = context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY);

        boolean isTestMode = context.getMetadata().containsKey("testMode");
        try {
            if (profile.requiresRepository() && !isExportOnly && !isTestMode) {
                manager.getGitManager().createBranchFrom(originalBranch, snapshotBranch);
                manager.getGitManager().forceCheckout(snapshotBranch);
            }

            context.log("[KERNEL] Executing winner variant: " + selectedVariant.getId() + " (" + selectedVariant.getStrategy() + ")");
            if (profile.requiresRepository() && !isExportOnly && !isTestMode) {
                manager.getGitManager().createBranchFrom(originalBranch, selectedVariant.getBranchName());
            }

            // ADAPTIVE KERNEL: Capability-based implementation delegation
            boolean skipHeavyImplementation = !selectedVariant.isImplementationEnabled();

            if (skipHeavyImplementation) {
                context.log("[DARWIN] Adaptive Kernel: Implementation disabled for variant. Executing via General Agent.");

                eu.kalafatic.evolution.controller.agents.GeneralAgent generalAgent = (eu.kalafatic.evolution.controller.agents.GeneralAgent)
                    sessionContainer.getAgentRegistry().get(eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants.AGENT_GENERAL);

                // For minimal reasoning levels (chat), provide only the goal to prevent confusing the agent with internal Darwin strategies
                boolean isMinimal = selectedVariant.getReasoningLevel() == BranchVariant.ReasoningLevel.MINIMAL;
                String input = isMinimal ? goal.getPrimaryAction() : goal.getPrimaryAction() + "\nStrategy: " + selectedVariant.getStrategy();
                String finalResult = generalAgent.process(input, context, null);
                selectedVariant.setMutationTrace(finalResult);
                selectedVariant.setSuccess(true);
                selectedVariant.setScore(0.95);
            } else {
                // IMPORTANT: We MUST re-evaluate the winner in the target branch context to persist changes,
                // even if it was pre-evaluated in a temporary worktree.
                winningContext = evaluateVariantParallel(selectedVariant, manager.getTaskPlanner(), context, baseCommit, decision.getPressure());
            }

            // Merge discovered architectural facts from the winner back into the session in Mediated Mode
            if (isExportOnly && selectedVariant.getMediationCandidate() != null) {
                manager.mergeArchitecturalDiscovery(selectedVariant, context);
            }

            ResultSynthesizer synthesizer = new ResultSynthesizer();
            synthesizer.synthesize(List.of(selectedVariant), context);

            mergeHybridInsights(variants, selectedVariant, context);

            if (!selectedVariant.isSuccess()) {
                context.log("[KERNEL] Winner variant execution failed: " + selectedVariant.getId());
                if (!isExportOnly && !isTestMode) {
                    manager.getGitManager().forceCheckout(originalBranch);
                    manager.getGitManager().rollback(context);
                }
                return manager.failedResult();
            }

            if (profile.requiresRepository() && !isExportOnly && !isTestMode) {
                manager.getGitManager().forceCheckout(originalBranch);
                manager.getGitManager().merge(selectedVariant.getBranchName());
            } else if (isExportOnly) {
                context.log("[KERNEL] Applying cognitive winner: " + selectedVariant.getStrategy());
                context.getOrchestrationState().getMetadata().put("current_understanding", selectedVariant.getStrategy());
                context.getOrchestrationState().getMetadata().put("current_strategy", selectedVariant.getStrategyType());
                context.getOrchestrationState().getMetadata().put("current_reasoning_focus", selectedVariant.getReasoningFocus());
                context.getOrchestrationState().getMetadata().put("current_selected_files", selectedVariant.getSelectedFiles());
                context.getOrchestrationState().getMetadata().put("current_actions", selectedVariant.getActions());
                if (selectedVariant.getMediationCandidate() != null) {
                    context.getOrchestrationState().getMetadata().put("winningMediationCandidate", selectedVariant.getMediationCandidate());
                }
            }

            if (winningContext != null) {
                for (Task t : winningContext.getTasks()) {
                    if (!context.getOrchestrator().getTasks().contains(t)) {
                        context.getOrchestrator().getTasks().add(t);
                    }
                }
            }

            // LOGICAL SYNC: Ensure files from variant actions are always recorded in UI panel
            for (BranchVariant.Action action : selectedVariant.getActions()) {
                if (("WRITE".equals(action.getOperation()) || "CREATE".equals(action.getOperation())) && action.getTarget() != null) {
                    context.getFileChangeTracker().recordChange(action.getTarget(), FileChangeTracker.ChangeType.EDITED);
                } else if ("DELETE".equals(action.getOperation()) && action.getTarget() != null) {
                    context.getFileChangeTracker().recordChange(action.getTarget(), FileChangeTracker.ChangeType.REMOVED);
                }
            }

            if (isExportOnly) {
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(eu.kalafatic.evolution.model.orchestration.SelfDevDecision.CONTINUE);
                return res;
            }

            if (profile.shouldPerformRealityCheck()) {
                WorkspaceDeltaAnalyzer analyzer = new WorkspaceDeltaAnalyzer(context.getProjectRoot(), context);
                WorkspaceDeltaAnalyzer.DeltaAnalysis reality = analyzer.analyze(baseCommit);
                context.log("[KERNEL] Reality Check: Winner variant applied. Analysis: " + reality.toString());

                final BranchVariant finalSelectedVariant = selectedVariant;
                reality.getChangedFileMap().forEach((path, type) -> {
                    context.getFileChangeTracker().recordChange(path, type);
                    if (finalSelectedVariant != null) {
                        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
                        EvolutionNode node = tree.getNode(finalSelectedVariant.getId());
                        if (node != null) {
                            if (type == FileChangeTracker.ChangeType.NEW) node.getCreatedFiles().add(path);
                            else if (type == FileChangeTracker.ChangeType.REMOVED) node.getDeletedFiles().add(path);
                            else node.getModifiedFiles().add(path);
                        }
                    }
                });

                boolean isSignificant = reality.isSignificant();
                if (!isSignificant) {
                    context.log("[KERNEL] Reality Check WARNING: Winner variant resulted in NO physical changes.");
                }
                context.getOrchestrationState().getMetadata().put("lastRealityCheckSignificant", isSignificant);
            }

            EvaluationResult result = manager.getFitnessEngine().evaluate(context.getProjectRoot(), context, decision.getPressure());

            if (result.isSuccess() || selectedVariant != null) {
                String completedPhase = context.getOrchestrationState().getCurrentPhase();

                // SAVE LINEAGE: Persist ACTIVE winner and any KEPT survivors (Milestone Requirement)
                for (BranchVariant v : variants) {
                    if (v.getActivationState() == BranchVariant.ActivationState.ACTIVE || v.getActivationState() == BranchVariant.ActivationState.KEPT) {
                        IterationRecord record = new IterationRecord();
                        record.setIteration(context.getOrchestrationState().getIterationCount());
                        record.setGoal(goal.getPrimaryAction());
                        record.setStrategy(v.getStrategy());
                        record.setStrategyType(v.getStrategyType() != null ? v.getStrategyType().toString() : null);
                        record.setSemanticAnchor(v.getSemanticAnchor());
                        record.setMutationTrace(v.getMutationTrace());
                        record.setInheritedContext(v.getInheritedContext());
                        record.setRejectedSiblings(v.getRejectedSiblings());
                        record.setBranchId(v.getId());

                        if (v.getId().equals(selectedVariant.getId())) {
                            record.setResult(result.isSuccess() ? "SUCCESS" : "SUCCESS_WITH_BUILD_ERROR");
                            record.setActivationState("ACTIVE");
                        } else {
                            record.setResult("KEPT_FOR_DIVERSITY");
                            record.setActivationState("KEPT");
                        }

                        record.setTimestamp(System.currentTimeMillis());
                        context.getKernelContext().getMemoryService().saveRecord(record);

                        // Update EvolutionTree status
                        EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
                        EvolutionNode node = tree.getNode(v.getId());
                        if (node != null) {
                            node.setStatus(v.getActivationState().name());
                            if (v.getActivationState() == BranchVariant.ActivationState.ACTIVE) {
                                tree.setCurrentWinnerId(v.getId());
                                sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.WINNER_SELECTED, context.getSessionId(), v.getId(), v.getStrategy()));
                            }

                            sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.FITNESS_UPDATED, context.getSessionId(), v.getId(), v.getScore()));
                        }
                    }
                }
                context.getKernelContext().getMemoryService().saveEvolutionTree();
                sessionContainer.getEventBus().publish(new RuntimeEvent(RuntimeEventType.TREE_UPDATED, context.getSessionId(), "DarwinFlow", null));

                if (profile.requiresRepository() && !isExportOnly && !isTestMode && manager.getGitManager().isGitRepository()) {
                    manager.checkStep(selectedVariant.getId(), "GIT_COMMIT", "Committing evolutionary changes for phase: " + completedPhase);
                    manager.getGitManager().commit("Darwin Evolution Phase " + completedPhase, context);
                }

                result.setSuccess(true);
                return result;
            } else {
                if (!isExportOnly && !isTestMode && manager.getGitManager().isGitRepository()) {
                    manager.getGitManager().rollback();
                }
                return result;
            }
        } catch (Exception e) {
            context.log("[KERNEL] DarwinFlow.executeWinner failed: " + e.getMessage());
            if (profile.requiresRepository() && !isExportOnly && !isTestMode && manager.getGitManager().isGitRepository()) {
                try {
                    manager.getGitManager().forceCheckout(originalBranch);
                    manager.getGitManager().rollback(context);
                } catch (Exception ex) {
                    context.log("[KERNEL] Failed to rollback after error: " + ex.getMessage());
                }
            }
            throw e;
        }
    }

    private VariantExecutionContext evaluateVariantParallel(BranchVariant variant, eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner planner, TaskContext context, String baseCommit, eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure) {
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
