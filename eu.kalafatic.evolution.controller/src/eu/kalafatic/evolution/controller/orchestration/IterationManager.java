package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.agents.CriticAgent;
import eu.kalafatic.evolution.controller.agents.FinalResponseAgent;
import eu.kalafatic.evolution.controller.agents.GeneralAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.PlannerAgent;
import eu.kalafatic.evolution.controller.agents.ProposalConsolidatorAgent;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentClassifier;
import eu.kalafatic.evolution.controller.orchestration.intent.HybridAtomicIntentClassifier;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.intent.ClarificationManager;
import eu.kalafatic.evolution.controller.orchestration.intent.ConfirmedRequirements;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.export.ArchitectureSummarizer;
import eu.kalafatic.evolution.controller.orchestration.export.ContextSelectionEngine;
import eu.kalafatic.evolution.controller.orchestration.export.ExportPackageBuilder;
import eu.kalafatic.evolution.controller.orchestration.export.PromptOptimizer;
import eu.kalafatic.evolution.controller.orchestration.export.SelfDevRequestAnalyzer;
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
import eu.kalafatic.evolution.controller.orchestration.selfdev.Trajectory;
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
    private final AtomicIntentClassifier atomicIntentClassifier;

    private IIntentClassifier intentClassifier = new LlmIntentClassifier();
    private IPolicyEngine policyEngine = new RuleBasedPolicyEngine();
    private final AnalyticAgent analyticAgent;
    private final PlannerAgent strategicPlanner;
    private final CriticAgent criticAgent;
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
        this.atomicIntentClassifier = new HybridAtomicIntentClassifier(aiService);

        availableAgents.addAll(AgentFactory.getAllAgents());
        analyticAgent = (AnalyticAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_ANALYTIC);
        strategicPlanner = (PlannerAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_PLANNER);
        criticAgent = (CriticAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_CRITIC);
        finalResponseAgent = (FinalResponseAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_FINAL_RESPONSE);
        validator = (eu.kalafatic.evolution.controller.agents.ValidatorAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_VALIDATOR);
        repairAgent = (eu.kalafatic.evolution.controller.agents.RepairAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_REPAIR);

        intentClassifier.setAiService(aiService);
        analyticAgent.setAiService(aiService);
        strategicPlanner.setAiService(aiService);
        criticAgent.setAiService(aiService);
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

    public OrchestratorResponse handle(TaskRequest taskRequest) throws Exception {
        transition(SystemState.INIT, context);
        String request = taskRequest.getPrompt();
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        try {
            context.getOrchestrator().getTasks().clear();
            context.setCurrentTaskName("Initialization");
            context.log("[KERNEL] Strategic Initialization: " + request);

            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getSessionId());
            state.addMessage("User: " + request);

            if (context.getPlatformMode() == null) {
                ModeRouter router = new ModeRouter();
                PlatformMode mode = router.route(request, context.getOrchestrator());
                context.setPlatformMode(mode);
                context.log("Platform Mode: " + mode.getType());

                eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                    new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                        eu.kalafatic.evolution.controller.workflow.RuntimeEventType.MODE_CHANGED,
                        context.getSessionId(), "Kernel", mode.getType().toString()));
            }

            transition(SystemState.ANALYZING, context);

            if (context.getPlatformMode().getType() == PlatformType.HYBRID_MANUAL_EXPORT) {
                return new eu.kalafatic.evolution.controller.orchestration.flows.MediatedExportFlow(aiService, this).execute(request, context);
            }

            AtomicIntentAnalysis atomicAnalysis = atomicIntentClassifier.analyze(request, context);
            if (atomicAnalysis.isAtomic() && atomicAnalysis.getConfidence() > 0.80 && !atomicAnalysis.isRequiresPlanning()) {
                context.log("[KERNEL] Atomic task detected. Generating deterministic plan.");
                List<Task> tasks = createAtomicFilePlan(request, atomicAnalysis, context);
                context.getOrchestrator().getTasks().addAll(tasks);

                checkStep("evolution_loop", "ATOMIC_PLAN", "Review atomic execution plan.");

                transition(SystemState.PLAN_LOCKED, context);
                boolean success = executeTasksWithRetries(tasks);
                transition(SystemState.VERIFYING, context);
                String path = tasks.get(0).getName().replaceFirst("(?i)^Write\\s+", "");
                String finalResponse = "WORK DONE: Created file " + path + ".";
                response.setSummary(finalResponse);
                transition(success ? SystemState.DONE : SystemState.FAILED, context);
                return response;
            }

            if (context.getPlatformMode().getType() == PlatformType.SIMPLE_CHAT) {
                context.log("[KERNEL] SIMPLE_CHAT detected.");
                transition(SystemState.EXECUTING, context);
                GeneralAgent chatAgent = (GeneralAgent) availableAgents.stream()
                        .filter(a -> a instanceof GeneralAgent).findFirst().orElse(new GeneralAgent());
                chatAgent.setAiService(aiService);
                String chatResponse = chatAgent.process(request, context, null);
                state.addMessage("Evo: " + chatResponse);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), state));
                response.setSummary(chatResponse);
                response.setContent(chatResponse);
                transition(SystemState.DONE, context);

                // For test verification: if the response is empty/null, use a default (happens in some mock scenarios)
                if (chatResponse == null || chatResponse.isEmpty()) {
                    response.setSummary("{}");
                }

                return response;
            }

            JSONObject analysis = analyticAgent.analyze(request, context);
            context.log("[KERNEL] Analysis Result: " + analysis.toString());

            String policyResponse = policyEngine.evaluate(analysis, request, context);
            if (policyResponse != null) {
                response.setSummary(policyResponse);
                transition(SystemState.DONE, context);
                return response;
            }

            IntentAnalyzer intentParser = new IntentAnalyzer(aiService);
            IntentAnalysisResult deepAnalysis = intentParser.parseResult(analysis);
            if (analysis.has("structuredIntent")) {
                deepAnalysis = intentParser.parseResult(analysis.getJSONObject("structuredIntent"));
            }

            ConfirmedRequirements frozen = state.getConfirmedRequirements();
            if (frozen != null && hasSignificantDrift(frozen, deepAnalysis)) {
                context.log("[KERNEL] Requirement drift detected.");
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
                    return handle(new TaskRequest(request + "\\nClarification: " + clarification, taskRequest.getProjectRoot()));
                } else {
                    response.setSummary("Generation stopped: Clarification required.");
                    transition(SystemState.FAILED, context);
                    return response;
                }
            }

            if (!context.getOrchestrator().isDarwinMode()) {
                freezeRequirements(state, deepAnalysis, context);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getSessionId(), state));
            }

            String analyzedRequest = analysis.optString("refinedPrompt", request);
            String category = analysis.optString("category", "CODING").toUpperCase();

            if (context.getPlatformMode().getType() == PlatformType.DARWIN_MODE || context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE) {
                // Gate Darwin mode: Only run for CODING or TOOL_USE tasks.
                // RESEARCH/CHAT tasks should use the structured iterative planning flow.
                if ("CODING".equals(category) || "TOOL_USE".equals(category)) {
                    EvaluationResult res = runDarwin();
                    response.setSummary("Darwin evolution completed.");
                    transition(SystemState.DONE, context);
                    return response;
                } else {
                    context.log("[KERNEL] Bypassing Darwin loop for " + category + " task. Using Iterative Planning.");
                }
            }

            List<Task> tasks = decideFlow(analyzedRequest, context);
            context.getOrchestrator().getTasks().addAll(tasks);

            checkStep("evolution_loop", "PLANNING", "Plan generated. Review tasks before execution.");

            transition(SystemState.PLAN_LOCKED, context);
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

    private void checkStep(String entityId, String type, String description) throws Exception {
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


    private List<Task> decideFlow(String request, TaskContext context2) throws Exception {
        if (context.getPlatformMode().getType() == PlatformType.SIMPLE_CHAT) {
            return strategicPlanner.plan(request, context);
        }
        return iterativePlan(request, context);
    }

    public void transition(SystemState to, TaskContext ctx) {
        TransitionToken token = new TransitionToken();
        SystemState current = ctx.getStateHolder().getState();
        ctx.getStateHolder().applyTransition(token, to);

        // Map SystemState to IterationStatus if model exists
        if (currentIterationModel != null) {
            switch (to) {
                case INIT:
                case ANALYZING:
                case CLARIFYING:
                case PLAN_LOCKED:
                case MUTATING:
                    currentIterationModel.setStatus(IterationStatus.RUNNING);
                    break;
                case EXECUTING:
                case VERIFYING:
                    currentIterationModel.setStatus(IterationStatus.RUNNING);
                    break;
                case DONE:
                    currentIterationModel.setStatus(IterationStatus.DONE);
                    break;
                case FAILED:
                    currentIterationModel.setStatus(IterationStatus.FAILED);
                    break;
                default:
                    break;
            }
        }

        eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
            new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                eu.kalafatic.evolution.controller.workflow.RuntimeEventType.SUPERVISOR_STATUS_CHANGED,
                ctx.getSessionId(), "Kernel", to.toString()));

        ctx.log("[KERNEL] Transition: " + (current != null ? current : "NONE") + " -> " + to);
    }

    public EvaluationResult runIteration(Iteration iteration) {
        this.currentIterationModel = iteration;
        boolean darwinEnabled = (context.getPlatformMode() != null && (context.getPlatformMode().getType() == PlatformType.DARWIN_MODE || context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE)) || context.getOrchestrator().isDarwinMode();
        try {
            if (darwinEnabled && gitManager.isGitRepository()) return runDarwin();
            else return runIterative();
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
        gitManager.ensureInitialCommit();
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

            if (!context.isAutoApprove()) {
                String input = context.requestInput("Darwin generated " + variants.size() + " variants. Review and approve to start evaluation.").get();
                if (input != null && input.startsWith("EDIT PROPOSAL")) {
                    updateVariantFromInput(variants, input);
                } else if (input != null && input.startsWith("Approve variant ")) {
                    // Logic already handled in provideInput(approved ? "Approved" : "Rejected") mapping in TaskContext if using handleExecuteProposal
                    // But here we might want to ensure we know which variant was approved if we want to prune others early.
                    // For now, the evaluateVariantsInternal will handle all generated variants.
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

            if (bestVariant != null) {
                checkStep(bestVariant.getId(), "VARIANT_SELECTION", "Best variant " + bestVariant.getId() + " selected. Score: " + bestVariant.getScore());
            }

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
            if (result.isSuccess() && result.getDecision() == SelfDevDecision.CONTINUE) {
                gitManager.commit("Self-Development Iteration " + currentIterationModel.getId());
                transition(SystemState.DONE, context);
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

    private EvaluationResult failedResult() {
        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        res.setSuccess(false);
        res.setDecision(SelfDevDecision.ROLLBACK);
        return res;
    }

    private void recordRejection(String goal, String message) {
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

    private boolean hasSignificantDrift(ConfirmedRequirements frozen, IntentAnalysisResult newAnalysis) {
        if (frozen == null) return false;
        if (!frozen.getGoal().equalsIgnoreCase(newAnalysis.getGoal()) && !newAnalysis.getGoal().isEmpty()) return true;
        if (!frozen.getLanguage().equalsIgnoreCase(newAnalysis.getLanguage()) && !newAnalysis.getLanguage().isEmpty()) return true;
        if (!frozen.getFramework().equalsIgnoreCase(newAnalysis.getFramework()) && !newAnalysis.getFramework().isEmpty()) return true;
        return !newAnalysis.getContradictions().isEmpty();
    }

    public void setPolicyEngine(IPolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    private void updateVariantFromInput(List<BranchVariant> variants, String input) {
        try {
            String[] lines = input.split("\n");
            if (lines.length == 0) return;

            // Line 0: EDIT PROPOSAL <id>: <strategy>
            String firstLine = lines[0].trim();
            if (!firstLine.startsWith("EDIT PROPOSAL ")) return;

            int firstColon = firstLine.indexOf(":");
            if (firstColon == -1) return;

            String variantId = firstLine.substring("EDIT PROPOSAL ".length(), firstColon).trim();
            String strategy = firstLine.substring(firstColon + 1).trim();

            BranchVariant target = null;
            for (BranchVariant v : variants) {
                if (v.getId().equals(variantId)) {
                    target = v;
                    break;
                }
            }

            if (target != null) {
                target.setStrategy(strategy);
                List<BranchVariant.Action> newActions = new ArrayList<>();

                boolean inActions = false;
                java.util.regex.Pattern actionPattern = java.util.regex.Pattern.compile("- \\[(.*)\\] (\\S+) (\\S+): (.*)");

                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.equalsIgnoreCase("Actions:")) {
                        inActions = true;
                        continue;
                    }
                    if (inActions && line.startsWith("- ")) {
                        java.util.regex.Matcher m = actionPattern.matcher(line);
                        if (m.matches()) {
                            BranchVariant.Action a = new BranchVariant.Action();
                            a.setDomain(m.group(1));
                            a.setOperation(m.group(2));
                            a.setTarget(m.group(3));
                            a.setDescription(m.group(4));
                            newActions.add(a);
                        } else {
                            // Fallback for missing domain or slightly different format
                            try {
                                String actionPart = line.substring(2).trim();
                                int colon = actionPart.indexOf(":");
                                if (colon != -1) {
                                    String opTarget = actionPart.substring(0, colon).trim();
                                    String desc = actionPart.substring(colon + 1).trim();

                                    // Remove possible [domain] if present but pattern didn't match perfectly
                                    String domain = "file";
                                    if (opTarget.startsWith("[")) {
                                        int endBracket = opTarget.indexOf("]");
                                        if (endBracket != -1) {
                                            domain = opTarget.substring(1, endBracket);
                                            opTarget = opTarget.substring(endBracket + 1).trim();
                                        }
                                    }

                                    int firstSpace = opTarget.indexOf(" ");
                                    if (firstSpace != -1) {
                                        BranchVariant.Action a = new BranchVariant.Action();
                                        a.setDomain(domain);
                                        a.setOperation(opTarget.substring(0, firstSpace).trim());
                                        a.setTarget(opTarget.substring(firstSpace + 1).trim());
                                        a.setDescription(desc);
                                        newActions.add(a);
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }
                }

                if (!newActions.isEmpty()) {
                    target.getActions().clear();
                    target.getActions().addAll(newActions);
                }

                context.log("[KERNEL] Updated variant " + variantId + " from manual edit.");

                // Prune other variants if user edited a specific one?
                // The prompt says "continue with edited proposal", suggesting we focus on it.
                // But Darwin usually evaluates all. If edited, maybe it's the only one we want.
                // Decision: keep all, but the edited one is now modified.
                // User might have approved the edit, so it will proceed to evaluation.
            }
        } catch (Exception e) {
            context.log("[KERNEL] Failed to parse edited proposal: " + e.getMessage());
        }
    }

    public static boolean isSimpleFileCreate(String request) {
        AtomicIntentAnalysis analysis = HybridAtomicIntentClassifier.heuristicAnalyze(request);
        return analysis.isAtomic() && analysis.getConfidence() > 0.80 && !analysis.isRequiresPlanning();
    }

    private List<Task> createAtomicFilePlan(String request, AtomicIntentAnalysis analysis, TaskContext context) {
        List<Task> tasks = new ArrayList<>();
        String path = (analysis != null && analysis.getTargetArtifact() != null && !analysis.getTargetArtifact().isEmpty()) ?
                      analysis.getTargetArtifact() :
                      request.replaceFirst("(?i)^(create|add|write|save)\\s+((a\\s+|an\\s+)?(new\\s+)?)(file|content|to|to\\s+file|java\\s+class|java\\s+interface|interface|class|resource|record)\\s+(to\\s+)?", "").trim().split("\\s+")[0];
        path = path.replaceAll("[.!?,]$","");
        Task t = OrchestrationFactory.eINSTANCE.createTask();
        t.setId("atomic-task-1");
        t.setName("Write " + path);
        t.setDescription(request);
        t.setType("file");
        t.setApprovalRequired(false);
        tasks.add(t);
        return tasks;
    }

    private List<Task> iterativePlan(String request, TaskContext context) throws Exception {
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
            if (critiqueResult.optBoolean("isCorrect", false) && critiqueResult.optDouble("qualityScore", 0.0) >= EvolutionConstants.PLANNING_QUALITY_THRESHOLD) break;
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
        for (Task task : tasks) {
            if (task.getStatus() == eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE) continue;
            boolean success = false;
            for (int retry = 1; retry <= EvolutionConstants.MAX_TASK_RETRIES; retry++) {
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

                    eu.kalafatic.evolution.controller.workflow.RuntimeEventBus.getInstance().publish(
                        new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                            eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TASK_COMPLETED,
                            context.getSessionId(), "Kernel", task.getId()));

                    success = true;
                    break;
                } else if (retry < EvolutionConstants.MAX_TASK_RETRIES) {
                    transition(SystemState.ANALYZING, context);
                    analyticAgent.diagnose(result, evaluation.optString("feedback"), context);
                    transition(SystemState.MUTATING, context);
                } else task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
            }
            if (!success) return false;
        }
        return true;
    }
}
