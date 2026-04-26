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
    private final Map<String, TaskContext> contexts = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public TaskResult execute(TaskRequest request) {
        final Orchestrator inputOrchModel = (Orchestrator) request.getContext().get("orchestrator");
        String taskId = (inputOrchModel != null && inputOrchModel.getId() != null && !inputOrchModel.getId().isEmpty()) ?
                         inputOrchModel.getId() : UUID.randomUUID().toString();

        TaskResult result = new TaskResult();
        result.setId(taskId);
        result.setStatus(TaskResult.Status.RUNNING);
        tasks.put(taskId, result);

        executor.submit(() -> {
            try {
                EvolutionOrchestrator evolutionOrchestrator = new EvolutionOrchestrator();
                Orchestrator orchModel = inputOrchModel;

                // We might need a real Orchestrator EMF object here if the system depends on it
                PromptInstructions promptInstructions = null;
                
                if (orchModel == null) {
                    orchModel = OrchestrationFactory.eINSTANCE.createOrchestrator();
                    orchModel.setId(taskId);
                    
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
                String threadId = (String) request.getContext().get("threadId");
                if (threadId != null && !threadId.isEmpty()) {
			context.setThreadId(threadId);
                } else {
			context.setThreadId(taskId);
                }

                context.addLogListener(log -> {
                    result.getLogs().add(log);
                });

                contexts.put(taskId, context);

                context.addApprovalListener(msg -> {
                    result.setStatus(TaskResult.Status.WAITING_FOR_APPROVAL);
                    result.setWaitingMessage(msg);
                });

                context.addInputListener(msg -> {
                    result.setStatus(TaskResult.Status.WAITING_FOR_INPUT);
                    result.setWaitingMessage(msg);
                });

                // Git Integration: Branching
                if (promptInstructions.isGitAutomation()) {
                    GitTool gitTool = new GitTool();
                    String requestedBranch = (String) request.getContext().get("branch");
                    String branchName = (requestedBranch != null && !requestedBranch.isEmpty()) ?
                                         requestedBranch : "evo-" + taskId.substring(0, 8);

                    try {
                        eu.kalafatic.evolution.controller.tools.ShellTool shell = new eu.kalafatic.evolution.controller.tools.ShellTool();

                        if (requestedBranch != null && !requestedBranch.isEmpty()) {
                            context.log("GIT: Checking out existing branch " + branchName);
                            try {
                                shell.execute("git checkout " + branchName, request.getProjectRoot(), context);
                            } catch (Exception e) {
                                context.log("GIT: Branch " + branchName + " not found, creating it.");
                                shell.execute("git checkout -b " + branchName, request.getProjectRoot(), context);
                            }
                        } else {
                            context.log("GIT: Creating new branch " + branchName);
                            shell.execute("git checkout -b " + branchName, request.getProjectRoot(), context);
                        }
                    } catch (Exception e) {
                        context.log("GIT WARNING: Could not manage branch: " + e.getMessage());
                    }
                }

                String response = evolutionOrchestrator.execute(request.getPrompt(), context);

                result.setResponse(response);

                // Git Integration: Commit
                if (promptInstructions.isGitAutomation() && result.getStatus() == TaskResult.Status.SUCCESS) {
                    try {
                        context.log("GIT: Committing changes");
                        GitTool gitTool = new GitTool();
                        gitTool.execute("add commit", request.getProjectRoot(), context);
                    } catch (Exception e) {
                        context.log("GIT WARNING: Could not commit changes: " + e.getMessage());
                    }
                }
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
                result.getLogs().add("ERROR: " + e.getMessage());
            }
        });

        return result;
    }

    @Override
    public TaskResult getTaskResult(String id) {
        return tasks.get(id);
    }

    public void provideApproval(String taskId, boolean approved) {
        TaskContext context = contexts.get(taskId);
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
        TaskContext context = contexts.get(taskId);
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
