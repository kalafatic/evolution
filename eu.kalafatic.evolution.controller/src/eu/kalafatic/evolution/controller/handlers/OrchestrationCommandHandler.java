package eu.kalafatic.evolution.controller.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import eu.kalafatic.evolution.model.orchestration.*;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.manager.TrainingManager;
import eu.kalafatic.evolution.controller.engine.NeuronEngine;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;


public class OrchestrationCommandHandler extends AbstractOrchestratorHandler {

    private final AiService aiService = new AiService();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Orchestrator orchestrator = getOrchestrator(event);
        if (orchestrator != null) {
            IProject project = getProject(orchestrator);
            Job job = new OrchestrationJob("Orchestration", orchestrator, project);
            job.schedule();
        }
        return null;
    }

    private class OrchestrationJob extends Job {
        private Orchestrator orchestrator;
        private IProject project;

        public OrchestrationJob(String name, Orchestrator orchestrator, IProject project) {
            super(name);
            this.orchestrator = orchestrator;
            this.project = project;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                executeOrchestration(orchestrator, project, monitor);
            } catch (Exception e) {
                e.printStackTrace();
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(null, "Orchestration Failed", e.getMessage());
                });
                return Status.CANCEL_STATUS;
            }
            return Status.OK_STATUS;
        }
    }

    private void executeOrchestration(Orchestrator orchestrator, IProject project, IProgressMonitor monitor) throws Exception {
        String id = orchestrator.getId();
        OrchestrationStatusManager.getInstance().updateStatus(id, 0.0, "Starting");

        monitor.beginTask("Orchestration: " + orchestrator.getName(), 100);

        TaskContext context = new TaskContext(orchestrator, project.getLocation().toFile());
        context.addTokenRequestListener((provider, future) -> {
            Display.getDefault().asyncExec(() -> {
                org.eclipse.jface.dialogs.InputDialog dlg = new org.eclipse.jface.dialogs.InputDialog(null, "API Token Required", "Please enter the API token for " + provider + ":", "", null);
                if (dlg.open() == org.eclipse.jface.window.Window.OK) {
                    String token = dlg.getValue();
                    orchestrator.setOpenAiToken(token);
                    future.complete(token);
                } else {
                    future.completeExceptionally(new Exception("Token request cancelled by user."));
                }
            });
        });
        context.appendSharedMemory("Initial Request: " + orchestrator.getAiChat().getPrompt());

        EvolutionOrchestrator core = new EvolutionOrchestrator();
        core.execute(orchestrator.getAiChat().getPrompt(), context);

        OrchestrationStatusManager.getInstance().updateStatus(id, 1.0, "Completed");
        monitor.done();
    }

    private List<Task> decomposeTasks(Orchestrator orchestrator, TaskContext context) throws Exception {
        String plannerPrompt = "You are a workflow planner for an agentic system. " +
                "Decompose the user request into a sequence of atomic, specialized tasks.\n" +
                "Available task types:\n" +
                "- 'llm': For reasoning, planning, or general text generation.\n" +
                "- 'file': For writing or creating files (e.g., Java source code, POM, README). Task name should be 'Write <path/to/file>'.\n" +
                "- 'git': For version control actions (add, commit, push).\n" +
                "- 'maven': For building, testing, or packaging the project.\n" +
                "- 'train_nn': For local project neural network training.\n" +
                "- 'train_llm': For local project LLM fine-tuning.\n" +
                "- 'train_agent': For local project agent behavior training.\n\n" +
                "Output MUST be a valid JSON array of objects. Schema:\n" +
                "[ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\"|\"train_nn\"|\"train_llm\"|\"train_agent\" } ]\n\n" +
                "Request: " + orchestrator.getAiChat().getPrompt();

        String response = sendRequest(orchestrator, plannerPrompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null, context);
        JSONArray jsonArray = extractJsonArray(response);

        List<Task> tasks = new ArrayList<>();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Task task = factory.createTask();
            task.setId(obj.optString("id", "task" + i));
            task.setName(obj.optString("name", "Task " + i));
            task.setType(obj.optString("taskType", "llm"));
            tasks.add(task);
        }
        return tasks;
    }

    private String evaluateTask(Orchestrator orchestrator, Task task, String context, TaskContext taskContext) throws Exception {
        String evalPrompt = "You are an AI Critic. Evaluate if the following task was successfully completed.\n\n" +
                "TASK GOAL: " + task.getName() + "\n" +
                "AGENT OUTPUT: " + task.getResponse() + "\n" +
                "OVERALL CONTEXT: " + context + "\n\n" +
                "CRITERIA:\n" +
                "1. Does the output directly address the goal?\n" +
                "2. Is the output technically sound and complete?\n\n" +
                "Output MUST be a valid JSON object. Schema:\n" +
                "{ \"success\": boolean, \"feedback\": \"Detailed explanation of why it failed and how to fix it\", \"comment\": \"Brief success message\" }";

        return sendRequest(orchestrator, evalPrompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null, taskContext);
    }

    private JSONObject parseEvaluation(String response) {
        try {
            return extractJsonObject(response);
        } catch (Exception e) {
            JSONObject fallback = new JSONObject();
            fallback.put("success", false);
            fallback.put("feedback", "Failed to parse evaluator response: " + e.getMessage());
            return fallback;
        }
    }

    private void validateRules(Agent agent, Task task, Orchestrator orchestrator) throws Exception {
        if (agent == null) return;
        for (Rule rule : agent.getRules()) {
            if (rule instanceof AccessRule) {
                AccessRule ar = (AccessRule) rule;
                String taskName = task.getName().toLowerCase();
                for (String denied : ar.getDeniedPaths()) {
                    if (taskName.contains(denied.toLowerCase())) {
                        throw new Exception("Rule Violation: Agent [" + agent.getId() + "] is denied access to [" + denied + "] (found in task: " + task.getName() + ")");
                    }
                }
            } else if (rule instanceof NetworkRule) {
                NetworkRule nr = (NetworkRule) rule;
                if (!nr.isAllowAll() && !nr.getAllowedDomains().isEmpty()) {
                    String url = (orchestrator.getOllama() != null && orchestrator.getOllama().getUrl() != null) ?
                                 orchestrator.getOllama().getUrl() :
                                 (orchestrator.getAiChat() != null ? orchestrator.getAiChat().getUrl() : null);
                    if (url != null) {
                        boolean allowed = false;
                        for (String domain : nr.getAllowedDomains()) {
                            if (url.contains(domain)) {
                                allowed = true;
                                break;
                            }
                        }
                        if (!allowed) throw new Exception("Rule Violation: Agent [" + agent.getId() + "] is not allowed to access URL [" + url + "]");
                    }
                }
            }
        }
    }

//    private String executeTask(Orchestrator orchestrator, Agent agent, Task task, String context, String lastFeedback) throws Exception {
//        validateRules(agent, task, orchestrator);
//        
//        
//    }
    private String executeTask(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback, TaskContext taskContext) throws Exception {
        String taskType = task.getType();

        if ("git".equalsIgnoreCase(taskType)) {
            return executeGitTool(project, orchestrator, task.getName());
        } else if ("maven".equalsIgnoreCase(taskType)) {
            return executeMavenTool(project, orchestrator);
        } else if ("file".equalsIgnoreCase(taskType)) {
            return executeFileTool(orchestrator, project, agent, task, context, lastFeedback, taskContext);
        } else if (taskType.startsWith("train_")) {
            return executeTrainingTool(orchestrator, agent, task);
        } else {
            // Default to LLM
            String agentType = (agent != null) ? agent.getType() : "general assistant";
            String prompt = "You are acting as a " + agentType + ".\n" +
                    "Context: " + context + "\n";
            if (lastFeedback != null) {
                prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
            }
            prompt += "Your task: " + task.getName() + "\n" +
                    "Provide your response below:";

            return sendRequest(orchestrator, prompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null, taskContext);
        }
    }

    private String executeGitTool(IProject project, Orchestrator orchestrator, String taskName) throws Exception {
        java.io.File workingDir = (project != null) ? project.getLocation().toFile() : null;
        Git gitSettings = orchestrator.getGit();
        if (gitSettings != null && gitSettings.getLocalPath() != null && !gitSettings.getLocalPath().isEmpty()) {
            java.io.File subDir = new java.io.File(workingDir, gitSettings.getLocalPath());
            if (subDir.exists() && subDir.isDirectory()) {
                workingDir = subDir;
            }
        }
        String branch = (gitSettings != null && gitSettings.getBranch() != null && !gitSettings.getBranch().isEmpty()) ? gitSettings.getBranch() : "master";

        StringBuilder output = new StringBuilder();
        String lowerTask = taskName.toLowerCase();

        // Simple heuristic mapping
        if (lowerTask.contains("add") || lowerTask.contains("commit")) {
            output.append(executeCommand(workingDir, "git", "add", ".")).append("\n");
            output.append(executeCommand(workingDir, "git", "commit", "-m", "AI Evolution step: " + taskName)).append("\n");
            if (project != null) project.refreshLocal(IResource.DEPTH_INFINITE, null);
        }

        if (lowerTask.contains("push")) {
            output.append(executeCommand(workingDir, "git", "push", "origin", branch)).append("\n");
        }

        if (output.length() == 0) {
            return "No git action mapped for: " + taskName;
        }
        return output.toString().trim();
    }

    private String executeMavenTool(IProject project, Orchestrator orchestrator) throws Exception {
        java.io.File workingDir = (project != null) ? project.getLocation().toFile() : null;
        List<String> mavenArgs = new ArrayList<>();

        String os = System.getProperty("os.name").toLowerCase();
        String mavenCmd = os.contains("win") ? "mvn.cmd" : "mvn";
        mavenArgs.add(mavenCmd);

        if (orchestrator.getMaven() != null) {
            if (!orchestrator.getMaven().getGoals().isEmpty()) {
                mavenArgs.addAll(orchestrator.getMaven().getGoals());
            } else {
                mavenArgs.add("clean");
                mavenArgs.add("install");
            }
            if (!orchestrator.getMaven().getProfiles().isEmpty()) {
                mavenArgs.add("-P" + String.join(",", orchestrator.getMaven().getProfiles()));
            }
        } else {
            mavenArgs.add("clean");
            mavenArgs.add("install");
        }

        String result = executeCommand(workingDir, mavenArgs.toArray(new String[0]));
        if (project != null) project.refreshLocal(IResource.DEPTH_INFINITE, null);
        return result;
    }

    private String executeTrainingTool(Orchestrator orchestrator, Agent agent, Task task) throws Exception {
        String taskType = task.getType();
        TrainingManager tm = TrainingManager.getInstance();
        String modelName = (agent != null) ? agent.getId() : orchestrator.getName();
        String testData = tm.getTestData().getOrDefault(taskType.substring(6), "Generic dataset for " + taskType);

        if ("train_nn".equalsIgnoreCase(taskType)) {
            return tm.trainNeuronNetwork(modelName, testData);
        } else if ("train_llm".equalsIgnoreCase(taskType)) {
            return tm.trainLLM(modelName, testData);
        } else if ("train_agent".equalsIgnoreCase(taskType)) {
            return tm.trainAgent(modelName, testData);
        }
        return "Unsupported training type: " + taskType;
    }

    private String executeFileTool(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback, TaskContext taskContext) throws Exception {
        String taskName = task.getName();
        String agentType = (agent != null) ? agent.getType() : "programmer";
        String prompt = "You are acting as a " + agentType + ".\n" +
                "Context: " + context + "\n";
        if (lastFeedback != null) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }
        prompt += "Your task: " + taskName + "\n" +
                "Provide ONLY the content of the file. Do not include any explanation or markdown code blocks unless they are part of the file content.";

        String content = sendRequest(orchestrator, prompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null, taskContext);

        // Clean up markdown if AI ignored "ONLY" instruction
        if (content.trim().startsWith("```")) {
            int firstNewline = content.indexOf("\n");
            int lastBackticks = content.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                content = content.substring(firstNewline + 1, lastBackticks).trim();
            }
        }

        String filePath = taskName.replaceAll("(?i)^Write\\s+", "").trim();
        if (filePath.isEmpty()) {
            throw new Exception("No file path specified in task name: " + taskName);
        }

        java.io.File file = new java.io.File(project.getLocation().toFile(), filePath);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        if (project != null) project.refreshLocal(IResource.DEPTH_INFINITE, null);
        return content; // Returning content so it can be used by subsequent tasks
    }

    private Agent findAgentForTask(Orchestrator orchestrator, Task task) {
        String target = task.getName().toLowerCase();
        return orchestrator.getAgents().stream()
                .filter(a -> target.contains(a.getType().toLowerCase()) || target.contains(a.getId().toLowerCase()))
                .findFirst()
                .orElse(orchestrator.getAgents().isEmpty() ? null : orchestrator.getAgents().get(0));
    }

    public String sendRequest(Orchestrator orchestrator, String prompt) throws Exception {
        return aiService.sendRequest(orchestrator, prompt);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl, TaskContext context) throws Exception {
        return aiService.sendRequest(orchestrator, prompt, proxyUrl, context);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl) throws Exception {
        return aiService.sendRequest(orchestrator, prompt, proxyUrl, null);
    }

    private String executeCommand(java.io.File workingDir, String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (workingDir != null) {
            processBuilder.directory(workingDir);
        }
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private JSONArray extractJsonArray(String response) throws Exception {
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start == -1 || end == -1 || end <= start) {
            throw new Exception("LLM failed to return a valid JSON array. Response: " + response);
        }
        return new JSONArray(response.substring(start, end + 1));
    }

    private JSONObject extractJsonObject(String response) throws Exception {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start == -1 || end == -1 || end <= start) {
            throw new Exception("LLM failed to return a valid JSON object. Response: " + response);
        }
        return new JSONObject(response.substring(start, end + 1));
    }

    public String[] getOllamaModels() {
        String baseUrl = (getOrchestrator(null) != null && getOrchestrator(null).getOllama() != null)
                         ? getOrchestrator(null).getOllama().getUrl() : "http://localhost:11434";
        return aiService.getOllamaModels(baseUrl);
    }
}
