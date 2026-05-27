package eu.kalafatic.evolution.controller.orchestration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Implementation of OrchestratorService.
 */
public class OrchestratorServiceImpl implements OrchestratorService {
    private final Map<String, TaskResult> tasks = new ConcurrentHashMap<>();
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    @Override
    public OrchestratorResponse handle(TaskRequest request) {
        final Orchestrator inputOrchModel = (Orchestrator) request.getContext().get("orchestrator");
        TaskContext context = (TaskContext) request.getContext().get("taskContext");

        String sessionId = (String) request.getContext().get("sessionId");
        if (sessionId == null) sessionId = UUID.randomUUID().toString();

        SessionContext session = sessions.computeIfAbsent(sessionId, SessionContext::new);

        if (context == null) {
            context = new TaskContext(inputOrchModel, request.getProjectRoot());
            context.setSessionId(sessionId);
            session.setTaskContext(context);
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
        SessionContext session = sessions.computeIfAbsent(finalSessionId, SessionContext::new);

        TaskResult result = new TaskResult();
        result.setId(finalSessionId);
        result.setStatus(TaskResult.Status.RUNNING);
        tasks.put(finalSessionId, result);

        session.getExecutorService().submit(() -> {
            try {
                KernelFacade kernel = new KernelFacade();
                Orchestrator orchModel = inputOrchModel;

                // We might need a real Orchestrator EMF object here if the system depends on it
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
                    
                    // Default Ollama configuration if not present
                    if (orchModel.getOllama() == null) {
                        orchModel.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
                        orchModel.getOllama().setUrl("http://localhost:11434");
                        orchModel.getOllama().setModel("llama3.2:3b");
                    }
                } else {
					promptInstructions = orchModel.getAiChat().getPromptInstructions();
				}

                // Override model if provided in request
                String requestedModel = (String) request.getContext().get("model");
                if (requestedModel != null && !requestedModel.isEmpty()) {
                    orchModel.getOllama().setModel(requestedModel);
                }

                TaskContext context = new TaskContext(orchModel, request.getProjectRoot());
                context.setSessionId(finalSessionId);
                session.setTaskContext(context);
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

                // NOTE: Git Automation (Branching and Commit) is now centrally managed by the IterationManager Kernel.
                // Redundant Git logic removed to ensure single authority.

                OrchestratorResponse orchResponse = kernel.handle(request, context);

                result.setResponse(orchResponse.getSummary());
                result.setStatus(TaskResult.Status.SUCCESS);

                // Capture file changes (simplified for now)
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
        SessionContext session = sessions.remove(sessionId);
        if (session != null) {
            session.shutdown();
        }
        tasks.remove(sessionId);
    }

    private Orchestrator orchestrator;

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public void registerContext(String id, TaskContext context) {
        SessionContext session = sessions.computeIfAbsent(id, SessionContext::new);
        session.setTaskContext(context);
    }

    public void provideApproval(String taskId, boolean approved) {
        SessionContext session = sessions.get(taskId);
        TaskContext context = session != null ? session.getTaskContext() : null;
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

    public void provideInput(String taskId, String input) {
        SessionContext session = sessions.get(taskId);
        TaskContext context = session != null ? session.getTaskContext() : null;
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

    // singleton for easy access from RCP and REST
    private static OrchestratorServiceImpl instance;
    public static synchronized OrchestratorServiceImpl getInstance() {
        if (instance == null) {
            instance = new OrchestratorServiceImpl();
        }
        return instance;
    }
}
