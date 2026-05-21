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

            boolean isStepMode = context.getOrchestrator().getAiChat() != null &&
                               context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
                               context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode();

            if (!context.isAutoApprove() && isStepMode) {
                context.log("[KERNEL] Darwin Evolution: Pausing for intent interpretation review.");
                String userResponse = context.requestInput("Intent interpretation complete. State: " + expansion.getState() + ". Review and select a hypothesis to proceed, or reject to refine.").get();

                if ("Force Solution".equalsIgnoreCase(userResponse)) {
                    context.log("[KERNEL] Force Solution requested. Enabling auto-approval for the rest of this session.");
                    context.setAutoApprove(true);
                    // Use dominant intent as is and proceed
                } else if ("No".equalsIgnoreCase(userResponse) || "Reject".equalsIgnoreCase(userResponse) || "Rejected".equalsIgnoreCase(userResponse)) {
                    manager.recordRejection(goal, "User rejected intent interpretation.");
                    manager.transition(SystemState.FAILED, context);
                    return manager.failedResult();
                }

                String selectedHypothesisId = null;
                if (userResponse.startsWith("Select ")) {
                    selectedHypothesisId = userResponse.substring(7).trim();
                } else if (userResponse.startsWith("Approve variant ")) {
                    selectedHypothesisId = userResponse.substring(16).trim();
                }

                if (selectedHypothesisId != null) {
                    context.log("[KERNEL] User selected hypothesis: " + selectedHypothesisId);
                    String finalId = selectedHypothesisId;
                    expansion.getHypotheses().stream()
                        .filter(h -> h.getId().equals(finalId))
                        .findFirst()
                        .ifPresent(h -> {
                            expansion.setDominantIntent(h.getDescription());
                            expansion.setDominantConfidence(1.0);
                        });
                }
            }

            ClarificationPlanner planner = manager.getClarificationPlanner();
            ClarificationPlanner.Strategy strategy = planner.determineStrategy(expansion, context);
            context.log("[KERNEL] Clarification Strategy: " + strategy);

            // FAST FORWARD: Skip discovery phases for simple/atomic goals
            AtomicIntentAnalysis atomicAnalysis = (AtomicIntentAnalysis) state.getMetadata().get("atomicAnalysis");
            if (atomicAnalysis != null && atomicAnalysis.getConfidence() > 0.8 && !atomicAnalysis.isMultiStep()) {
                context.log("[KERNEL] Simple goal detected. Fast-forwarding to implementation planning.");
                state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.IMPLEMENTATION_PLAN));
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                res.setDecision(SelfDevDecision.CONTINUE);
                return res;
            }

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
            // UNLESS we are in manual mode, where we want the user to review intent first.
            if (strategy != ClarificationPlanner.Strategy.AUTO_INFER || !context.isAutoApprove()) {
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

            // METADATA PERSISTENCE: Record trajectory analysis for ALL proposals
            for (BranchVariant v : variants) {
                TrajectoryAnalysisRecord tar = new TrajectoryAnalysisRecord();
                tar.setIterationId(iterId);
                tar.setBranchId(v.getId());
                tar.setStrategy(v.getStrategy());
                tar.setFitnessScore(v.getScore());
                context.getKernelContext().getMemoryService().saveTrajectoryAnalysis(tar);
            }
            context.getKernelContext().getMemoryService().flush();

            BehaviorProfile profile = context.getBehaviorProfile();

            manager.transition(SystemState.PLAN_LOCKED, context);

            BranchVariant selectedVariant = null;
            String manualId = null;
            if (profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED) || !context.isAutoApprove()) {
                while (true) {
                    manager.transition(SystemState.CLARIFYING, context);
                    context.log("[KERNEL] Darwin Evolution: Pausing for variant selection (Manual Mode).");

                    StringBuilder sb = new StringBuilder("Darwin generated " + variants.size() + " proposals for your review:\n");
                    for (BranchVariant v : variants) {
                        String status = (v.getActivationState() == BranchVariant.ActivationState.KEPT) ? " [KEPT]" : "";
                        sb.append(String.format("- [%s] %s (Predicted Score: %.2f)%s\n", v.getId(), v.getStrategy(), v.getScore(), status));
                    }
                    sb.append("\nSelect a variant to execute (e.g. 'Select v0'), Keep to save, or Reject to stop.");

                    String input = context.requestInput(sb.toString()).get();
                    if ("Force Solution".equalsIgnoreCase(input)) {
                        context.log("[KERNEL] Force Solution requested. Enabling auto-approval for the rest of this session.");
                        context.setAutoApprove(true);
                        break; // Exit loop, authority will decide
                    }

                    if (input == null || input.trim().isEmpty()) continue;

                    if (input.startsWith("Select ") || input.startsWith("Approve variant ")) {
                        manualId = input.startsWith("Select ") ? input.substring(7).trim() : input.substring(16).trim();
                        context.log("[KERNEL] User selected variant: " + manualId);
                        break;
                    } else if (input.startsWith("Keep variant ")) {
                        String keepId = input.substring(13).trim();
                        variants.stream().filter(v -> v.getId().equals(keepId)).findFirst().ifPresent(v -> {
                            v.setActivationState(BranchVariant.ActivationState.KEPT);
                            context.log("[KERNEL] Variant " + keepId + " marked as KEPT for final evaluation.");
                        });
                        // Continue loop
                    } else if (input.startsWith("Reject variant ")) {
                        String rejectedId = input.substring(15).trim();
                        context.log("[KERNEL] User rejected variant: " + rejectedId + ". Evolution stopped by user.");
                        manager.recordRejection(goal, "Darwin variant " + rejectedId + " rejected by user ('no way').");
                        EvaluationResult res = manager.failedResult();
                        res.setDecision(SelfDevDecision.STOP);
                        manager.transition(SystemState.FAILED, context);
                        return res;
                    } else if ("Rejected".equalsIgnoreCase(input)) {
                        manager.recordRejection(goal, "Darwin " + state.getCurrentPhase() + " proposals rejected by user.");
                        EvaluationResult res = manager.failedResult();
                        res.setDecision(SelfDevDecision.CONTINUE);
                        manager.transition(SystemState.FAILED, context);
                        return res;
                    } else if (input.startsWith("Propose:") || input.trim().startsWith("{")) {
                        context.log("[KERNEL] User injected a new solution proposal. Integrating as a first-class candidate.");
                        BranchVariant userVariant = createUserVariant(input, goal, context);
                        variants.add(userVariant);

                        // Metadata for selection: Ensure user variant is visible in the next prompt
                        context.log("[KERNEL] User variant " + userVariant.getId() + " added to the evolutionary pool.");
                        // Continue loop to allow selection of this or other variants
                    } else {
                        // High priority guidance text input
                        context.log("[KERNEL] User provided guidance: " + input + ". Refining intent and regenerating variants.");
                        goal = goal + " (Guidance: " + input + ")";
                        if (context.getOrchestrator().getSelfDevSession() != null) {
                             context.getOrchestrator().getSelfDevSession().setInitialRequest(goal);
                        }
                        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                        res.setSuccess(true);
                        res.setDecision(SelfDevDecision.CONTINUE);
                        return res; // Triggers loop back in Supervisor
                    }
                }
            }

            // SINGLE AUTHORITY DECISION CALL - Selection happens BEFORE execution
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
                        vObj.put("approved", v.getId().equals(finalWinnerId));
                        updatedVariants.put(vObj);
                    }
                    context.log("[APPROVED:" + finalWinnerId + "] [DARWIN_BRANCHES] " + updatedVariants.toString());
                }
            }

            if (selectedVariant == null || (selectedVariant.getActivationState() != BranchVariant.ActivationState.ACTIVE && selectedVariant.getScore() < 0.3)) {
                context.log("[KERNEL] Darwin Evolution: No viable winner selected or winner score too low.");
                manager.getGitManager().forceCheckout(originalBranch);
                manager.getGitManager().rollback();
                manager.transition(SystemState.FAILED, context);
                return manager.failedResult();
            }

            if (currentIterationModelImpl != null) {
                currentIterationModelImpl.setSurvivalArgument(selectedVariant.getSurvivalArgument());
                currentIterationModelImpl.setTradeoffs(selectedVariant.getTradeoffs());
                currentIterationModelImpl.setFailureRisks(selectedVariant.getFailureRisks());
                currentIterationModelImpl.setJustification(selectedVariant.getStrategy());
            }

            // EXECUTION PHASE - Only for the winner
            manager.getGitManager().forceCheckout(snapshotBranch);
            manager.transition(SystemState.EXECUTING, context);

            context.log("[KERNEL] Executing winner variant: " + selectedVariant.getId() + " (" + selectedVariant.getStrategy() + ")");
            // Provision branch for winner
            manager.getGitManager().createBranchFrom(originalBranch, selectedVariant.getBranchName());

            evaluateVariantParallel(selectedVariant, manager.getTaskPlanner(), context, baseCommit);

            // SYNONIMOUS WITH evaluateVariantsInternal in old flow:
            ResultSynthesizer synthesizer = new ResultSynthesizer();
            synthesizer.synthesize(List.of(selectedVariant), context);

            // HYBRID INSIGHT MERGING: Collect insights from non-winning analytical/stabilization branches
            mergeHybridInsights(variants, selectedVariant, context);

            // Convergence check
            if (checkConvergence(variants, context)) {
                context.log("[KERNEL] Convergence detected. Transitioning to final synthesis.");
                state.setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.FINAL_SYNTHESIS));
            }

            if (!selectedVariant.isSuccess()) {
                context.log("[KERNEL] Winner variant execution failed.");
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

                boolean isStepModeConfirmation = context.getOrchestrator().getAiChat() != null &&
                                               context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
                                               context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode();

                if (!isFinalPhase && !context.isAutoApprove() && isStepModeConfirmation) {
                    context.log("[KERNEL] Darwin Evolution: Phase " + completedPhase + " completed. Pausing for user confirmation before next phase: " + nextPhase);
                    try {
                        String userResponse = context.requestInput("Phase " + completedPhase + " completed successfully. Proceed to " + nextPhase + "? (Yes/No)").get();
                        if ("Force Solution".equalsIgnoreCase(userResponse)) {
                            context.log("[KERNEL] Force Solution requested. Enabling auto-approval for the rest of this session.");
                            context.setAutoApprove(true);
                        } else if ("No".equalsIgnoreCase(userResponse) || "Reject".equalsIgnoreCase(userResponse)) {
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

    private BranchVariant createUserVariant(String input, String goal, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId("v-user-" + System.currentTimeMillis());
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType("USER_PROPOSAL");

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

    private boolean checkConvergence(List<BranchVariant> variants, TaskContext context) {
        if (variants == null || variants.size() < 2) return false;

        // 1. Fitness Stability: Check if top variants have stabilized at high scores
        double maxScore = variants.stream().mapToDouble(BranchVariant::getScore).max().orElse(0.0);
        double avgScore = variants.stream().mapToDouble(BranchVariant::getScore).average().orElse(0.0);

        if (maxScore > 0.9 && (maxScore - avgScore) < 0.05) {
            context.log("[DARWIN] Strong convergence: Top variants have high and stable fitness.");
            return true;
        }

        // 2. Novelty Check: Check if generated variants are repetitive
        // (This would ideally compare against historical trajectories in TrajectoryMemory)

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
