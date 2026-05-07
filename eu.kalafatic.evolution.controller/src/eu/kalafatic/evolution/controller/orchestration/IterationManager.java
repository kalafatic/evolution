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
import eu.kalafatic.evolution.controller.agents.FinalResponseAgent;
import eu.kalafatic.evolution.controller.agents.GeneralAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.PlannerAgent;
import eu.kalafatic.evolution.controller.agents.ProposalConsolidatorAgent;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationManager;
import eu.kalafatic.evolution.controller.orchestration.intent.ConfirmedRequirements;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.selfdev.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * The Kernel Control Plane. Sole authority for state transitions and strategic orchestration.
 *
 * @evo:21:A reason=kernel-refactor-alignment
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

    private IIntentClassifier intentClassifier = new LlmIntentClassifier();
    private IPolicyEngine policyEngine = new RuleBasedPolicyEngine();
    private final AnalyticAgent analyticAgent;
    private final PlannerAgent strategicPlanner;
    private final FinalResponseAgent finalResponseAgent;
    private final eu.kalafatic.evolution.controller.agents.ValidatorAgent validator;
    private final eu.kalafatic.evolution.controller.agents.RepairAgent repairAgent;
    private final List<IAgent> availableAgents = new ArrayList<>();

    private Iteration currentIterationModel;

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

        availableAgents.addAll(AgentFactory.getAllAgents());
        analyticAgent = (AnalyticAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_ANALYTIC);
        strategicPlanner = (PlannerAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_PLANNER);
        finalResponseAgent = (FinalResponseAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_FINAL_RESPONSE);
        validator = (eu.kalafatic.evolution.controller.agents.ValidatorAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_VALIDATOR);
        repairAgent = (eu.kalafatic.evolution.controller.agents.RepairAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_REPAIR);

        // Ensure agents use the provided AiService
        intentClassifier.setAiService(aiService);
        analyticAgent.setAiService(aiService);
        strategicPlanner.setAiService(aiService);
        finalResponseAgent.setAiService(aiService);
        validator.setAiService(aiService);
        repairAgent.setAiService(aiService);
        darwinEngine.setAiService(aiService);
        taskExecutor.getOrchestrator().setAiService(aiService);
        availableAgents.forEach(a -> {
            if (a instanceof eu.kalafatic.evolution.controller.agents.BaseAiAgent) {
                ((eu.kalafatic.evolution.controller.agents.BaseAiAgent)a).setAiService(aiService);
            }
        });
    }

    /**
     * Entry point for a task request. Manages the full strategic lifecycle.
     */
    public OrchestratorResponse handle(TaskRequest taskRequest) throws Exception {
        transition(SystemState.INIT, context);
        String request = taskRequest.getPrompt();
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        try {
            context.setCurrentTaskName("Initialization");
            context.log("[KERNEL] Strategic Initialization: " + request);

            // 1. Manage Conversation State
            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getSessionId());
            state.addMessage("User: " + request);

            // 2. Mode Routing
            if (context.getPlatformMode() == null) {
                ModeRouter router = new ModeRouter();
                PlatformMode mode = router.route(request, context.getOrchestrator());
                context.setPlatformMode(mode);
                context.log("Platform Mode: " + mode.getType());
            }

            // --- FAST TRACK: SIMPLE_CHAT Mode Short-circuit ---
            // We bypass the orchestration loop if the mode is already identified as SIMPLE_CHAT.
            if (context.getPlatformMode().getType() == PlatformType.SIMPLE_CHAT) {
                context.log("Evo-Orchestrator-Mode: SIMPLE_CHAT detected. Bypassing orchestration loop.");
                transition(SystemState.EXECUTING, context);
                GeneralAgent chatAgent = (GeneralAgent) availableAgents.stream()
                        .filter(a -> a instanceof GeneralAgent)
                        .findFirst()
                        .orElse(new GeneralAgent());
                chatAgent.setAiService(aiService);
                String chatResponse = chatAgent.process(request, context, null);
                state.addMessage("Evo: " + chatResponse);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), state));
                response.setSummary(chatResponse);
                response.setContent(chatResponse);
                transition(SystemState.DONE, context);
                return response;
            }

            // --- Consolidated Kernel Intelligence Entry (IntentAnalyzer) ---
            // AnalyticAgent is the semantic authority for non-trivial requests.
            // --- Consolidated Kernel Intelligence Entry ---
            // AnalyticAgent is the single source of truth for intent, category, and clarification.
            JSONObject analysis = analyticAgent.analyze(request, context);
            context.log("[KERNEL] Analysis Result: " + analysis.toString());

            // 1. Intent/Policy Gate
            String policyResponse = policyEngine.evaluate(analysis, request, context);
            if (policyResponse != null) {
                response.setSummary(policyResponse);
                transition(SystemState.DONE, context);
                return response;
            }

            // 3. Strategic Planning & Execution
            transition(SystemState.ANALYZING, context);

            // --- Atomic Task Detection (Optimized Pipeline) ---
            if (isSimpleFileCreate(request)) {
                context.log("[KERNEL] Atomic file task detected. Generating deterministic plan.");
                List<Task> tasks = createAtomicFilePlan(request, context);
                context.getOrchestrator().getTasks().addAll(tasks);

                transition(SystemState.PLAN_LOCKED, context);

                boolean success = executeTasksWithRetries(tasks);

                transition(SystemState.VERIFYING, context);
                String path = tasks.get(0).getName().replaceFirst("(?i)^Write\\s+", "");
                String finalResponse = "WORK DONE: Created file " + path + ".\nFILES: [FILE:" + path + "]";
                response.setSummary(finalResponse);

                transition(success ? SystemState.DONE : SystemState.FAILED, context);
                return response;
            }

            // 2. Clarification Loop (Only for non-Darwin tasks)
            String analyzedRequest = analysis.optString("refinedPrompt", request);
            if (analysis.optBoolean("isAmbiguous", false) && !context.getOrchestrator().isDarwinMode() && !context.isAutoApprove()) {
                String question = analysis.optString("clarificationQuestion", "More details needed.");
            // 2. Intent Clarification Loop (Only for non-Darwin tasks)
            IntentAnalyzer intentParser = new IntentAnalyzer(aiService);
            IntentAnalysisResult deepAnalysis = intentParser.parseResult(analysis);

            // Map structuredIntent if available (from AnalyticAgent internal Deep Intent Analysis)
            if (analysis.has("structuredIntent")) {
                deepAnalysis = intentParser.parseResult(analysis.getJSONObject("structuredIntent"));
            }

            // --- Requirement Drift Detection ---
            ConfirmedRequirements frozen = state.getConfirmedRequirements();
            if (frozen != null && hasSignificantDrift(frozen, deepAnalysis)) {
                context.log("[KERNEL] Requirement drift detected (v" + frozen.getVersion() + "). New intent: " + deepAnalysis.getGoal());
                context.log("[AUDIT] Re-evaluating requirements due to significant drift.");
                // We don't null it here to preserve the version number for the next freeze
            }

            if (clarificationManager.shouldClarify(deepAnalysis) && !context.getOrchestrator().isDarwinMode() && !context.isAutoApprove()) {
                transition(SystemState.CLARIFYING, context);
                String question = clarificationManager.generateClarificationQuestion(deepAnalysis, context);
                clarificationManager.updateState(state, deepAnalysis, question);

                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), state));

                String clarification = context.requestInput(question).get();
                if (clarification != null && !clarification.isEmpty() && !clarification.equalsIgnoreCase("Rejected")) {
                    state.addClarification(clarification);
                    state.setRequirementMet(true);
                    context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), state));

                    // Recursive re-analysis with clarification
                    return handle(new TaskRequest(request + "\nClarification: " + clarification, taskRequest.getProjectRoot()));
                } else {
                    // Blocking: prevent code generation if clarification is empty or rejected
                    context.log("[KERNEL] Clarification refused or empty. Stopping generation.");
                    response.setSummary("Generation stopped: Clarification required but not provided.");
                    transition(SystemState.FAILED, context);
                    return response;
                }
            }

            // --- Requirement Freezing ---
            if (!context.getOrchestrator().isDarwinMode()) {
                freezeRequirements(state, deepAnalysis, context);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), state));
            }

            String analyzedRequest = analysis.optString("refinedPrompt", request);

            // Goal/Darwin Handoff
            if (context.getPlatformMode().getType() == PlatformType.DARWIN_MODE || context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE) {
                EvaluationResult res = runDarwin();
                response.setSummary("Darwin evolution completed. Result: " + (res.isSuccess() ? "SUCCESS" : "FAIL"));
                transition(SystemState.DONE, context);
                return response;
            }

            // 3. Strategic Planning (using already analyzed/clarified request)
            List<Task> tasks = strategicPlanner.plan(analyzedRequest, context);
            context.getOrchestrator().getTasks().addAll(tasks);
            transition(SystemState.PLAN_LOCKED, context);

            // Execution
            boolean success = executeTasksWithRetries(tasks);

            transition(SystemState.VERIFYING, context);
            String finalResponse = finalResponseAgent.generateFinalResponse(request, tasks, context);
            response.setSummary(finalResponse);

            transition(success ? SystemState.DONE : SystemState.FAILED, context);
            return response;

        } catch (Exception e) {
            transition(SystemState.FAILED, context);
            throw e;
        }
    }

    private void transition(SystemState to, TaskContext ctx) {
        TransitionToken token = new TransitionToken();
        SystemState current = ctx.getStateHolder().getState();
        ctx.log("[KERNEL] Transition: " + current + " -> " + to);
        ctx.getStateHolder().applyTransition(token, to);
        if (currentIterationModel != null) {
            currentIterationModel.setPhase(to == SystemState.DONE ? "LEARN" : to.name());
        }
    }

    public EvaluationResult runIteration(Iteration iteration) {
        this.currentIterationModel = iteration;
        PlatformMode mode = context.getPlatformMode();
        boolean darwinEnabled = (mode != null && (mode.getType() == PlatformType.DARWIN_MODE || mode.getType() == PlatformType.SELF_DEV_MODE))
                                || context.getOrchestrator().isDarwinMode();

        try {
            if (darwinEnabled && gitManager.isGitRepository()) {
                return runDarwin();
            } else {
                return runIterative();
            }
        } catch (Exception e) {
            context.log("[KERNEL] Iteration encountered an error: " + e.getMessage());
            EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            return result;
        }
    }

    private EvaluationResult runIterative() throws Exception {
        transition(SystemState.INIT, context);
        currentIterationModel.setStatus(IterationStatus.RUNNING);
        transition(SystemState.ANALYZING, context);

        String goal = context.getOrchestrator().getSelfDevSession() != null ?
                     context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";

        try {
            List<Task> tasks = taskPlanner.generateTasks(context, goal);
            transition(SystemState.PLAN_LOCKED, context);

            transition(SystemState.EXECUTING, context);
            boolean success = executeTasksWithRetries(tasks);

            transition(SystemState.VERIFYING, context);
            EvaluationResult result = evaluator.evaluate();
            currentIterationModel.setEvaluationResult(result);

            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                transition(SystemState.DONE, context);
                gitManager.commit("Self-Development Iteration " + currentIterationModel.getId());
                currentIterationModel.setStatus(IterationStatus.DONE);
            } else {
                transition(SystemState.FAILED, context);
                gitManager.rollback();
                currentIterationModel.setStatus(IterationStatus.FAILED);
            }
            return result;
        } catch (Exception e) {
            transition(SystemState.FAILED, context);
            gitManager.rollback();
            currentIterationModel.setStatus(IterationStatus.FAILED);
            throw e;
        }
    }

    private EvaluationResult runDarwin() throws Exception {
        currentIterationModel.setStatus(IterationStatus.RUNNING);
        transition(SystemState.INIT, context);

        String goal = context.getOrchestrator().getSelfDevSession() != null ?
                     context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Autonomous Improvement";

        gitManager.ensureInitialCommit();
        String originalBranch = gitManager.getCurrentBranch();
        String snapshotBranch = "snapshot/" + currentIterationModel.getId() + "-" + System.currentTimeMillis();

        try {
            gitManager.createBranch(snapshotBranch);
            transition(SystemState.ANALYZING, context);

            Evaluator.Evaluation initialEval = evaluator.evaluateWithSnapshot();
            StateSnapshot snapshot = initialEval.snapshot;
            Trajectory trajectory = computeTrajectory(snapshot);
            FailureMemory failureMemory = memoryService.getFailureMemory();

            transition(SystemState.MUTATING, context);
            List<BranchVariant> variants = darwinEngine.generateVariants(goal, snapshot, failureMemory, trajectory);

            if (!context.isAutoApprove()) {
                context.requestApproval("Darwin generated " + variants.size() + " variants. Review and approve to start evaluation.").get();
            }

            transition(SystemState.PLAN_LOCKED, context);
            gitManager.forceCheckout(snapshotBranch);
            transition(SystemState.EXECUTING, context);
            BranchVariant bestVariant = evaluateVariantsInternal(variants, taskPlanner, currentIterationModel);

            if (bestVariant == null || bestVariant.getScore() <= 0) {
                transition(SystemState.FAILED, context);
                currentIterationModel.setStatus(IterationStatus.FAILED);
                gitManager.forceCheckout(originalBranch);
                return failedResult();
            }

            transition(SystemState.EXECUTING, context);
            gitManager.forceCheckout(originalBranch);
            gitManager.merge(bestVariant.getBranchName());

            // Darwin variants are already executed in evaluateVariantsInternal
            transition(SystemState.VERIFYING, context);
            EvaluationResult result = evaluator.evaluate();
            currentIterationModel.setEvaluationResult(result);

            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                transition(SystemState.DONE, context);
                gitManager.commit("Self-Development Iteration " + currentIterationModel.getId());
                currentIterationModel.setStatus(IterationStatus.DONE);
            } else {
                transition(SystemState.FAILED, context);
                gitManager.rollback();
                currentIterationModel.setStatus(IterationStatus.FAILED);
            }
            return result;
        } catch (Exception e) {
            transition(SystemState.FAILED, context);
            gitManager.forceCheckout(originalBranch);
            gitManager.rollback();
            currentIterationModel.setStatus(IterationStatus.FAILED);
            throw e;
        }
    }

    private BranchVariant evaluateVariantsInternal(List<BranchVariant> variants, TaskPlanner planner, Iteration iteration) throws Exception {
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

            TaskExecutor variantExecutor = new TaskExecutor(variantContext);
            List<Task> tasks = planner.generateTasksFromVariant(variantContext, variant);

            IterationManager variantManager = KernelFactory.create(variantContext);
            boolean success = variantManager.executeTasksWithRetries(tasks);

            variant.setSuccess(success);
            Evaluator variantEvaluator = new Evaluator(tempDir, variantContext);
            EvaluationResult result = variantEvaluator.evaluate();

            variant.setSuccess(result.isSuccess());
            variant.setScore(calculateScore(result));
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


    private double calculateScore(EvaluationResult result) {
        if (result.isSuccess()) return 0.8 + (result.getTestPassRate() * 0.2);
        return result.getTestPassRate() * 0.5;
    }

    private EvaluationResult failedResult() {
        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        res.setSuccess(false);
        res.setDecision(SelfDevDecision.ROLLBACK);
        return res;
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

        // If it's identical to the existing one, don't update (avoid version churn)
        if (existing != null && existing.getHash().equals(Integer.toHexString(java.util.Objects.hash(result.getGoal(), result.getLanguage(), result.getFramework(), result.getConstraints(), result.getExpectedOutput())))) {
            return;
        }

        int version = existing != null ? existing.getVersion() + 1 : 1;
        ConfirmedRequirements frozen = new ConfirmedRequirements(
            result.getGoal(),
            result.getLanguage(),
            result.getFramework(),
            result.getConstraints(),
            result.getExpectedOutput(),
            version
        );
        state.setConfirmedRequirements(frozen);
        context.log("[KERNEL] Requirements Frozen (v" + version + "): " + frozen.getHash());
        context.log("[AUDIT] Frozen Requirements Goal: " + frozen.getGoal());
    }

    private boolean hasSignificantDrift(ConfirmedRequirements frozen, IntentAnalysisResult newAnalysis) {
        if (frozen == null) return false;

        // Simple heuristic for significant drift
        if (!frozen.getGoal().equalsIgnoreCase(newAnalysis.getGoal()) && !newAnalysis.getGoal().isEmpty()) {
            return true;
        }
        if (!frozen.getLanguage().equalsIgnoreCase(newAnalysis.getLanguage()) && !newAnalysis.getLanguage().isEmpty()) {
            return true;
        }
        if (!frozen.getFramework().equalsIgnoreCase(newAnalysis.getFramework()) && !newAnalysis.getFramework().isEmpty()) {
            return true;
        }

        return !newAnalysis.getContradictions().isEmpty();
    }

    private Trajectory computeTrajectory(StateSnapshot current) {
        return new Trajectory();
    }

    public void setIntentClassifier(IIntentClassifier intentClassifier) {
        this.intentClassifier = intentClassifier;
        this.intentClassifier.setAiService(aiService);
    }

    public void setPolicyEngine(IPolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    private boolean isSimpleFileCreate(String request) {
        if (request == null) return false;
        String lower = request.toLowerCase().trim();
        // Detect patterns: create file x.txt, write to file y.java, save content to z.xml
        return lower.matches("^(create|add|write|save)\\s+(file|content|to)\\s+.*$") &&
               !lower.contains("\n") && request.length() < 100;
    }

    private List<Task> createAtomicFilePlan(String request, TaskContext context) {
        List<Task> tasks = new ArrayList<>();
        String lower = request.toLowerCase().trim();
        String path = request.replaceFirst("(?i)^(create|add|write|save)\\s+(file|content|to|to\\s+file)\\s+", "").trim();

        // Clean up path if it ends with punctuation
        path = path.replaceAll("[.!?,]$", "");

        Task t = OrchestrationFactory.eINSTANCE.createTask();
        t.setId("atomic-task-1");
        t.setName("Write " + path);
        t.setDescription(request);
        t.setType("file");
        t.setApprovalRequired(false);
        tasks.add(t);
        return tasks;
    }

    /**
     * Centralized execution loop for tasks. Handles retries, verification, and diagnosis.
     */
    public boolean executeTasksWithRetries(List<Task> tasks) throws Exception {
        context.log("[KERNEL] Starting centralized execution loop for " + tasks.size() + " tasks.");

        for (Task task : tasks) {
            if (task.getStatus() == eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE) continue;

            context.setCurrentTaskName(task.getName());
            boolean success = false;
            int maxRetries = EvolutionConstants.MAX_TASK_RETRIES;

            for (int retry = 1; retry <= maxRetries; retry++) {
                context.checkPause();
                context.setCurrentIteration(retry);

                // 1. EXECUTE
                transition(SystemState.EXECUTING, context);
                task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.RUNNING);
                String result = taskExecutor.getOrchestrator().executeTask(task, context);

                // 2. VERIFY
                transition(SystemState.VERIFYING, context);
                task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.VERIFYING);

                ChangeUnit change = new ChangeUnit();
                change.setPatch(task.getResponse()); // Orchestrator sets response
                JSONObject evaluation = validator.evaluate(change, task.getName(), context);

                if (evaluation.optBoolean("success", false)) {
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
                    success = true;
                    break;
                } else {
                    context.log("[KERNEL] Verification failed for task: " + task.getName() + " (Attempt " + retry + ")");
                    task.setFeedback("Verification Failed: " + evaluation.optString("feedback"));

                    // 3. ANALYZE/DIAGNOSE
                    transition(SystemState.ANALYZING, context);
                    JSONObject diagnosis = analyticAgent.diagnose(result, evaluation.optString("feedback"), context);

                    if (retry < maxRetries) {
                        context.log("[KERNEL] Mutating strategy for retry...");
                        transition(SystemState.MUTATING, context);
                        // If AnalyticAgent suggests RepairAgent, we could switch logic here
                        // For now, we stay in the loop and retry with feedback
                    } else {
                        task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
                        success = false;
                    }
                }
            }

            if (!success) {
                context.log("[KERNEL] Task failed after " + maxRetries + " attempts: " + task.getName());
                return false;
            }
        }
        return true;
    }
}
