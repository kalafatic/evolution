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
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
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
            ClarificationPlanner.Strategy strategy = planner.determineStrategy(expansion);
            context.log("[KERNEL] Intent Expansion Strategy: " + strategy);

            if (strategy == ClarificationPlanner.Strategy.CLARIFY_USER) {
                String clarificationRequest = planner.formatClarificationRequest(expansion);
                String userResponse = context.requestInput(clarificationRequest).get();
                if ("Rejected".equalsIgnoreCase(userResponse)) {
                    manager.recordRejection(goal, "User rejected clarification request.");
                    return manager.failedResult();
                }
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
            DecisionResolver decisionResolver = new DecisionResolver();

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

                decisionResolver.resolveWinner(iterId, variants, context, manualId);

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
                    decisionResolver.resolveWinner(iterId, variants, context);
                }
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

            if (finalDiff.isEmpty() && !selectedVariant.getActions().isEmpty()) {
                context.log("[KERNEL] WARNING: Reality divergence detected. Selected variant expected changes but Git is empty.");
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

        try {
            List<CompletableFuture<BranchVariant>> futures = variants.stream()
                .map(variant -> CompletableFuture.supplyAsync(() -> evaluateVariantParallel(variant, planner, context), variantExecutor))
                .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            context.log("[KERNEL] Variant evaluation error: " + e.getMessage());
        }

        DecisionResolver decisionResolver = new DecisionResolver();
        DecisionSnapshot decision = decisionResolver.resolveWinner(iteration.getId(), variants, context);

        ResultSynthesizer synthesizer = new ResultSynthesizer();
        synthesizer.synthesize(variants, context);

        BranchVariant bestVariant = null;
        if (decision.getSelectedVariantId() != null) {
            bestVariant = variants.stream()
                .filter(v -> v.getId().equals(decision.getSelectedVariantId()))
                .findFirst().orElse(null);
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

            // ARCHITECTURAL CHANGE: Dynamic Re-Evaluation Loop
            // Partial execution with observation after each task
            final File finalTempDir = tempDir;
            boolean success = variantManager.executeTasksWithRetries(tasks, () -> {
                try {
                    // Observe Git changes after each task
                    GitTool gitTool = new GitTool();
                    String diff = gitTool.execute("diff HEAD", finalTempDir, variantContext);
                    context.log("[DARWIN] Observed Git changes for variant " + variant.getId() + ": " + diff.length() + " chars");

                    // Update EMF with new evidence (semantic justification of the step)
                    if (variantManager.getCurrentIterationModel() != null) {
                        variantManager.getCurrentIterationModel().setJustification("Adaptive observation: Step completed with " + diff.length() + " chars changed.");
                    }

                    // Re-evaluate (In a full implementation, we might re-run DecisionResolver here)
                    // For now, we update the variant's trajectory based on the observation
                    context.getSemanticWorkspace().getTrajectoryMemory().recordLineagePattern("Variant " + variant.getId() + " step observation: " + (diff.isEmpty() ? "NO_CHANGE" : "PROGRESS"));
                } catch (Exception e) {
                    context.log("[DARWIN] Error during dynamic re-evaluation: " + e.getMessage());
                }
            });

            if (success) {
                variantManager.getGitManager().commit("Darwin Variant Execution: " + variant.getStrategy(), variantContext);
            }
            variant.setSuccess(success);

            IEvaluationContract evaluator = CapabilityRegistry.getInstance().getContractImplementation(IEvaluationContract.ID, IEvaluationContract.class);
            EvaluationResult result = evaluator.evaluate(tempDir, variantContext, manager.getEvaluator() != null ? manager.getEvaluator().getMavenTool() : null);
            variant.setSuccess(result.isSuccess());

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
