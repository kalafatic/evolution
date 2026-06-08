package eu.kalafatic.evolution.controller.orchestration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.utils.log.Log;

/**
 * Implementation of OrchestratorService.
 */
public class OrchestratorServiceImpl implements OrchestratorService {
    private final Map<String, TaskResult> tasks = new ConcurrentHashMap<>();

    @Override
    public OrchestratorResponse handle(TaskRequest request) {
        Orchestrator inputOrchModel = (Orchestrator) request.getContext().get("orchestrator");
        TaskContext context = (TaskContext) request.getContext().get("taskContext");

        String sessionId = (String) request.getContext().get("sessionId");
        if (sessionId == null) sessionId = UUID.randomUUID().toString();

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);

        inputOrchModel = ensureOrchestratorModel(inputOrchModel, sessionId);

        if (context == null) {
            context = new TaskContext(inputOrchModel, request.getProjectRoot());
            context.setSessionId(sessionId);
            if (session instanceof SessionContext) {
                ((SessionContext)session).setTaskContext(context);
            }
        }

        context.getMetadata().put("sessionContext", session);

        try {
            KernelFacade kernel = new KernelFacade();
            return kernel.handle(request, context);
        } catch (Exception e) {
            OrchestratorResponse response = new OrchestratorResponse();
            response.setResultType(ResultType.ERROR);
            response.setSummary("Error: " + e.getMessage());
            response.setContent(e.getMessage());
            return response;
        }
    }

    @Override
    public TaskResult execute(TaskRequest request) {
        final Orchestrator inputOrchModel = (Orchestrator) request.getContext().get("orchestrator");
        String sessionId = (String) request.getContext().get("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = (inputOrchModel != null && inputOrchModel.getId() != null && !inputOrchModel.getId().isEmpty()) ?
                         inputOrchModel.getId() : UUID.randomUUID().toString();
        }

        final String finalSessionId = sessionId;
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(finalSessionId);

        TaskResult result = new TaskResult();
        result.setId(finalSessionId);
        result.setStatus(TaskResult.Status.RUNNING);
        tasks.put(finalSessionId, result);

        session.getExecutorService().submit(() -> {
            try {
                KernelFacade kernel = new KernelFacade();
                Orchestrator orchModel = inputOrchModel;

                PromptInstructions promptInstructions = null;
                
                if (orchModel == null) {
                    orchModel = OrchestrationFactory.eINSTANCE.createOrchestrator();
                    orchModel.setId(finalSessionId);
                    
                    if (orchModel.getAiChat() == null) orchModel.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
                	
                	promptInstructions = orchModel.getAiChat().getPromptInstructions();
                	
                	if (promptInstructions == null) {
                		promptInstructions = OrchestrationFactory.eINSTANCE.createPromptInstructions();
                		orchModel.getAiChat().setPromptInstructions(promptInstructions);
                	}        	
                    
                    if (orchModel.getOllama() == null) {
                        orchModel.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
                        orchModel.getOllama().setUrl("http://localhost:11434");
                        orchModel.getOllama().setModel("llama3.2:3b");
                    }
                } else {
					promptInstructions = orchModel.getAiChat().getPromptInstructions();
				}

                String requestedModel = (String) request.getContext().get("model");
                if (requestedModel != null && !requestedModel.isEmpty()) {
                    orchModel.getOllama().setModel(requestedModel);
                }

                TaskContext context = new TaskContext(orchModel, request.getProjectRoot());
                context.setSessionId(finalSessionId);
                if (session instanceof SessionContext) {
                    ((SessionContext)session).setTaskContext(context);
                }
                context.getMetadata().put("sessionContext", session);

                context.addLogListener(log -> {
                    result.getLogs().add(log);
                });

                context.addApprovalListener(msg -> {
                    result.setStatus(TaskResult.Status.WAITING_FOR_APPROVAL);
                    result.setWaitingMessage(msg);
                });

                context.addInputListener(msg -> {
                    result.setStatus(TaskResult.Status.WAITING_FOR_INPUT);
                    result.setWaitingMessage(msg);
                });

                OrchestratorResponse orchResponse = kernel.handle(request, context);

                result.setResponse(orchResponse.getSummary());
                result.setStatus(TaskResult.Status.SUCCESS);

                for (Task t : orchModel.getTasks()) {
                    if (t.getResultSummary() != null && t.getResultSummary().contains("[FILE:")) {
                        result.getFileChanges().add(t.getResultSummary());
                    }
                }

            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setError(e.getMessage());
                result.getLogs().add("[" + finalSessionId + "] ERROR: " + e.getMessage());
            }
        });

        return result;
    }

    @Override
    public TaskResult getTaskResult(String id) {
        return tasks.get(id);
    }

    @Override
    public void shutdownSession(String sessionId) {
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        if (session == null) {
            throw new IllegalStateException("OrchestratorServiceImpl: session is null for sessionId: " + sessionId);
        }
        RuntimeEventBus bus = session.getEventBus();
        SessionManager.getInstance().shutdownSession(sessionId);
        tasks.remove(sessionId);
        bus.publish(new RuntimeEvent(RuntimeEventType.KERNEL_SHUTDOWN, sessionId, "OrchestratorService", null));
    }

    @Override
    public void setPaused(String sessionId, boolean paused) {
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        TaskContext context = (session instanceof SessionContext) ? ((SessionContext)session).getTaskContext() : null;
        if (context != null) {
            context.setPaused(paused);
            RuntimeEventBus bus = session.getEventBus();
            bus.publish(new RuntimeEvent(paused ? RuntimeEventType.FLOW_PAUSED : RuntimeEventType.STEP_RESUMED, sessionId, "OrchestratorService", null));
        }
    }

    @Override
    public void updateConfiguration(String sessionId, java.util.Map<String, Object> settings) {
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        Orchestrator model = (this.orchestrator != null) ? this.orchestrator : null;

        if (session instanceof SessionContext) {
            TaskContext context = ((SessionContext)session).getTaskContext();
            if (context != null && context.getOrchestrator() != null) {
                model = context.getOrchestrator();
            }
        }

        boolean changed = !settings.isEmpty();

        if (model != null) {
            if (settings.containsKey("aiMode")) {
                Object modeVal = settings.get("aiMode");
                if (modeVal instanceof Integer) {
                    model.setAiMode(eu.kalafatic.evolution.model.orchestration.AiMode.get((Integer)modeVal));
                } else if (modeVal instanceof eu.kalafatic.evolution.model.orchestration.AiMode) {
                    model.setAiMode((eu.kalafatic.evolution.model.orchestration.AiMode)modeVal);
                }
            }
            if (settings.containsKey("localModel")) {
                model.setLocalModel((String)settings.get("localModel"));
                if (model.getOllama() != null) model.getOllama().setModel((String)settings.get("localModel"));
            }
            if (settings.containsKey("remoteModel")) {
                model.setRemoteModel((String)settings.get("remoteModel"));
            }
            if (settings.containsKey("darwinMode")) {
                model.setDarwinMode((Boolean)settings.get("darwinMode"));
            }

            // Handle ChatSession settings if sessionId matches
            if (model.getAiChat() == null) {
                model.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
            }

            final String sid = sessionId;
            model.getAiChat().getSessions().stream()
                .filter(s -> sid.equals(s.getId()))
                .findFirst()
                .ifPresent(s -> {
                    if (settings.containsKey("iterativeMode")) s.setIterativeMode((Boolean)settings.get("iterativeMode"));
                    if (settings.containsKey("selfIterativeMode")) s.setSelfIterativeMode((Boolean)settings.get("selfIterativeMode"));
                    if (settings.containsKey("darwinMode")) s.setDarwinMode((Boolean)settings.get("darwinMode"));
                    if (settings.containsKey("gitAutomation")) s.setGitAutomation((Boolean)settings.get("gitAutomation"));
                    if (settings.containsKey("stepMode")) s.setStepMode((Boolean)settings.get("stepMode"));
                    if (settings.containsKey("maxIterations")) s.setMaxIterations((Integer)settings.get("maxIterations"));
                    if (settings.containsKey("autoApprove")) s.setAutoApprove((Boolean)settings.get("autoApprove"));
                    if (settings.containsKey("aiMode")) {
                        Object modeVal = settings.get("aiMode");
                        if (modeVal instanceof Integer) {
                            s.setAiMode(eu.kalafatic.evolution.model.orchestration.AiMode.get((Integer)modeVal));
                        } else if (modeVal instanceof eu.kalafatic.evolution.model.orchestration.AiMode) {
                            s.setAiMode((eu.kalafatic.evolution.model.orchestration.AiMode)modeVal);
                        }
                    }
                    if (settings.containsKey("localModel")) s.setLocalModel((String)settings.get("localModel"));
                    if (settings.containsKey("remoteModel")) s.setRemoteModel((String)settings.get("remoteModel"));
                    if (settings.containsKey("bitState")) s.setBitState((Long)settings.get("bitState"));
                    if (settings.containsKey("expansion")) s.setExpansion((Integer)settings.get("expansion"));
                    if (settings.containsKey("targetPath")) s.setTargetPath((String)settings.get("targetPath"));
                });

            PromptInstructions pi = model.getAiChat().getPromptInstructions();
            if (pi == null) {
                pi = OrchestrationFactory.eINSTANCE.createPromptInstructions();
                model.getAiChat().setPromptInstructions(pi);
            }

            if (settings.containsKey("iterativeMode")) pi.setIterativeMode((Boolean)settings.get("iterativeMode"));
            if (settings.containsKey("selfIterativeMode")) pi.setSelfIterativeMode((Boolean)settings.get("selfIterativeMode"));
            if (settings.containsKey("gitAutomation")) pi.setGitAutomation((Boolean)settings.get("gitAutomation"));
            if (settings.containsKey("stepMode")) pi.setStepMode((Boolean)settings.get("stepMode"));
            if (settings.containsKey("maxIterations")) pi.setPreferredMaxIterations((Integer)settings.get("maxIterations"));
            if (settings.containsKey("autoApprove")) {
                boolean autoApprove = (Boolean)settings.get("autoApprove");
                pi.setAutoApprove(autoApprove);

                // If auto-approve is enabled while waiting, resume the session
                if (autoApprove && session instanceof SessionContext) {
                    TaskContext taskContext = ((SessionContext)session).getTaskContext();
                    if (taskContext != null && (taskContext.isWaitingForApproval() || taskContext.isWaitingForInput())) {
                        provideApproval(sessionId, true);
                    }
                }
            }
        }

        if (changed && session != null) {
            RuntimeEventBus bus = session.getEventBus();
            if (bus != null) {
                bus.publish(new RuntimeEvent(RuntimeEventType.CONFIGURATION_UPDATED, sessionId, "OrchestratorService", settings));
            }
        }
    }

    @Override
    public void submit(String sessionId, TaskRequest request) {
        final SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        final RuntimeEventBus bus = session.getEventBus();
        final String turnId = sessionId + "__" + System.currentTimeMillis();

        bus.publish(new RuntimeEvent(RuntimeEventType.USER_INTERACTION_RECEIVED, sessionId, "UI", request.getPrompt()));

        // CONTINUATION HANDLING: If the session is waiting for input/approval, treat the prompt as a response
        if (session instanceof SessionContext) {
            TaskContext context = ((SessionContext)session).getTaskContext();
            if (context != null && (context.isWaitingForApproval() || context.isWaitingForInput())) {
                String prompt = request.getPrompt().trim();
                boolean isControl = prompt.equalsIgnoreCase("yes") || prompt.equalsIgnoreCase("no") ||
                                   prompt.toLowerCase().startsWith("select ") ||
                                   prompt.toLowerCase().startsWith("approve variant ") ||
                                   prompt.toLowerCase().startsWith("reject variant ") ||
                                   prompt.toLowerCase().startsWith("keep variant ") ||
                                   prompt.equalsIgnoreCase("force solution") ||
                                   prompt.equalsIgnoreCase("approved") ||
                                   prompt.equalsIgnoreCase("rejected") ||
                                   prompt.equalsIgnoreCase("ok") ||
                                   prompt.equalsIgnoreCase("okay") ||
                                   prompt.equalsIgnoreCase("proceed") ||
                                   prompt.toLowerCase().startsWith("approve variant ") ||
                                   prompt.matches("^(yes|y|ok|okay|approve|proceed|go ahead|yep|sure)$");

                if (isControl) {
                    Log.log("[SERVICE] Continuation detected: " + prompt + ". Routing to existing wait.");
                    if (context.isWaitingForApproval()) {
                        boolean approved = prompt.equalsIgnoreCase("yes") ||
                                         prompt.equalsIgnoreCase("approved") ||
                                         prompt.equalsIgnoreCase("proceed") ||
                                         prompt.equalsIgnoreCase("ok") ||
                                         prompt.toLowerCase().startsWith("approve variant ") ||
                                         prompt.matches("^(yes|y|ok|okay|approve|proceed|go ahead|yep|sure)$");
                        provideApproval(sessionId, approved);
                    } else {
                        provideInput(sessionId, prompt);
                    }
                    return;
                }
            }
        }

        session.getExecutorService().submit(() -> {
            bus.publish(new RuntimeEvent(RuntimeEventType.FLOW_STARTED, sessionId, "OrchestratorService", request.getPrompt()));
            try {
                Orchestrator orchModel = (Orchestrator) request.getContext().get("orchestrator");
                if (orchModel == null) orchModel = this.orchestrator;

                orchModel = ensureOrchestratorModel(orchModel, sessionId);

                // Sync session settings to model/context if available
                final String sid = sessionId;
                final Orchestrator finalOrch = orchModel;
                orchModel.getAiChat().getSessions().stream()
                    .filter(s -> sid.equals(s.getId()))
                    .findFirst()
                    .ifPresent(s -> {
                        if (s.getAiMode() != null) finalOrch.setAiMode(s.getAiMode());
                        if (s.getLocalModel() != null) {
                            finalOrch.setLocalModel(s.getLocalModel());
                            if (finalOrch.getOllama() != null) finalOrch.getOllama().setModel(s.getLocalModel());
                        }
                        if (s.getRemoteModel() != null) finalOrch.setRemoteModel(s.getRemoteModel());
                    if (s.getTargetPath() != null) {
                        // For Mediated Mode persistence
                    }

                        // Sync session flags to PromptInstructions
                        PromptInstructions pi = finalOrch.getAiChat().getPromptInstructions();
                        if (pi == null) {
                            pi = OrchestrationFactory.eINSTANCE.createPromptInstructions();
                            finalOrch.getAiChat().setPromptInstructions(pi);
                        }
                        pi.setAutoApprove(s.isAutoApprove());
                        pi.setStepMode(s.isStepMode());
                        pi.setGitAutomation(s.isGitAutomation());
                        pi.setIterativeMode(s.isIterativeMode());
                        pi.setSelfIterativeMode(s.isSelfIterativeMode());
                        pi.setPreferredMaxIterations(s.getMaxIterations());
                    });

                TaskContext context = (session instanceof SessionContext) ? ((SessionContext)session).getTaskContext() : null;

                if (context == null) {
                    context = new TaskContext(orchModel, request.getProjectRoot());
                    context.setSessionId(sessionId);
                    if (session instanceof SessionContext) {
                        ((SessionContext)session).setTaskContext(context);
                    }
                }
                context.getMetadata().put("sessionContext", session);
                context.setStartTime(java.time.Instant.now());

                // Clear any existing listeners from previous turns to prevent duplication
                context.clearListeners();

                // Add log propagation to UI via ConversationOutputController
                context.addLogListener(log -> processLogEntry(log, sessionId, turnId));
                context.addApprovalListener(msg -> processLogEntry(msg, sessionId, turnId));
                context.addInputListener(msg -> processLogEntry(msg, sessionId, turnId));

                context.addTokenRequestListener((provider, future) -> bus.publish(new RuntimeEvent(RuntimeEventType.VIEW_UPDATED, sessionId, "TokenRequest", new Object[]{provider, future})));

                KernelFacade kernel = new KernelFacade();
                OrchestratorResponse response = kernel.handle(request, context);

                processLogEntry("Final Response: " + response.getSummary(), sessionId, turnId);

                // Refresh workspace
                try {
                    org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, null);
                } catch (Exception e) {
                	  processLogEntry("Error: " + e.getMessage(), sessionId, turnId);
                }
                

                bus.publish(new RuntimeEvent(RuntimeEventType.FLOW_COMPLETED, sessionId, "OrchestratorService", response));
            } catch (Throwable e) {
                Log.log("CRITICAL ERROR in orchestration thread: " + e.getMessage());
                processLogEntry("Error: " + e.getMessage(), sessionId, turnId);
                bus.publish(new RuntimeEvent(RuntimeEventType.TASK_FAILED, sessionId, "OrchestratorService", e.getMessage()));
            }
        });
    }

    private void processLogEntry(String log, String sessionId, String turnId) {
        if (log == null || log.isEmpty()) return;

        String trimmedText = log.trim();
        String sender = "Evo";
        String content = trimmedText;
        String agentType = "ai";
        MessagePriority priority = MessagePriority.PROGRESS;

        java.util.regex.Pattern logPattern = java.util.regex.Pattern.compile("^([A-Z][A-Z0-9-]*)(?:\\s+\\[(.*?)\\])?(?:\\s+\\(\\d{2}:\\d{2}:\\d{2}\\))?:\\s*([\\s\\S]*)$", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = logPattern.matcher(trimmedText);

        if (matcher.find()) {
            sender = matcher.group(1);
            String extra = matcher.group(2);
            content = matcher.group(3);

            String senderUpper = sender.toUpperCase();
            if (senderUpper.startsWith("USER")) agentType = "user";
            else if (senderUpper.startsWith("EVO")) agentType = "ai";
            else if (senderUpper.startsWith("TOOL")) agentType = "tool";
            else if (senderUpper.startsWith("LLMROUTER")) agentType = "orchestrator";

            String agentSource = (sender + (extra != null ? "-" + extra : "")).toLowerCase();
            if (agentSource.contains("planner")) agentType = "planner";
            else if (agentSource.contains("architect")) agentType = "architect";
            else if (agentSource.contains("javadev")) agentType = "javadev";
            else if (agentSource.contains("tester")) agentType = "tester";
            else if (agentSource.contains("reviewer")) agentType = "reviewer";
            else if (agentSource.contains("analytic") || agentSource.contains("analysis")) agentType = "analytic";
            else if (agentSource.contains("general")) agentType = "general";
            else if (agentSource.contains("terminal")) agentType = "terminal";
            else if (agentSource.contains("file")) agentType = "file";
            else if (agentSource.contains("maven")) agentType = "maven";
            else if (agentSource.contains("git")) agentType = "git";
            else if (agentSource.contains("structure")) agentType = "structure";
            else if (agentSource.contains("websearch")) agentType = "websearch";
            else if (agentSource.contains("quality")) agentType = "quality";
            else if (agentSource.contains("observability")) agentType = "observability";
            else if (agentSource.contains("orchestrator")) agentType = "orchestrator";
            else if (agentSource.contains("darwinengine")) agentType = "darwin";

            if (agentSource.contains("thinking")) agentType = "thinking";
            else if (agentSource.contains("response") && !agentType.equals("darwin")) {
                agentType = "response";
                priority = MessagePriority.NORMAL;
            }
        } else if (trimmedText.startsWith("Final Response: ")) {
            sender = "Final Response";
            content = trimmedText.substring(16);
            agentType = "final-response";
            priority = MessagePriority.FINAL;
        } else if (trimmedText.startsWith("Error: ")) {
            sender = "Error";
            content = trimmedText.substring(7);
            agentType = "error";
            priority = MessagePriority.FINAL;
        } else if (trimmedText.startsWith("Result Summary: ")) {
            sender = "Result Summary";
            content = trimmedText.substring(16);
            agentType = "result-summary";
            priority = MessagePriority.FINAL;
        }

        if (content.contains("[DARWIN_BRANCHES]")) {
            agentType = "darwin-branches waiting";
            content = content.replace("[DARWIN_BRANCHES]", "").trim();
            priority = MessagePriority.USER_ACTION_REQUIRED;
        }

        java.util.regex.Pattern approvedPattern = java.util.regex.Pattern.compile("\\[(APPROVED|REJECTED|KEPT):([^]]+)\\]");
        java.util.regex.Matcher approvedMatcher = approvedPattern.matcher(content);
        boolean foundStatus = false;
        while (approvedMatcher.find()) {
            foundStatus = true;
            String status = approvedMatcher.group(1).toLowerCase();
            String variantId = approvedMatcher.group(2);

            if (!agentType.contains(status + ":" + variantId)) {
                if (agentType.contains("waiting")) {
                    agentType = agentType.replace("waiting", "").trim();
                }
                if (agentType.isEmpty()) agentType = "ai";
                agentType += " " + status + ":" + variantId;
            }

            if (agentType.contains("darwin-branches")) {
                 content = content.replace(approvedMatcher.group(0), "").trim();
                 content = content.replaceAll("\\[[\\s\\S]*\\]", "").trim();
                 if (content.isEmpty()) content = "Evolution variant " + variantId + " " + status + ".";
            } else {
                 content = content.replace(approvedMatcher.group(0), "").trim();
            }
        }

        if (foundStatus) {
            priority = MessagePriority.NORMAL;
        }

        boolean needsApproval = (content.toLowerCase().contains("waiting for user") ||
                content.toLowerCase().contains("guidance?") ||
                content.toLowerCase().contains("clarify") ||
                content.toLowerCase().contains("clarification") ||
                content.contains("[PROPOSAL:") ||
                content.toLowerCase().contains("ambiguous") ||
                content.toLowerCase().contains("approve") ||
                content.toLowerCase().contains("approval") ||
                content.toLowerCase().contains("proceed?")) &&
                !content.contains("AUTO_INFER") &&
                !content.contains("BRANCH_PARALLEL") &&
                !content.contains("Interpretation State: CLEAR");

        if (needsApproval && !agentType.contains("user")) {
            if (!agentType.contains("waiting")) agentType += " waiting";
            priority = MessagePriority.USER_ACTION_REQUIRED;
        }

        content = content.replaceAll("\\[KERNEL\\]", "")
                        .replaceAll("\\[STRATEGY\\]", "")
                        .replaceAll("\\[ANALYSIS\\]", "")
                        .replaceAll("\\[DIAGNOSIS\\]", "")
                        .replaceAll("\\[SUPERVISOR\\]", "")
                        .replaceAll("\\[EVO\\]", "")
                        .replaceAll("\\[DARWIN\\]", "")
                        .replaceAll("\\[DARWINENGINE\\]", "")
                        .replaceAll("\\[THINKING\\]", "")
                        .replaceAll("\\[ORCHESTRATOR\\]", "")
                        .trim();

        ConversationOutputController.getInstance().submitMessage(sessionId, turnId, sender, content, agentType, priority, priority == MessagePriority.FINAL);
    }

    private Orchestrator orchestrator;

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public void registerContext(String id, TaskContext context) {
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(id);
        if (session instanceof SessionContext) {
            ((SessionContext)session).setTaskContext(context);
        }
    }

    @Override
    public void provideApproval(String taskId, boolean approved) {
        SessionContainer session = SessionManager.getInstance().getSession(taskId);
        TaskContext context = (session instanceof SessionContext) ? ((SessionContext)session).getTaskContext() : null;
        if (context != null) {
            context.log("User Interaction: Approval provided - " + approved);
            context.provideApproval(approved);
            TaskResult result = tasks.get(taskId);
            if (result != null && (result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL || result.getStatus() == TaskResult.Status.WAITING_FOR_INPUT)) {
                result.setStatus(TaskResult.Status.RUNNING);
                result.setWaitingMessage(null);
            }
        }
    }

    @Override
    public void provideInput(String taskId, String input) {
        SessionContainer session = SessionManager.getInstance().getSession(taskId);
        TaskContext context = (session instanceof SessionContext) ? ((SessionContext)session).getTaskContext() : null;
        if (context != null) {
            context.log("User Interaction: Input provided - " + input);
            context.provideInput(input);
            TaskResult result = tasks.get(taskId);
            if (result != null && result.getStatus() == TaskResult.Status.WAITING_FOR_INPUT) {
                result.setStatus(TaskResult.Status.RUNNING);
                result.setWaitingMessage(null);
            }
        }
    }

    @Override
    public void resumeStep(String sessionId, String stepId, eu.kalafatic.evolution.controller.workflow.WorkflowStatus status) {
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        if (session instanceof SessionContext) {
            ((SessionContext)session).getStepModeController().resumeStep(stepId, status);
        }
    }

    private Orchestrator ensureOrchestratorModel(Orchestrator model, String sessionId) {
        if (model != null) return model;

        Orchestrator newModel = OrchestrationFactory.eINSTANCE.createOrchestrator();
        newModel.setId(sessionId);
        if (newModel.getAiChat() == null) newModel.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        if (newModel.getAiChat().getPromptInstructions() == null) {
            newModel.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());
        }
        if (newModel.getOllama() == null) {
            newModel.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
            newModel.getOllama().setUrl("http://localhost:11434");
            newModel.getOllama().setModel("llama3.2:3b");
        }
        return newModel;
    }

    private static OrchestratorServiceImpl instance;
    public static synchronized OrchestratorServiceImpl getInstance() {
        if (instance == null) {
            instance = new OrchestratorServiceImpl();
        }
        return instance;
    }
}
