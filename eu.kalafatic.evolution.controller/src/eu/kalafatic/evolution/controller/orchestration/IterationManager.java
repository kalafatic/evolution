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

    private IIntentClassifier intentClassifier = new LlmIntentClassifier();
    private IPolicyEngine policyEngine = new RuleBasedPolicyEngine();
    private final AnalyticAgent analyticAgent;
    private final PlannerAgent strategicPlanner;
    private final FinalResponseAgent finalResponseAgent;
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

        // Ensure agents use the provided AiService
        intentClassifier.setAiService(aiService);
        analyticAgent.setAiService(aiService);
        strategicPlanner.setAiService(aiService);
        finalResponseAgent.setAiService(aiService);
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
            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());
            state.addMessage("User: " + request);

            // 1b. Fast Greeting Detection
            if (state.getGoal().isEmpty() && request.toLowerCase().matches("^\\s*(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\s*[!.]*\\s*$")
                && (context.getPlatformMode() == null || context.getPlatformMode().getType() != PlatformType.SIMPLE_CHAT)) {
                String greeting = "Hello! I'm Evo, your AI software engineer. How can I help you today?";
                state.addMessage("Evo: " + greeting);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getThreadId(), state));
                response.setSummary(greeting);
                response.setContent(greeting);
                transition(SystemState.DONE, context);
                return response;
            }

            // 2. Mode Routing
            if (context.getPlatformMode() == null) {
                ModeRouter router = new ModeRouter();
                PlatformMode mode = router.route(request, context.getOrchestrator());
                context.setPlatformMode(mode);
                context.log("Platform Mode: " + mode.getType());
            }

            // 3. Strategic Planning & Execution
            transition(SystemState.ANALYZING, context);

            // SIMPLE_CHAT Mode - handled after ANALYZING start to keep flow consistent
            if (context.getPlatformMode().getType() == PlatformType.SIMPLE_CHAT) {
                transition(SystemState.EXECUTING, context);
                GeneralAgent chatAgent = (GeneralAgent) availableAgents.stream()
                        .filter(a -> a instanceof GeneralAgent)
                        .findFirst()
                        .orElse(new GeneralAgent());
                chatAgent.setAiService(aiService);
                String chatResponse = chatAgent.process(request, context, null);
                state.addMessage("Evo: " + chatResponse);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getThreadId(), state));
                response.setSummary(chatResponse);
                response.setContent(chatResponse);
                transition(SystemState.DONE, context);
                return response;
            }

            // Intent Gate
            JSONObject classification = intentClassifier.classify(request, context);

            String policyResponse = policyEngine.evaluate(classification, request, context);
            if (policyResponse != null) {
                response.setSummary(policyResponse);
                transition(SystemState.DONE, context);
                return response;
            }

            // Goal/Darwin Handoff
            if (context.getPlatformMode().getType() == PlatformType.DARWIN_MODE || context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE) {
                EvaluationResult res = runDarwin();
                response.setSummary("Darwin evolution completed. Result: " + (res.isSuccess() ? "SUCCESS" : "FAIL"));
                transition(SystemState.DONE, context);
                return response;
            }

            // Strategic Planning
            String analyzedRequest = analyzeAndClarify(request, context);
            List<Task> tasks = strategicPlanner.plan(analyzedRequest, context);
            context.getOrchestrator().getTasks().addAll(tasks);
            transition(SystemState.PLAN_LOCKED, context);

            // Execution
            transition(SystemState.EXECUTING, context);
            boolean success = taskExecutor.executeTasks(tasks);

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

    public EvaluationResult runIteration(Iteration iteration) throws Exception {
        this.currentIterationModel = iteration;
        PlatformMode mode = context.getPlatformMode();
        boolean darwinEnabled = (mode != null && (mode.getType() == PlatformType.DARWIN_MODE || mode.getType() == PlatformType.SELF_DEV_MODE))
                                || context.getOrchestrator().isDarwinMode();

        if (darwinEnabled && gitManager.isGitRepository()) {
            return runDarwin();
        } else {
            return runIterative();
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
            boolean success = taskExecutor.executeTasks(tasks);

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

            List<BranchVariant> variants = darwinEngine.generateVariants(goal, snapshot, failureMemory, trajectory);

            if (!context.isAutoApprove()) {
                context.requestApproval("Darwin generated " + variants.size() + " variants. Review and approve to start evaluation.").get();
            }

            transition(SystemState.PLAN_LOCKED, context);
            gitManager.forceCheckout(snapshotBranch);
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

            TransitionToken token = new TransitionToken();
            variantContext.getStateHolder().applyTransition(token, SystemState.EXECUTING);

            boolean success = variantExecutor.executeTasks(tasks);

            variantContext.getStateHolder().applyTransition(token, SystemState.VERIFYING);
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

    private String analyzeAndClarify(String request, TaskContext context) throws Exception {
        JSONObject analysis = analyticAgent.analyze(request, context);
        if (analysis.optBoolean("isAmbiguous", false) && !context.getOrchestrator().isDarwinMode() && !context.isAutoApprove()) {
            String question = analysis.optString("clarificationQuestion", "More details needed.");
            String clarification = context.requestInput(question).get();
            if (clarification != null && !clarification.isEmpty()) {
                return analyzeAndClarify(request + "\nClarification: " + clarification, context);
            }
        }
        return analysis.optString("refinedPrompt", request);
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
}
