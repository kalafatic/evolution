package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.agents.StructureAgent;
import eu.kalafatic.evolution.controller.agents.CriticAgent;
import eu.kalafatic.evolution.controller.agents.FinalResponseAgent;
import eu.kalafatic.evolution.controller.agents.GeneralAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.PlannerAgent;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentClassifier;
import eu.kalafatic.evolution.controller.orchestration.intent.HybridAtomicIntentClassifier;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationManager;
import eu.kalafatic.evolution.controller.orchestration.intent.ConfirmedRequirements;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.attachments.AttachmentInjector;
import eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorResolver;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.evolution.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.workflow.StepModeController;
import eu.kalafatic.evolution.controller.workflow.WorkflowStatus;
import eu.kalafatic.evolution.controller.workflow.WorkflowStep;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * The Kernel Control Plane. Sole authority for state transitions and strategic orchestration.
 * Unified and refactored for architectural coherence.
 */
public class IterationManager {
    private static final ExecutorService variantExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    private final TaskContext context;
    private final AiService aiService;
    private final GitManager gitManager;
    private final TaskPlanner taskPlanner;
    private final TaskExecutor taskExecutor;
    private final Evaluator evaluator;
    private final DarwinEngine darwinEngine;
    private final IterationMemoryService memoryService;
    private final ClarificationManager clarificationManager = new ClarificationManager();
    private final IntentService intentService;

    private final AnalyticAgent analyticAgent;
    private final StructureAgent structureAgent;
    private final PlannerAgent strategicPlanner;
    private final CriticAgent criticAgent;
    private final FinalResponseAgent finalResponseAgent;
    private final eu.kalafatic.evolution.controller.agents.ValidatorAgent validator;
    private final eu.kalafatic.evolution.controller.agents.RepairAgent repairAgent;
    private final List<IAgent> availableAgents = new ArrayList<>();

    private Iteration currentIterationModel;

    public TaskContext getContext() { return context; }
    public AiService getAiService() { return aiService; }
    public GitManager getGitManager() { return gitManager; }
    public TaskPlanner getTaskPlanner() { return taskPlanner; }
    public TaskExecutor getTaskExecutor() { return taskExecutor; }
    public Evaluator getEvaluator() { return evaluator; }
    public DarwinEngine getDarwinEngine() { return darwinEngine; }
    public IterationMemoryService getMemoryService() { return memoryService; }
    public AnalyticAgent getAnalyticAgent() { return analyticAgent; }
    public FinalResponseAgent getFinalResponseAgent() { return finalResponseAgent; }
    public Iteration getCurrentIterationModel() { return currentIterationModel; }

    public IterationManager(
            TaskContext context,
            AiService aiService,
            GitManager gitManager,
            TaskPlanner taskPlanner,
            TaskExecutor taskExecutor,
            Evaluator evaluator,
            DarwinEngine darwinEngine,
            IterationMemoryService memoryService) {
        this.context = context;
        this.aiService = aiService;
        this.gitManager = gitManager;
        this.taskPlanner = taskPlanner;
        this.taskExecutor = taskExecutor;
        this.evaluator = evaluator;
        this.darwinEngine = darwinEngine;
        this.memoryService = memoryService;
        this.intentService = new IntentService(aiService);

        availableAgents.addAll(AgentFactory.getAllAgents());
        analyticAgent = (AnalyticAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_ANALYTIC);
        structureAgent = (StructureAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_STRUCTURE);
        strategicPlanner = (PlannerAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_PLANNER);
        criticAgent = (CriticAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_CRITIC);
        finalResponseAgent = (FinalResponseAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_FINAL_RESPONSE);
        validator = (eu.kalafatic.evolution.controller.agents.ValidatorAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_VALIDATOR);
        repairAgent = (eu.kalafatic.evolution.controller.agents.RepairAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_REPAIR);

        // Inject AiService into agents
        availableAgents.forEach(a -> {
            if (a instanceof eu.kalafatic.evolution.controller.agents.BaseAiAgent) {
                ((eu.kalafatic.evolution.controller.agents.BaseAiAgent)a).setAiService(aiService);
            }
        });
        darwinEngine.setAiService(aiService);
        taskExecutor.getOrchestrator().setAiService(aiService);
    }

    public OrchestratorResponse handle(TaskRequest taskRequest) throws Exception {
        transition(SystemState.INIT, context);
        String request = taskRequest.getPrompt();
        OrchestrationState state = context.getOrchestrationState();
        state.setRawInput(request);

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        BehaviorProfile profile = context.getBehaviorProfile();
        ModeRouter router = new ModeRouter();

        try {
            context.getOrchestrator().getTasks().clear();
            context.setCurrentTaskName("Initialization");
            context.log("[KERNEL] Strategic Initialization: " + request);

            ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
            convState.addMessage("User: " + request);

            // 1. DISCOVERY phase (Repository-First Reasoning)
            if (!profile.hasTrait(BehaviorTrait.REASONING_ATOMIC)) {
                if (gitManager.isGitRepository()) {
                    transition(SystemState.ANALYZING, context);
                    context.log("[KERNEL] Discovery: Inspecting repository structure.");
                    String projectStructure = structureAgent.process("Provide a concise summary of the project structure and technology stack.", context, null);
                    state.getMetadata().put("projectStructure", projectStructure);
                    context.getOrchestrationState().addDiagnostic("[OrchestrationTrace] Discovery complete. Repository-aware context initialized.");
                }
            }

            // 2. ANALYZING stage & Git Synchronization
            transition(SystemState.ANALYZING, context);
            if (gitManager.isGitRepository()) {
                gitManager.ensureInitialCommit();
            }

            // Mode Routing
            if (context.getPlatformMode() == null) {
                PlatformMode mode = router.route(request, context.getOrchestrator());
                context.setPlatformMode(mode);
                context.log("Platform Mode: " + mode.getType());

                eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                    new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                        eu.kalafatic.evolution.controller.workflow.RuntimeEventType.MODE_CHANGED,
                        context.getSessionId(), "Kernel", mode.getType().toString()));
            }

            // Unified Intent Analysis
            transition(SystemState.ANALYZING, context);
            context.log("[KERNEL] Performing repository-grounded intent analysis.");
            intentService.analyze(request, context);
            AtomicIntentAnalysis atomicAnalysis = (AtomicIntentAnalysis) state.getMetadata().get("atomicAnalysis");

            // Flow Resolution & Execution
            IOrchestrationFlow flow = resolveFlow(router, atomicAnalysis);
            transition(SystemState.EXECUTING, context);
            OrchestratorResponse result = flow.execute(request, context);
            transition(SystemState.DONE, context);
            return result;

        } catch (Exception e) {
            state.addDiagnostic("Critical error: " + e.getMessage());
            transition(SystemState.FAILED, context);
            throw e;
        }
    }

    public void checkStep(String entityId, String type, String description) throws Exception {
        if (context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode()) {

            WorkflowStep step = new WorkflowStep("step-" + System.currentTimeMillis(), entityId, type);
            step.setDescription(description);
            WorkflowStatus result = StepModeController.getInstance().waitForStep(context.getSessionId(), step, context);
            if (result == WorkflowStatus.FAILED) {
                throw new Exception("Step failed or rejected by user: " + description);
            }
        }
    }

    public void transition(SystemState to, TaskContext ctx) {
        TransitionToken token = new TransitionToken();
        SystemState current = ctx.getStateHolder().getState();
        ctx.getStateHolder().applyTransition(token, to);

        if (currentIterationModel != null) {
            switch (to) {
                case DONE: currentIterationModel.setStatus(IterationStatus.DONE); break;
                case FAILED: currentIterationModel.setStatus(IterationStatus.FAILED); break;
                default: currentIterationModel.setStatus(IterationStatus.RUNNING); break;
            }
        }

        eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
            new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                eu.kalafatic.evolution.controller.workflow.RuntimeEventType.SUPERVISOR_STATUS_CHANGED,
                ctx.getSessionId(), "Kernel", to.toString()));

        ctx.log("[KERNEL] Transition: " + (current != null ? current : "NONE") + " -> " + to);
        ctx.getOrchestrationState().addDiagnostic("[OrchestrationTrace] Transition: " + (current != null ? current : "NONE") + " -> " + to);
    }

    public EvaluationResult runIteration(Iteration iteration) {
        this.currentIterationModel = iteration;
        BehaviorProfile profile = context.getBehaviorProfile();
        boolean darwinEnabled = profile.hasTrait(BehaviorTrait.REASONING_DARWIN_ITERATIVE);

        try {
            if (darwinEnabled && gitManager.isGitRepository()) {
                return new eu.kalafatic.evolution.controller.orchestration.flows.DarwinFlow(aiService, this).runDarwin(context);
            } else {
                return runIterative();
            }
        } catch (Exception e) {
            EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            return result;
        }
    }

    private EvaluationResult runIterative() throws Exception {
        transition(SystemState.INIT, context);
        transition(SystemState.ANALYZING, context);
        String goal = context.getOrchestrator().getSelfDevSession() != null ? context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";
        try {
            List<Task> tasks = iterativePlan(goal, context);
            transition(SystemState.PLAN_LOCKED, context);
            transition(SystemState.EXECUTING, context);
            boolean success = executeTasksWithRetries(tasks);
            transition(SystemState.VERIFYING, context);
            EvaluationResult result = evaluator.evaluate();
            currentIterationModel.setEvaluationResult(result);
            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                gitManager.commit("Self-Development Iteration " + currentIterationModel.getId());
                transition(SystemState.DONE, context);
            } else {
                gitManager.rollback();
                transition(SystemState.FAILED, context);
            }
            return result;
        } catch (Exception e) {
            gitManager.rollback();
            transition(SystemState.FAILED, context);
            throw e;
        }
    }

    private EvaluationResult runDarwin() throws Exception {
        transition(SystemState.INIT, context);
        String goal = context.getOrchestrator().getSelfDevSession() != null ? context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";

        OrchestrationState state = context.getOrchestrationState();
        if (state.getCurrentPhase() == null) {
            state.setCurrentPhase(EvolutionConstants.PHASE_INTENT_EXPANSION);
        }
        currentIterationModel.setPhase(state.getCurrentPhase());
        context.log("[KERNEL] Darwin Evolution Phase: " + state.getCurrentPhase());

        if (gitManager.isGitRepository()) {
            gitManager.ensureInitialCommit();
        }
        String originalBranch = gitManager.getCurrentBranch();
        String snapshotBranch = "snapshot/" + currentIterationModel.getId() + "-" + System.currentTimeMillis();
        try {
            gitManager.createBranch(snapshotBranch);
            transition(SystemState.ANALYZING, context);
            Evaluator.Evaluation initialEval = evaluator.evaluateWithSnapshot();
            StateSnapshot snapshot = initialEval.snapshot;
            Trajectory trajectory = new Trajectory();
            FailureMemory failureMemory = memoryService.getFailureMemory();
            transition(SystemState.MUTATING, context);
            List<BranchVariant> variants = darwinEngine.generateVariants(goal, snapshot, failureMemory, trajectory);

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

            checkStep("evolution_loop", "MUTATION", "Darwin variants generated. Review before approval.");

            BehaviorProfile profile = context.getBehaviorProfile();

            // MEDIATED mode behavior: If in mediated mode, Darwin is used for analysis/proposal generation only.
            if (profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED)) {
                context.log("[KERNEL] Darwin in MEDIATED mode (Phase: " + state.getCurrentPhase() + "): Stopping for user review/export of proposals.");
                String input = context.requestInput("Darwin generated " + variants.size() + " proposals for phase " + state.getCurrentPhase() + ". Review and select one to proceed, or reject to refine.").get();
                if ("Rejected".equalsIgnoreCase(input)) {
                    recordRejection(goal, "Darwin " + state.getCurrentPhase() + " proposals rejected by user.");
                    EvaluationResult res = failedResult();
                    res.setDecision(SelfDevDecision.CONTINUE);
                    currentIterationModel.setEvaluationResult(res);
                    transition(SystemState.FAILED, context);
                    return res;
                }

                // Advance phase in mediated mode too, but don't execute merge/tasks
                advanceEvolutionPhase(state);

                // In MEDIATED mode, we finish this iteration. Decision decides if we stop or continue to next phase iteration.
                transition(SystemState.DONE, context);
                EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                res.setSuccess(true);
                // Continue if we haven't reached synthesis, otherwise stop for export.
                res.setDecision(EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(currentIterationModel.getPhase()) ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);
                currentIterationModel.setEvaluationResult(res);
                return res;
            }

            if (!context.isAutoApprove()) {
                String input = context.requestInput("Darwin generated " + variants.size() + " variants. Review and approve to start evaluation.").get();
                if (input != null && input.startsWith("EDIT PROPOSAL")) {
                    updateVariantFromInput(variants, input);
                } else if ("Rejected".equalsIgnoreCase(input)) {
                    recordRejection(goal, "Darwin variants rejected by user.");
                    EvaluationResult res = failedResult();
                    res.setDecision(SelfDevDecision.CONTINUE);
                    currentIterationModel.setEvaluationResult(res);
                    transition(SystemState.FAILED, context);
                    return res;
                }
                Boolean approved = context.requestApproval("Darwin generated " + variants.size() + " variants.").get();
                if (approved != null && !approved) {
                    recordRejection(goal, "Darwin variants rejected by user (Approval denied).");
                    EvaluationResult res = failedResult();
                    res.setDecision(SelfDevDecision.CONTINUE);
                    currentIterationModel.setEvaluationResult(res);
                    transition(SystemState.FAILED, context);
                    return res;
                }
            }
            transition(SystemState.PLAN_LOCKED, context);
            gitManager.forceCheckout(snapshotBranch);
            transition(SystemState.EXECUTING, context);
            BranchVariant bestVariant = evaluateVariantsInternal(variants, taskPlanner, currentIterationModel);

            checkStep("evolution_loop", "BRANCH_COMPARISON", "Evaluation complete. Best variant selected: " + (bestVariant != null ? bestVariant.getId() : "None"));

            if (bestVariant == null || bestVariant.getScore() <= 0) {
                gitManager.forceCheckout(originalBranch);
                transition(SystemState.FAILED, context);
                return failedResult();
            }
            transition(SystemState.EXECUTING, context);
            gitManager.forceCheckout(originalBranch);
            gitManager.merge(bestVariant.getBranchName());
            transition(SystemState.VERIFYING, context);
            EvaluationResult result = evaluator.evaluate();
            currentIterationModel.setEvaluationResult(result);

            if (result.isSuccess()) {
                String completedPhase = state.getCurrentPhase();
                advanceEvolutionPhase(state);

                if (!EvolutionConstants.PHASE_FINAL_SYNTHESIS.equals(completedPhase)) {
                    result.setDecision(SelfDevDecision.CONTINUE);
                    context.log("[KERNEL] Phase " + completedPhase + " completed. Moving to " + state.getCurrentPhase());
                }

                if (result.getDecision() == SelfDevDecision.CONTINUE) {
                    gitManager.commit("Darwin Evolution Phase " + completedPhase + " (Iteration " + currentIterationModel.getId() + ")");
                    transition(SystemState.DONE, context);
                } else {
                    gitManager.commit("Darwin Evolution Finalized (Iteration " + currentIterationModel.getId() + ")");
                    transition(SystemState.DONE, context);
                }
            } else {
                gitManager.rollback();
                transition(SystemState.FAILED, context);
            }
            return result;
        } catch (Exception e) {
            gitManager.forceCheckout(originalBranch);
            gitManager.rollback();
            transition(SystemState.FAILED, context);
            throw e;
        }
    }

    public BranchVariant evaluateVariantsInternal(List<BranchVariant> variants, TaskPlanner planner, Iteration iteration) throws Exception {
        String baseBranch = gitManager.getCurrentBranch();
        for (BranchVariant variant : variants) {
            gitManager.createBranch(variant.getBranchName());
            gitManager.forceCheckout(baseBranch);
        }
        List<CompletableFuture<BranchVariant>> futures = variants.stream()
            .map(variant -> CompletableFuture.supplyAsync(() -> evaluateVariantParallel(variant, planner), variantExecutor))
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

    private BranchVariant evaluateVariantParallel(BranchVariant variant, TaskPlanner planner) {
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("evo-variant-" + variant.getId()).toFile();
            gitManager.createWorktree(variant.getBranchName(), tempDir.getAbsolutePath());
            TaskContext variantContext = new TaskContext(context.getOrchestrator(), tempDir);
            variantContext.setPlatformMode(context.getPlatformMode());
            variantContext.setAutoApprove(true);
            List<Task> tasks = planner.generateTasksFromVariant(variantContext, variant);
            IterationManager variantManager = KernelFactory.create(variantContext);
            boolean success = taskExecutor.executeTasks(tasks, aiService);
            variant.setSuccess(success);
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
                    gitManager.removeWorktree(tempDir.getAbsolutePath());
                    deleteDirectory(tempDir);
                } catch (Exception e) {}
            }
        }
    }

    public EvaluationResult failedResult() {
        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        res.setSuccess(false);
        res.setDecision(SelfDevDecision.ROLLBACK);
        return res;
    }

    public void recordRejection(String goal, String message) {
        IterationRecord record = new IterationRecord();
        int iterNum = 0;
        try {
            if (currentIterationModel != null) {
                iterNum = Integer.parseInt(currentIterationModel.getId().replace("iteration-", ""));
            }
        } catch (Exception e) {}
        record.setIteration(iterNum);
        record.setGoal(goal);
        record.setStrategy("Darwin Variant Selection");
        record.setResult("FAIL");
        record.setStatus("REJECTED");
        record.setErrorMessage(message);
        record.setTimestamp(System.currentTimeMillis());
        memoryService.saveRecord(record);
    }

    public IOrchestrationFlow resolveFlow(ModeRouter router, AtomicIntentAnalysis atomicAnalysis) {
        BehaviorProfile profile = context.getBehaviorProfile();
        OrchestrationState state = context.getOrchestrationState();

        if (profile.hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
            return new eu.kalafatic.evolution.controller.orchestration.flows.MediatedExportFlow(aiService, this);
        }

        // Atomic optimization
        boolean isImplementation = state.getTaskIntents() != null && state.getTaskIntents().contains(eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent.IMPLEMENTATION);
        boolean isDarwinMode = context.getOrchestrator().isDarwinMode();

        if (isDarwinMode && isImplementation && atomicAnalysis != null) {
            atomicAnalysis.setRequiresPlanning(true);
        }

        if (atomicAnalysis != null && atomicAnalysis.isAtomic() && atomicAnalysis.getConfidence() > 0.80 && !atomicAnalysis.isRequiresPlanning()) {
            return new eu.kalafatic.evolution.controller.orchestration.flows.AtomicFlow(aiService, this);
        }

        // Simple chat path
        if (profile.hasTrait(BehaviorTrait.REASONING_ATOMIC) && !profile.hasTrait(BehaviorTrait.WORKFLOW_SELF_DEV)) {
            return (IOrchestrationFlow) AgentFactory.getAgent(EvolutionConstants.AGENT_GENERAL);
        }

        return router.resolveFlow(context.getPlatformMode(), aiService, this);
    }

    public void advanceEvolutionPhase(OrchestrationState state) {
        String current = state.getCurrentPhase();
        if (EvolutionConstants.PHASE_INTENT_EXPANSION.equals(current)) {
            state.setCurrentPhase(EvolutionConstants.PHASE_ARCHITECTURE_VARIANTS);
        } else if (EvolutionConstants.PHASE_ARCHITECTURE_VARIANTS.equals(current)) {
            state.setCurrentPhase(EvolutionConstants.PHASE_SELECTION_REFINEMENT);
        } else if (EvolutionConstants.PHASE_SELECTION_REFINEMENT.equals(current)) {
            state.setCurrentPhase(EvolutionConstants.PHASE_IMPLEMENTATION_PLAN);
        } else if (EvolutionConstants.PHASE_IMPLEMENTATION_PLAN.equals(current)) {
            state.setCurrentPhase(EvolutionConstants.PHASE_FINAL_SYNTHESIS);
        }
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) deleteDirectory(file);
        }
        directory.delete();
    }

    private void freezeRequirements(ConversationState state, IntentAnalysisResult result, TaskContext context) {
        ConfirmedRequirements existing = state.getConfirmedRequirements();
        if (existing != null && existing.getHash().equals(Integer.toHexString(java.util.Objects.hash(result.getGoal(), result.getLanguage(), result.getFramework(), result.getConstraints(), result.getExpectedOutput())))) return;
        int version = existing != null ? existing.getVersion() + 1 : 1;
        ConfirmedRequirements frozen = new ConfirmedRequirements(result.getGoal(), result.getLanguage(), result.getFramework(), result.getConstraints(), result.getExpectedOutput(), version);
        state.setConfirmedRequirements(frozen);
    }

    public static boolean isSimpleFileCreate(String request) {
        AtomicIntentAnalysis analysis = HybridAtomicIntentClassifier.heuristicAnalyze(request);
        return analysis.isAtomic() && analysis.getConfidence() > 0.80 && !analysis.isRequiresPlanning();
    }

    public List<Task> createAtomicFilePlan(String request, AtomicIntentAnalysis analysis, TaskContext context) {
        List<Task> tasks = new ArrayList<>();
        String path = (analysis != null && analysis.getTargetArtifact() != null && !analysis.getTargetArtifact().isEmpty()) ?
                      analysis.getTargetArtifact() : "generated_file";
        Task t = OrchestrationFactory.eINSTANCE.createTask();
        t.setId("atomic-task-1");
        t.setName("Write " + path);
        t.setDescription(request);
        t.setType("file");
        t.setApprovalRequired(false);
        tasks.add(t);
        return tasks;
    }

    public List<Task> iterativePlan(String request, TaskContext context) throws Exception {
        context.getOrchestrationState().addDiagnostic("[OrchestrationTrace] Starting iterative planning.");
        List<Task> currentTasks = strategicPlanner.plan(request, context);
        for (int i = 1; i <= EvolutionConstants.MAX_PLANNING_ITERATIONS; i++) {
            JSONArray taskArray = new JSONArray();
            for (Task t : currentTasks) {
                JSONObject tObj = new JSONObject();
                tObj.put("id", t.getId());
                tObj.put("name", t.getName());
                tObj.put("description", t.getDescription());
                tObj.put("taskType", t.getType());
                taskArray.put(tObj);
            }
            JSONObject critiqueResult = criticAgent.critique(request, taskArray.toString(), context);
            if (critiqueResult.optBoolean("isCorrect", false) && critiqueResult.optDouble("qualityScore", 0.0) >= EvolutionConstants.PLANNING_QUALITY_THRESHOLD) {
                context.getOrchestrationState().addDiagnostic("[OrchestrationTrace] Plan approved by critic at iteration " + i);
                break;
            }
            context.getOrchestrationState().addDiagnostic("[OrchestrationTrace] Plan critique failure at iteration " + i + ": " + critiqueResult.optString("feedback"));
            if (i < EvolutionConstants.MAX_PLANNING_ITERATIONS) {
                String repairedPlanResponse = repairAgent.process("ORIGINAL REQUEST: " + request + "\\nCURRENT PLAN: " + taskArray.toString() + "\\nCRITIQUE: " + critiqueResult.toString(), context, null);
                JSONArray repairedJsonArray = JsonUtils.extractJsonArrayFlexible(repairedPlanResponse);
                if (repairedJsonArray != null) {
                    List<Task> repairedTasks = new ArrayList<>();
                    for (int j = 0; j < repairedJsonArray.length(); j++) {
                        JSONObject obj = repairedJsonArray.getJSONObject(j);
                        Task task = OrchestrationFactory.eINSTANCE.createTask();
                        task.setId(obj.optString("id", "rt" + j));
                        task.setName(obj.optString("name", "Task " + j));
                        task.setDescription(obj.optString("description", ""));
                        task.setType(obj.optString("taskType", "llm"));
                        repairedTasks.add(task);
                    }
                    currentTasks = repairedTasks;
                }
            }
        }
        return currentTasks;
    }

    public boolean executeTasksWithRetries(List<Task> tasks) throws Exception {
        OrchestrationState state = context.getOrchestrationState();
        for (Task task : tasks) {
            if (task.getStatus() == eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE) continue;
            boolean success = false;
            for (int retry = 1; retry <= EvolutionConstants.MAX_TASK_RETRIES; retry++) {
                state.addDiagnostic("[OrchestrationTrace] Executing task: " + task.getName() + " (Attempt " + retry + ")");
                context.checkPause();
                transition(SystemState.EXECUTING, context);
                task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.RUNNING);

                eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                    new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                        eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TASK_STARTED,
                        context.getSessionId(), "Kernel", task.getId()));

                checkStep(task.getId(), "TASK_EXECUTION", "Executing task: " + task.getName());

                String result = taskExecutor.getOrchestrator().executeTask(task, context);
                transition(SystemState.VERIFYING, context);
                task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.VERIFYING);
                ChangeUnit change = new ChangeUnit();
                change.setPatch(task.getResponse());

                checkStep(task.getId(), "PATCH_GENERATION", "Patch generated for task: " + task.getName());

                JSONObject evaluation = validator.evaluate(change, task.getName(), context);
                if (evaluation.optBoolean("success", false)) {
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " succeeded.");

                    eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                        new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                            eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TASK_COMPLETED,
                            context.getSessionId(), "Kernel", task.getId()));

                    success = true;
                    break;
                } else if (retry < EvolutionConstants.MAX_TASK_RETRIES) {
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " failed attempt " + retry + ". Diagnosing...");
                    transition(SystemState.ANALYZING, context);
                    analyticAgent.diagnose(result, evaluation.optString("feedback"), context);
                    transition(SystemState.MUTATING, context);
                } else {
                    state.addDiagnostic("[OrchestrationTrace] Task " + task.getName() + " failed after max retries.");
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
                }
            }
            if (!success) return false;
        }
        return true;
    }

    public void updateVariantFromInput(List<BranchVariant> variants, String input) {
        // ... (implementation same as before, simplified for brevity here if needed, but keeping it for completeness)
        try {
            String[] lines = input.split("\n");
            if (lines.length == 0) return;
            String firstLine = lines[0].trim();
            if (!firstLine.startsWith("EDIT PROPOSAL ")) return;
            int firstColon = firstLine.indexOf(":");
            if (firstColon == -1) return;
            String variantId = firstLine.substring("EDIT PROPOSAL ".length(), firstColon).trim();
            String strategy = firstLine.substring(firstColon + 1).trim();
            BranchVariant target = variants.stream().filter(v -> v.getId().equals(variantId)).findFirst().orElse(null);
            if (target != null) {
                target.setStrategy(strategy);
                context.log("[KERNEL] Updated variant " + variantId + " from manual edit.");
            }
        } catch (Exception e) {}
    }
}
