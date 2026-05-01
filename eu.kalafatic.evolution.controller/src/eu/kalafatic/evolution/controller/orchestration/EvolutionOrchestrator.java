package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.LogLevel;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.controller.log.LoggingService;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.agents.ArchitectAgent;
import eu.kalafatic.evolution.controller.agents.ConstraintAgent;
import eu.kalafatic.evolution.controller.agents.FinalResponseAgent;
import eu.kalafatic.evolution.controller.agents.FileAgent;
import eu.kalafatic.evolution.controller.agents.GeneralAgent;
import eu.kalafatic.evolution.controller.agents.GitAgent;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.agents.JavaDevAgent;
import eu.kalafatic.evolution.controller.agents.MavenAgent;
import eu.kalafatic.evolution.controller.agents.ObservabilityAgent;
import eu.kalafatic.evolution.controller.agents.PlannerAgent;
import eu.kalafatic.evolution.controller.agents.ProposalConsolidatorAgent;
import eu.kalafatic.evolution.controller.agents.QualityAgent;
import eu.kalafatic.evolution.controller.agents.RepairAgent;
import eu.kalafatic.evolution.controller.agents.ValidatorAgent;
import eu.kalafatic.evolution.controller.agents.StructureAgent;
import eu.kalafatic.evolution.controller.agents.TerminalAgent;
import eu.kalafatic.evolution.controller.agents.TesterAgent;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.tools.MavenTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Core Orchestrator implementation that manages the task lifecycle and execution.
 * It implements a sophisticated 6-phase PEV (Plan-Execute-Verify) loop for autonomous development.
 *
 * @evo.lastModified: 20:A
 * @evo.origin: self
 * @evo:20:A reason=architecture-documentation-sync
 */
public class EvolutionOrchestrator implements IOrchestrator {

    private static final int MAX_RETRIES = 3;
    private IIntentClassifier intentClassifier = new LlmIntentClassifier();
    private IPolicyEngine policyEngine = new RuleBasedPolicyEngine();
    private ContextAssistant contextAssistant = new ContextAssistant();
    private AnalyticAgent analyticAgent = new AnalyticAgent();
    private PlannerAgent planner = new PlannerAgent();
    private ValidatorAgent validator = new ValidatorAgent();
    private RepairAgent repairAgent = new RepairAgent();
    private ProposalConsolidatorAgent consolidator = new ProposalConsolidatorAgent();
    private FinalResponseAgent finalResponseAgent = new FinalResponseAgent();
    private final List<IAgent> availableAgents = new ArrayList<>();

    public EvolutionOrchestrator() {
        // Initialize default agents
        availableAgents.add(analyticAgent);
        availableAgents.add(new ArchitectAgent());
        availableAgents.add(new JavaDevAgent());
        availableAgents.add(new TesterAgent());
        availableAgents.add(validator);
        availableAgents.add(new GeneralAgent());
        availableAgents.add(new TerminalAgent());
        availableAgents.add(new FileAgent());
        availableAgents.add(new MavenAgent());
        availableAgents.add(new GitAgent());
        availableAgents.add(new StructureAgent());
        availableAgents.add(new WebSearchAgent());
        availableAgents.add(new QualityAgent());
        availableAgents.add(new ObservabilityAgent());
        availableAgents.add(repairAgent);
    }

    @Override
    @Deprecated
    public String execute(String request, TaskContext context) throws Exception {
        TaskRequest taskRequest = new TaskRequest(request, context.getProjectRoot());
        OrchestratorResponse response = handle(taskRequest, context);
        if (response.getResultType() == ResultType.ERROR) {
            throw new Exception(response.getContent());
        }
        return response.getSummary();
    }

    @Override
    @Deprecated
    public String executeTask(Task task, TaskContext context) throws Exception {
        context.setCurrentTaskName(task.getName());
        context.log("Evo-Orchestrator-" + task.getName() + ": Executing single task");
        boolean success = executeTaskWithRetries(task, context);
        if (!success) {
            task.setStatus(TaskStatus.FAILED);
            throw new Exception("Task failed after maximum retries: " + task.getName());
        }
        task.setStatus(TaskStatus.DONE);
        return task.getResponse();
    }

    @Override
    public OrchestratorResponse handle(TaskRequest taskRequest, TaskContext context) throws Exception {
        String request = taskRequest.getPrompt();
        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        try {
            context.setCurrentTaskName("Initialization");
            context.log("Evo-Orchestrator-Initialization: Starting request - " + request);

            // 1. Manage Conversation State
            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());
            state.addMessage("User: " + request);

            // 1b. Fast Greeting Detection (Bypass LLM for simple hi/hello)
            if (state.getGoal().isEmpty() && request.toLowerCase().matches("^\\s*(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\s*[!.]*\\s*$")) {
                String greeting = "Hello! I'm Evo, your AI software engineer. How can I help you today?";
                state.addMessage("Evo: " + greeting);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getThreadId(), state));
                response.setSummary(greeting);
                response.setContent(greeting);
                return response;
            }

            // 2. Context Assist Layer + Mode Routing (Internal Guard)
            if (context.getPlatformMode() == null) {
                ModeRouter router = new ModeRouter();
                PlatformMode fastMode = router.routeFast(request, context.getOrchestrator());

                ContextAssistResult assistResult;
                if (fastMode != null) {
                    context.log("Evo-Orchestrator-Mode: Fast-track mode detected: " + fastMode.getType());
                    assistResult = new ContextAssistResult();
                    assistResult.setMode(fastMode.getType());
                    assistResult.setConfidence(ConfidenceLevel.HIGH);
                } else {
                    assistResult = contextAssistant.analyze(request, context);
                }

                // SAFETY RULE: SELF_DEV_MODE confirmation (Always confirm explicitly)
                if (assistResult.getMode() == PlatformType.SELF_DEV_MODE) {
                    Boolean confirmed = true;
                    if (!context.isAutoApprove()) {
                        confirmed = context.requestApproval("You are asking the system to modify itself. Proceed?").get();
                    }
                    if (confirmed == null || !confirmed) {
                        assistResult.setMode(PlatformType.SIMPLE_CHAT);
                    } else {
                        // If confirmed, and no other missing info, we can treat it as HIGH confidence
                        if (assistResult.getMissingInfo() == null || assistResult.getMissingInfo().isEmpty()) {
                            assistResult.setConfidence(ConfidenceLevel.HIGH);
                        }
                    }
                }

                // Handle confidence and missing info
                if (assistResult.getConfidence() != ConfidenceLevel.HIGH) {
                    StringBuilder clarificationMsg = new StringBuilder();
                    boolean criticalMissing = false;

                    if (assistResult.getMissingInfo() != null && !assistResult.getMissingInfo().isEmpty()) {
                        clarificationMsg.append("To help you better, I need a bit more information:\n");
                        for (String info : assistResult.getMissingInfo()) {
                            clarificationMsg.append("- ").append(info).append("\n");
                        }
                        criticalMissing = true;
                    } else if (assistResult.getMode() == null) {
                        clarificationMsg.append("I'm not entirely sure how to help. Do you want quick chat, coding help, or an iterative solution search?");
                        criticalMissing = true;
                    }

                    if (assistResult.getSuggestedSteps() != null && !assistResult.getSuggestedSteps().isEmpty()) {
                        clarificationMsg.append("\nFor best results:\n");
                        for (String step : assistResult.getSuggestedSteps()) {
                            clarificationMsg.append("- ").append(step).append("\n");
                        }
                    }

                    // Only block if critical info is missing and not in SIMPLE_CHAT or AUTO_APPROVE mode
                    if (criticalMissing && clarificationMsg.length() > 0 && !context.isAutoApprove() && assistResult.getMode() != PlatformType.SIMPLE_CHAT) {
                        String finalClarification = consolidator.consolidate(clarificationMsg.toString(), context);
                        context.log("Evo-Orchestrator-Waiting: Waiting for user clarification...");
                        String clarification = context.requestInput(finalClarification).get();
                        if (clarification != null && !clarification.isEmpty()) {
                            context.log("Evo-Orchestrator-Clarification: User provided - " + clarification);
                            taskRequest.setPrompt(request + "\nClarification: " + clarification);
                            return handle(taskRequest, context);
                        }
                    } else if (clarificationMsg.length() > 0) {
                        // Non-blocking hints
                        context.log("Evo-Orchestrator: Guidance (non-blocking):\n" + clarificationMsg.toString());
                    }
                }

                PlatformMode mode = router.route(request, context.getOrchestrator(), assistResult);
                context.setPlatformMode(mode);
                context.log("Platform Mode: " + mode.getType() + " (Autonomy: " + mode.getAutonomyLevel() + ")");
            }

            // SIMPLE_CHAT Mode: Direct response bypass
            if (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.SIMPLE_CHAT) {
                context.log("Evo-Orchestrator-Mode: SIMPLE_CHAT detected. Bypassing orchestration loop.");
                GeneralAgent chatAgent = (GeneralAgent) availableAgents.stream()
                        .filter(a -> a instanceof GeneralAgent)
                        .findFirst()
                        .orElse(new GeneralAgent());
                String chatResponse = chatAgent.process(request, context, null);
                state.addMessage("Evo: " + chatResponse);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getThreadId(), state));

                response.setSummary(chatResponse);
                response.setContent(chatResponse);
                return response;
            }

            // 3. Intent Gate + Policy Engine
            context.log("Evo-Orchestrator-IntentGate: Classifying intent...");
            JSONObject classification;
            try {
                classification = intentClassifier.classify(request, context);
            } catch (Exception e) {
                context.log("Evo-Orchestrator-IntentGate: Classifier error, falling back to 'new'. Error: " + e.getMessage());
                classification = new JSONObject();
                classification.put("intent", "new");
            }
            String intent = classification.optString("intent", "unclear");
            context.log("Evo-Orchestrator-IntentGate: Intent - " + intent + " (conf: " + classification.optDouble("confidence") + ")");

            String policyResponse = policyEngine.evaluate(classification, request, context);
            context.log("Evo-Orchestrator-Policy: Decision - " + (policyResponse == null ? "PROCEED" : "HANDLE DIRECTLY/BLOCK"));
            if (policyResponse != null) {
                context.log("Evo-Orchestrator-Policy: Action blocked or handled directly.");
                state.addMessage("Evo: " + policyResponse);
                context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getThreadId(), state));
                response.setSummary(policyResponse);
                response.setContent(policyResponse);
                return response;
            }

            // 3. Goal Update
            String goalUpdate = classification.optString("goal_update");
            if ("new".equals(intent) && goalUpdate != null && !goalUpdate.isEmpty()) {
                state.setGoal(goalUpdate);
            } else if (state.getGoal().isEmpty()) {
                state.setGoal(request);
            }
            context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getThreadId(), state));

            // 4. Continuation Logic
            if ("continue".equals(intent) && !context.getOrchestrator().getTasks().isEmpty()) {
                boolean allDone = context.getOrchestrator().getTasks().stream().allMatch(t -> t.getStatus() == TaskStatus.DONE);
                if (!allDone) {
                    context.log("Evo-Orchestrator-Continuation: Context suggests continuing current tasks.");
                    // In a real system, we'd find the next PENDING task.
                    // For this simple version, we'll allow the loop below to handle it.
                }
            }

            // 5. Analytic Phase
            String analyzedRequest = analyzeAndClarify(request, context);

            // 6. Strategic Planning: Run once per 'new' goal. Plan becomes LOCKED for the session.
            boolean allDone = context.getOrchestrator().getTasks().stream().allMatch(t -> t.getStatus() == TaskStatus.DONE);
            boolean shouldReplan = "new".equals(intent) || context.getOrchestrator().getTasks().isEmpty();

            // Darwin Guard: If all tasks are done but intent is not 'new', we might be in a loop or the user is just chatting.
            if (allDone && !"new".equals(intent) && !"continue".equals(intent)) {
                context.log("Evo-Orchestrator-Darwin: All tasks done. Locking plan unless new intent detected.");
                shouldReplan = false;
            }

            List<Task> originalPlannedTasks;
            if (shouldReplan) {
                context.log("Evo-Orchestrator-Planning: Generating new strategic plan...");
                OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Planning...");
                originalPlannedTasks = planner.plan(analyzedRequest, context);
                OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Finished");
                context.getOrchestrator().getTasks().addAll(originalPlannedTasks);
            } else {
                context.log("Evo-Orchestrator-Planning: Strategic plan is LOCKED. Continuing with existing tasks.");
                originalPlannedTasks = new ArrayList<>(context.getOrchestrator().getTasks());
            }

            // Determine if Plan Approval is needed based on task severity
            boolean requiresPlanApproval = false;
            for (Task t : originalPlannedTasks) {
                if (t.isApprovalRequired()) {
                    requiresPlanApproval = true;
                    break;
                }
            }

            if (requiresPlanApproval) {
                // Pause for Plan Approval
                StringBuilder planSummary = new StringBuilder();
                for (int i = 0; i < originalPlannedTasks.size(); i++) {
                    Task t = originalPlannedTasks.get(i);
                    planSummary.append((i + 1)).append(". ").append(t.getName()).append("\n");
                    if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                        planSummary.append("   - ").append(t.getDescription()).append("\n");
                    }
                }

                String consolidatedPlan = consolidator.consolidate(planSummary.toString(), context);
                context.log(consolidatedPlan);

                Boolean planApproved = true;
                if (!context.isAutoApprove()) {
                    context.log("Evo-Orchestrator-Waiting: Waiting for plan approval...");
                    planApproved = context.requestApproval(TaskContext.PLAN_APPROVAL_MESSAGE).get();
                } else {
                    context.log("Evo-Orchestrator-Planning: Auto-approval enabled. Skipping manual approval.");
                }

                if (planApproved == null || !planApproved) {
                    context.log("Evo-Orchestrator-Planning: Plan rejected by user.");
                    throw new Exception("Orchestration plan rejected by user.");
                }
                context.log("Evo-Orchestrator-Planning: Plan approved. Starting execution...");
            } else {
                context.log("Evo-Orchestrator-Planning: Low severity plan generated. Skipping manual approval and starting execution...");
            }

            // Reload tasks from model in case the user modified them during approval
            List<Task> tasks = new ArrayList<>(context.getOrchestrator().getTasks());

            // 2. Execution Loop
            int taskCount = tasks.size();
            int iterationLimit = context.getPlatformMode().getIterationLimit();
            String lastResult = "";
            for (int i = 0; i < taskCount; i++) {
                Task task = tasks.get(i);
                if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.FAILED) {
                    continue;
                }

                if (i >= iterationLimit && context.getPlatformMode().getType() != PlatformType.SELF_DEV_MODE) {
                    context.log("Evo-Orchestrator-Mode: Iteration limit reached for " + context.getPlatformMode().getType() + " mode.");
                    break;
                }

                context.checkPause();
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                context.setCurrentTaskName(task.getName());

                // Check for User Approval
                if (task.isApprovalRequired() || "approval".equalsIgnoreCase(task.getType())) {
                    task.setStatus(TaskStatus.WAITING_FOR_APPROVAL);

                    Boolean approved = true;
                    if (!context.isAutoApprove()) {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Waiting for user approval...");
                        context.log("Evo-Orchestrator-Waiting: Waiting for task approval...");
                        approved = context.requestApproval("Approve task: " + task.getName() + "?").get();
                        context.log("Evo-Orchestrator-Approval: Task " + task.getName() + " " + (approved != null && approved ? "APPROVED" : "REJECTED"));
                    } else {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Auto-approving task.");
                    }

                    if (approved == null || !approved) {
                        task.setStatus(TaskStatus.FAILED);
                        task.setFeedback("Rejected by user.");
                        throw new Exception("Task rejected by user: " + task.getName());
                    }
                }

                task.setStatus(TaskStatus.RUNNING);
                if (task.getGoal() == null || task.getGoal().isEmpty()) {
                    task.setGoal(task.getName());
                }

                double progress = (double) i / taskCount;
                updateStatus(context, progress, "Executing: " + task.getName());

                boolean success = executeTaskWithRetries(task, context);

                if (!success) {
                    task.setStatus(TaskStatus.FAILED);
                    throw new Exception("Task failed after maximum retries: " + task.getName());
                }

                task.setStatus(TaskStatus.DONE);
                lastResult = task.getResponse();
                context.appendSharedMemory("Task [" + task.getName() + "] completed. Result: " + lastResult);

                // Handle Looping logic
                String loopToId = task.getLoopToTaskId();
                if (loopToId != null && !loopToId.isEmpty() && !"none".equalsIgnoreCase(loopToId)) {
                    // Decide if we should loop. For now, if the agent or a specific 'loop' task type suggests it.
                    // If it's a 'loop' task, we check its response or feedback to see if it should continue.
                    // Simplified: if task type is 'loop' and response contains 'CONTINUE', or if it's just any task with a loopToId
                    // and we haven't exceeded some internal loop limit (safety).

                    int loopTargetIndex = -1;
                    for (int j = 0; j < tasks.size(); j++) {
                        if (loopToId.equals(tasks.get(j).getId())) {
                            loopTargetIndex = j;
                            break;
                        }
                    }

                    if (loopTargetIndex != -1) {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Looping back to task ID: " + loopToId);
                        tasks.get(loopTargetIndex).setStatus(TaskStatus.READY);
                        i = loopTargetIndex - 1; // -1 because the for loop will increment i
                    }
                }
            }

            updateStatus(context, 1.0, "Completed");

            String finalResponse;
            if (context.getPlatformMode().getType() == PlatformType.SIMPLE_CHAT) {
                finalResponse = lastResult;
            } else {
                try {
                    finalResponse = finalResponseAgent.generateFinalResponse(request, tasks, context);
                } catch (Exception e) {
                    context.log("Evo-Orchestrator-Warning: Final response generation failed: " + e.getMessage());
                    finalResponse = "Tasks completed. " + lastResult;
                }
            }

            state.addMessage("Evo: " + finalResponse);
            context.getOrchestrator().setSharedMemory(ConversationState.save(context.getSharedMemory(), context.getThreadId(), state));

            // Self-Development Handoff
            if (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE) {
                performSelfDevHandoff(context, finalResponse);
            }

            // --- Unified Response Population ---
            eu.kalafatic.evolution.model.orchestration.FeedbackLevel feedbackLevel = eu.kalafatic.evolution.model.orchestration.FeedbackLevel.SIMPLE;
            if (!tasks.isEmpty()) {
                feedbackLevel = tasks.get(0).getFeedbackLevel();
            } else if (context.getOrchestrator().getTasks() != null && !context.getOrchestrator().getTasks().isEmpty()) {
                feedbackLevel = context.getOrchestrator().getTasks().get(0).getFeedbackLevel();
            }

            response.setSummary(finalResponse);

            if (feedbackLevel.getValue() >= eu.kalafatic.evolution.model.orchestration.FeedbackLevel.INTERACTIVE_VALUE) {
                response.setContent(finalResponse);
            }

            if (feedbackLevel.getValue() >= eu.kalafatic.evolution.model.orchestration.FeedbackLevel.ADVANCED_VALUE) {
                JSONArray tasksJson = new JSONArray();
                for (Task t : tasks) {
                    JSONObject tObj = new JSONObject();
                    tObj.put("id", t.getId());
                    tObj.put("name", t.getName());
                    tObj.put("status", t.getStatus().toString());
                    tasksJson.put(tObj);
                }
                response.getMetadata().put("plannedTasks", tasksJson.toString());
            }

            if (feedbackLevel.getValue() >= eu.kalafatic.evolution.model.orchestration.FeedbackLevel.FULL_VALUE) {
                response.setDebugLogs(new ArrayList<>(context.getLogs()));
            }

            return response;
        } catch (Exception e) {
            context.log("Evo-Orchestrator-Error: " + e.getMessage());
            response.setResultType(ResultType.ERROR);
            response.setSummary("Error: " + e.getMessage());
            response.setContent(e.getMessage());
            return response;
        } finally {
            OrchestrationStatusManager.getInstance().updateAgentStatus("Planner", "Idle");
            for (IAgent agent : availableAgents) {
                OrchestrationStatusManager.getInstance().updateAgentStatus(agent.getType(), "Idle");
            }
        }
    }

    private boolean executeTaskWithRetries(Task task, TaskContext context) throws Exception {
        IAgent agent = findAgentForTask(task, context);
        String lastFeedback = null;
        OrchestrationStatusManager.getInstance().updateAgentStatus(agent.getType(), "PEV Loop: " + task.getName());
        LoggingService logger = LoggingService.getInstance();
        context.setCurrentTaskId(task.getId());

        for (int retry = 1; retry <= MAX_RETRIES; retry++) {
            context.checkPause();
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            context.setCurrentIteration(retry);
            context.log("Evo-Orchestrator-" + task.getName() + ": PEV Attempt " + retry);

            // Adaptive logging: increase verbosity on retries (DEBUG = 1)
            if (retry > 1 && task.getLogLevel().getValue() > LogLevel.DEBUG_VALUE) {
                task.setLogLevel(LogLevel.DEBUG);
                logger.warn(context, "Increasing verbosity to DEBUG due to retry " + retry);
            }

            try {
                // 1. PLAN (Tactical): Determine approach for this specific task
                // @evo:20:A reason=6-phase-pev-loop
                task.setStatus(TaskStatus.PLANNING);
                context.setCurrentPhase("PLAN");
                context.log("Evo-Orchestrator-" + task.getName() + ": Phase 1 - Tactical Planning...");

                String mutationStrategy = "initial";
                IAgent currentAgent = agent;

                if (lastFeedback != null) {
                    if (lastFeedback.toLowerCase().contains("exception") || lastFeedback.toLowerCase().contains("error") || lastFeedback.toLowerCase().contains("fail")) {
                        mutationStrategy = "Syntactic fix (Self-Correction)";
                        if (task.getType().equalsIgnoreCase("maven") || task.getType().equalsIgnoreCase("shell") || task.getType().equalsIgnoreCase("file")) {
                            currentAgent = repairAgent;
                        }
                    } else if (lastFeedback.toLowerCase().contains("test") || lastFeedback.toLowerCase().contains("verify")) {
                        mutationStrategy = "Logic fix (Behavioral-Correction)";
                    } else {
                        mutationStrategy = "Heuristic improvement (Evolution)";
                    }
                }

                // Tactical Planning: Mutation allows modifying the step (task's tactical plan)
                String planInstruction = "Create a structured JSON plan with: 'steps' (array), 'targetFiles' (array), 'strategy' (string), and optional 'implementation' (string - the actual code if known). " +
                        "Mutation Strategy: " + mutationStrategy + ". Feedback: " + (lastFeedback != null ? lastFeedback : "none");

                String localPlan = currentAgent.process(task.getDescription() + "\nGOAL: " + task.getGoal() + "\nINSTRUCTION: " + planInstruction, context, lastFeedback);
                task.setPlan(localPlan);
                logger.debug(context, "Generated tactical plan", localPlan);

                // 2. CONTEXT: Gather minimal tactical context
                // @evo:20:A reason=6-phase-pev-loop
                context.setCurrentPhase("CONTEXT");
                context.log("Evo-Orchestrator-" + task.getName() + ": Phase 2 - Gathering Context...");
                ContextPackage contextPkg = ContextBuilder.build(task, context, retry, lastFeedback);
                String contextPrompt = ContextBuilder.buildPrompt(contextPkg);

                // 3. EXECUTE: Perform the action strictly using tactical context
                // @evo:21:A reason=split-execute-phase
                task.setStatus(TaskStatus.EXECUTING);
                context.setCurrentPhase("EXECUTE");
                context.log("Evo-Orchestrator-" + task.getName() + ": Phase 3 - Executing...");

                // 3a. GeneratePatch (LLM responsibility)
                String patch = generatePatch(task, currentAgent, context, lastFeedback, localPlan, contextPrompt);

                // Wrap into ChangeUnit
                ChangeUnit change = new ChangeUnit();
                change.setPatch(patch);
                change.setReason("@evo:" + context.getCurrentIteration());
                // Simple file extraction for ChangeUnit (placeholder logic)
                if (task.getType().equalsIgnoreCase("file")) {
                    change.getFiles().add(task.getName());
                }

                // 3b. ApplyPatch (Local / Supervisor responsibility - triggered here)
                String result = applyPatch(task, currentAgent, context, lastFeedback, patch);
                task.setResponse(result);

                // Capture Artifacts (result summary + ChangeUnit)
                task.setArtifacts(change.toJson().toString());

                // Handle Clarification/Proposal stall
                if (result != null && (result.contains("CLARIFY") || result.contains("[PROPOSAL:"))) {
                    if (context.isAutoApprove()) {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Auto-approval enabled. Skipping agent prompt.");
                        return true;
                    }

                    context.log("Evo-Orchestrator-Waiting: Waiting for agent-requested clarification...");

                    // Enforce Task 3: Move Proposal Consolidation INTO Orchestrator
                    String consolidatedResult = result;
                    try {
                        consolidatedResult = consolidator.consolidate(result, context);
                    } catch (Exception e) {
                        context.log("Evo-Orchestrator-Warning: Proposal consolidation failed, using raw result.");
                    }

                    String clarification = context.requestInput(consolidatedResult).get();
                    if (clarification != null) {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Received clarification: " + clarification);
                        lastFeedback = "User Response: " + clarification;
                        retry--; // Retry with the new information
                        continue;
                    }
                }

                // Design Model Update (DFE support)
                if (result != null && result.contains("[PROPOSAL:DESIGN")) {
                    try {
                        int start = result.indexOf("[PROPOSAL:DESIGN") + 16;
                        int end = result.indexOf("]", start);
                        if (end != -1) {
                            String designJson = result.substring(start, end).trim();
                            context.getOrchestrator().setSharedMemory(designJson);
                            context.log("Evo-Orchestrator: Design Model updated from [PROPOSAL:DESIGN]");
                        }
                    } catch (Exception e) {
                        context.log("Evo-Orchestrator: Error parsing design proposal: " + e.getMessage());
                    }
                }

                // 4. VERIFY: Evaluate the result
                // @evo:21:A reason=unified-validator-role
                task.setStatus(TaskStatus.VERIFYING);
                context.setCurrentPhase("VERIFY");
                context.log("Evo-Orchestrator-" + task.getName() + ": Phase 4 - Verifying...");

                JSONObject evaluation = validator.evaluate(change, task.getName(), context);

                logger.debug(context, "Evaluation result", evaluation.toString());

                if (evaluation.optBoolean("success", false)) {
                    task.setFeedback("Success: " + evaluation.optString("comment", "Task validated."));
                    return true;
                } else {
                    // 5. ANALYZE: Diagnose the failure
                    // @evo:20:A reason=6-phase-pev-loop
                    context.setCurrentPhase("ANALYZE");
                    context.log("Evo-Orchestrator-" + task.getName() + ": Phase 5 - Analyzing failure...");
                    JSONObject diagnosis = analyticAgent.diagnose(result, evaluation.optString("feedback"), context);

                    // 6. MUTATE: Adjust strategy based on diagnosis
                    // @evo:20:A reason=6-phase-pev-loop
                    context.setCurrentPhase("MUTATE");
                    context.log("Evo-Orchestrator-" + task.getName() + ": Phase 6 - Mutating approach...");

                    if (diagnosis.optBoolean("repeatFailure", false) || "SAME".equals(diagnosis.optString("progress"))) {
                        context.log("Evo-Orchestrator-Darwin: Repeated failure or NO PROGRESS detected. Escalating strategy.");
                        task.setFeedbackLevel(eu.kalafatic.evolution.model.orchestration.FeedbackLevel.ADVANCED);
                    }

                    if ("WORSE".equals(diagnosis.optString("progress"))) {
                        context.log("Evo-Orchestrator-Darwin: Situation worsened. Forcing repair or fallback.");
                    }

                    String suggested = diagnosis.optString("suggestedStrategy", "RETRY");
                    if ("REPAIR_AGENT".equals(suggested)) {
                        agent = repairAgent;
                    }

                    lastFeedback = "Verification Failed: " + evaluation.optString("feedback") + " (Diagnosis: " + diagnosis.optString("explanation") + ")";
                    context.log("Evo-Orchestrator-Darwin: Mutation triggered. Feedback: " + lastFeedback);
                    task.setFeedback("Retry " + retry + ": " + lastFeedback);

                    if (task.getLogLevel().getValue() > LogLevel.DEBUG_VALUE) {
                        task.setLogLevel(LogLevel.DEBUG);
                    }
                }
            } catch (Exception e) {
                // Adaptive logging: set level to ERROR on exception
                task.setLogLevel(LogLevel.ERROR);
                logger.error(context, "Error during PEV execution", e);
                lastFeedback = "Exception: " + e.getMessage();
                task.setFeedback("Retry " + retry + " Exception: " + e.getMessage());
            }

            if (retry == MAX_RETRIES) {
                context.log("Evo-Orchestrator-" + task.getName() + ": Failed after " + MAX_RETRIES + " retries");
                try {
                    if (context.isAutoApprove()) {
                        context.log("Evo-Orchestrator-" + task.getName() + ": Auto-approval enabled. Skipping failure guidance, failing task.");
                        return false;
                    }
                    context.log("Evo-Orchestrator-Waiting: Waiting for failure guidance...");
                    String guidance = context.requestInput("Evo-Orchestrator-" + task.getName() + ": Task failed consistently. Waiting for user guidance (retry/skip/hint)...").get();
                    if (guidance != null) {
                        if ("retry".equalsIgnoreCase(guidance.trim())) {
                            retry = 0; // Reset loop to try again
                            lastFeedback = null;
                            context.log("Evo-Orchestrator-" + task.getName() + ": User requested retry");
                        } else if ("skip".equalsIgnoreCase(guidance.trim())) {
                            context.log("Evo-Orchestrator-" + task.getName() + ": User requested to skip task");
                            task.setFeedback("Skipped by user.");
                            return true;
                        } else {
                            // Treat as hint for one more attempt
                            lastFeedback = "User Hint: " + guidance;
                            context.log("Evo-Orchestrator-" + task.getName() + ": Applying user hint: " + guidance);
                            retry = MAX_RETRIES - 1;
                        }
                    }
                } catch (Exception ex) {
                    context.log("Evo: Error getting user guidance: " + ex.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * Conceptual Step 3a: GeneratePatch (LLM responsibility)
     */
    private String generatePatch(Task task, IAgent agent, TaskContext context, String lastFeedback, String localPlan, String contextPrompt) throws Exception {
        String taskType = task.getType();
        String taskName = task.getName();
        String taskDescription = task.getDescription();
        String processInput = (taskDescription != null && !taskDescription.isEmpty()) ? taskDescription : taskName;

        // Extract implementation from plan if present to avoid overthinking
        String preGeneratedContent = null;
        try {
            JSONObject planJson = new JSONObject(localPlan);
            if (planJson.has("implementation")) {
                preGeneratedContent = planJson.getString("implementation");
            }
        } catch (Exception e) { }

        if (preGeneratedContent != null && !preGeneratedContent.isEmpty()) {
            context.log("Evo-Orchestrator-" + taskName + ": Using pre-generated patch from PLAN phase.");
            return preGeneratedContent;
        }

        if ("file".equalsIgnoreCase(taskType)) {
            context.log("Evo-Orchestrator-" + taskName + ": Generating file content/patch via " + agent.getType());
            return agent.process(processInput, context, lastFeedback);
        }

        // For non-file tasks (maven, git, shell), the "patch" is the command itself or the refined instruction
        return contextPrompt;
    }

    /**
     * Conceptual Step 3b: ApplyPatch (Local / Supervisor responsibility - signaled here)
     */
    private String applyPatch(Task task, IAgent agent, TaskContext context, String lastFeedback, String patch) throws Exception {
        String taskType = task.getType();
        String taskName = task.getName();

        if ("file".equalsIgnoreCase(taskType)) {
            FileTool fileTool = new FileTool();
            String path = taskName.replaceFirst("(?i)^(.+:\\s*)?(Write|Create|Generate|Update|Modify|Delete)(\\s+file)?\\s+", "").trim();
            path = path.replaceFirst("^([a-zA-Z]:)?(/|\\\\)+", "").replace("\\", "/");
            path = path.split("(?i)\\s+(with|to|for|using|based|containing|in|at)\\s+")[0];

            if (taskName.toLowerCase().startsWith("delete")) {
                context.log("Evo-Orchestrator-" + taskName + ": File deletion request for " + path + ". Waiting for user approval...");
                Boolean approved = true;
                if (!context.isAutoApprove()) {
                    approved = context.requestApproval("[DELETE] Approve deletion of file: " + path + "?").get();
                }
                if (approved == null || !approved) throw new Exception("File deletion rejected by user: " + path);
                return fileTool.execute("DELETE " + path, context.getProjectRoot(), context);
            }

            // Check for significant deletions
            try {
                String existingContent = fileTool.execute("READ " + path, context.getProjectRoot(), context);
                if (existingContent != null && !existingContent.isEmpty()) {
                    int existingLen = existingContent.length();
                    int newLen = patch.length();
                    if (newLen < existingLen * 0.8) {
                        double deletionPercent = (1.0 - (double)newLen / existingLen) * 100;
                        context.log("Evo-Orchestrator-" + taskName + ": Significant deletion detected (" + String.format("%.1f", deletionPercent) + "%) for " + path + ". Waiting for user approval...");
                        Boolean approved = true;
                        if (!context.isAutoApprove()) {
                            approved = context.requestApproval("[Significant deletion] Content of " + path + " will be reduced by " + String.format("%.1f", deletionPercent) + "%. Approve?").get();
                        }
                        if (approved == null || !approved) throw new Exception("Significant content reduction rejected by user for: " + path);
                    }
                }
            } catch (Exception e) {}

            String writeResult = fileTool.execute("WRITE " + path + "\n" + patch, context.getProjectRoot(), context);
            task.setResultSummary("I created/updated the file: [FILE:" + path + "]");
            return writeResult + "\nCONTENT:\n" + patch;
        } else if ("maven".equalsIgnoreCase(taskType)) {
            context.log("Evo-Orchestrator-" + taskName + ": Signaling Supervisor for Maven build...");
            MavenTool mavenTool = new MavenTool();
            return mavenTool.execute(taskName, context.getProjectRoot(), context);
        } else if ("git".equalsIgnoreCase(taskType)) {
            if (taskName.toLowerCase().matches(".*\\b(pr|pull request)\\b.*")) {
                context.log("Evo-Orchestrator-" + taskName + ": Waiting for user approval for PR...");
                Boolean approved = true;
                if (!context.isAutoApprove()) {
                    approved = context.requestApproval("[PR] Approve Pull Request creation? Task: " + taskName).get();
                }
                if (approved == null || !approved) throw new Exception("PR rejected by user: " + taskName);
            }
            GitTool gitTool = new GitTool();
            return gitTool.execute(taskName, context.getProjectRoot(), context);
        } else if ("shell".equalsIgnoreCase(taskType)) {
            context.log("Evo-Orchestrator-" + taskName + ": Waiting for user approval for command...");
            Boolean approved = true;
            if (!context.isAutoApprove()) {
                approved = context.requestApproval("Approve terminal command: " + taskName + "?").get();
            }
            if (approved == null || !approved) throw new Exception("Terminal command rejected by user: " + taskName);
            ShellTool shellTool = new ShellTool();
            return shellTool.execute(taskName, context.getProjectRoot(), context);
        }

        return agent.process(taskName, context, lastFeedback);
    }

    private String analyzeAndClarify(String request, TaskContext context) throws Exception {
        OrchestrationStatusManager.getInstance().updateAgentStatus("Analytic", "Analyzing request...");
        try {
            JSONObject analysis = analyticAgent.analyze(request, context);
            context.log("Evo-Orchestrator-Analysis: Identified category - " + analysis.optString("category"));

            if (analysis.optBoolean("isAmbiguous", false)) {
                String question = analysis.optString("clarificationQuestion", "The request is ambiguous. Can you please provide more details?");

                // If Darwin mode is enabled, we bypass blocking clarification and let the DarwinEngine handle ambiguity via variants.
                if (context.getOrchestrator().isDarwinMode()) {
                    context.log("Evo-Orchestrator-Analysis: Request identified as ambiguous, but Darwin mode is active. Bypassing clarification.");
                    return analysis.optString("refinedPrompt", request);
                }

                context.log("Evo-Orchestrator-Analysis: Request is ambiguous. Question: " + question);

                if (context.isAutoApprove()) {
                    context.log("Evo-Orchestrator-Analysis: Auto-approval enabled. Skipping clarification in headless mode.");
                    return request;
                }

                context.log("Evo-Orchestrator-Waiting: Waiting for user clarification...");
                String clarification = context.requestInput(question).get();
                if (clarification == null || clarification.trim().isEmpty()) {
                    context.log("Evo-Orchestrator-Analysis: No clarification provided.");
                    return request;
                }

                context.log("Evo-Orchestrator-Analysis: Received clarification: " + clarification);
                context.appendSharedMemory("User Clarification: " + clarification);

                // Recursively analyze with the clarification
                return analyzeAndClarify(request + "\nClarification: " + clarification, context);
            }

            String refined = analysis.optString("refinedPrompt", request);
            if (!refined.equals(request)) {
                context.log("Evo-Orchestrator-Analysis: Refined prompt for planning.");
            }
            return refined;
        } catch (Exception e) {
            context.log("Evo-Orchestrator-Analysis-Warning: " + e.getMessage());
            return request;
        } finally {
            OrchestrationStatusManager.getInstance().updateAgentStatus("Analytic", "Idle");
        }
    }

    private IAgent findAgentForTask(Task task, TaskContext context) {
        String name = task.getName().toLowerCase();
        String type = task.getType().toLowerCase();

        // 1. Check for explicit agent type in task name
        for (IAgent agent : availableAgents) {
            if (name.contains(agent.getType().toLowerCase())) {
                return agent;
            }
        }

        // 2. Map task types to specialized agents (using exact matches for clarity)
        if (type.equals("terminal") || type.equals("shell")) return availableAgents.stream().filter(a -> a instanceof TerminalAgent).findFirst().orElse(availableAgents.get(0));
        if (type.equals("file")) return availableAgents.stream().filter(a -> a instanceof FileAgent).findFirst().orElse(availableAgents.get(0));
        if (type.equals("maven")) return availableAgents.stream().filter(a -> a instanceof MavenAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("git")) return availableAgents.stream().filter(a -> a instanceof GitAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("structure") || type.contains("tree")) return availableAgents.stream().filter(a -> a instanceof StructureAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("search") || type.contains("web")) return availableAgents.stream().filter(a -> a instanceof WebSearchAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("quality") || type.contains("linter") || type.contains("checkstyle")) return availableAgents.stream().filter(a -> a instanceof QualityAgent).findFirst().orElse(availableAgents.get(0));
        if (type.contains("observability") || type.contains("log") || type.contains("tail")) return availableAgents.stream().filter(a -> a instanceof ObservabilityAgent).findFirst().orElse(availableAgents.get(0));

        if (type.contains("test")) return availableAgents.stream().filter(a -> a instanceof TesterAgent).findFirst().orElse(availableAgents.get(2));
        if (type.contains("java")) return availableAgents.stream().filter(a -> a instanceof JavaDevAgent).findFirst().orElse(availableAgents.get(1));
        if (type.contains("arch") || type.contains("design")) return availableAgents.stream().filter(a -> a instanceof ArchitectAgent).findFirst().orElse(availableAgents.get(0));

        // Default to General Agent for reasoning or unknown tasks
        return availableAgents.stream().filter(a -> a instanceof GeneralAgent).findFirst().orElse(availableAgents.get(availableAgents.size() - 1));
    }

    private void performSelfDevHandoff(TaskContext context, String summary) {
        context.log("[RCP] Initiating Self-Development Handoff to Supervisor...");
        try {
            File projectRoot = context.getProjectRoot();
            File runDir = new File(projectRoot, "self-dev-run");
            if (!runDir.exists()) runDir.mkdirs();

            // 1. Generate patch.json
            GitVersionControlProvider vcs = new GitVersionControlProvider();
            String diff = vcs.getDiff(projectRoot, "HEAD");
            List<String> changedFiles = vcs.getChangedFiles(projectRoot, "HEAD");

            JSONObject patch = new JSONObject();
            patch.put("iteration", context.getOrchestrator().getSelfDevSession() != null ?
                context.getOrchestrator().getSelfDevSession().getIterations().size() : 0);
            patch.put("files", new JSONArray(changedFiles));
            patch.put("diff", diff);
            patch.put("summary", summary);

            Files.write(new File(runDir, "patch.json").toPath(), patch.toString(4).getBytes());
            context.log("[RCP] patch.json generated.");

            // 2. Generate status.json signal
            JSONObject status = new JSONObject();
            status.put("phase", "WAITING_FOR_SUPERVISOR");
            status.put("action", "APPLY_PATCH_AND_RESTART");
            Files.write(new File(runDir, "status.json").toPath(), status.toString(4).getBytes());
            context.log("[RCP] status.json signal written.");

            // 3. Clean Shutdown
            context.log("[RCP] Shutting down for Supervisor to take over...");
            System.exit(0);
        } catch (Exception e) {
            context.log("[RCP] Handoff failed: " + e.getMessage());
        }
    }

    private void updateStatus(TaskContext context, double progress, String message) {
        String id = context.getOrchestrator().getId();
        if (id != null) {
            OrchestrationStatusManager.getInstance().updateStatus(id, progress, message);
        }
    }
}
