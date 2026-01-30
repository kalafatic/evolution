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
import eu.kalafatic.evolution.controller.engine.NeuronEngine;
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

    private static final int MAX_RETRIES = 3;

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

        // 1. Task Decomposition
        OrchestrationStatusManager.getInstance().updateStatus(id, 0.05, "Decomposing tasks...");
        monitor.subTask("Planning workflow...");
        List<Task> tasks = decomposeTasks(orchestrator);
        orchestrator.getTasks().clear();
        orchestrator.getTasks().addAll(tasks);
        monitor.worked(10);

        // 2. Execution Loop with Evaluation and Retry
        String handOffContext = orchestrator.getAiChat().getPrompt();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            String taskName = task.getName();
            double baseProgress = 0.1 + (0.8 * (double) i / taskCount);

            task.setStatus(TaskStatus.RUNNING);
            Agent agent = findAgentForTask(orchestrator, task);

            boolean taskSuccess = false;
            String lastFeedback = null;

            for (int retry = 0; retry < MAX_RETRIES; retry++) {
                String statusMsg = "Executing " + taskName + (retry > 0 ? " (Retry " + retry + ")" : "");
                OrchestrationStatusManager.getInstance().updateStatus(id, baseProgress, statusMsg);
                monitor.subTask(statusMsg);

                String response = executeTask(orchestrator, project, agent, task, handOffContext, lastFeedback);
                task.setResponse(response);

                // 3. Evaluation
                monitor.subTask("Evaluating: " + taskName);
                String evaluationResult = evaluateTask(orchestrator, task, handOffContext);
                JSONObject evalJson = parseEvaluation(evaluationResult);

                if (evalJson.optBoolean("success", false)) {
                    task.setStatus(TaskStatus.DONE);
                    task.setFeedback("Success: " + evalJson.optString("comment", "Task completed."));
                    taskSuccess = true;
                    break;
                } else {
                    lastFeedback = evalJson.optString("feedback", "Task failed validation.");
                    task.setFeedback("Failure: " + lastFeedback);
                }
            }

            if (!taskSuccess) {
                task.setStatus(TaskStatus.FAILED);
                throw new Exception("Task failed after " + MAX_RETRIES + " attempts: " + taskName + ". Feedback: " + task.getFeedback());
            }

            // Hand-off: output of this task becomes context for the next
            handOffContext += "\n\nPrevious task [" + taskName + "] output:\n" + task.getResponse();
            monitor.worked(80 / taskCount);
        }

        OrchestrationStatusManager.getInstance().updateStatus(id, 1.0, "Completed");
        monitor.done();
    }

    private List<Task> decomposeTasks(Orchestrator orchestrator) throws Exception {
        String plannerPrompt = "You are a workflow planner for an agentic system. " +
                "Decompose the user request into a sequence of atomic, specialized tasks.\n" +
                "Available task types:\n" +
                "- 'llm': For reasoning, planning, or general text generation.\n" +
                "- 'file': For writing or creating files (e.g., Java source code, POM, README). Task name should be 'Write <path/to/file>'.\n" +
                "- 'git': For version control actions (add, commit, push).\n" +
                "- 'maven': For building, testing, or packaging the project.\n\n" +
                "Output MUST be a valid JSON array of objects. Schema:\n" +
                "[ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\" } ]\n\n" +
                "Request: " + orchestrator.getAiChat().getPrompt();

        String response = sendRequest(orchestrator, plannerPrompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null);
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

    private String evaluateTask(Orchestrator orchestrator, Task task, String context) throws Exception {
        String evalPrompt = "You are an AI Critic. Evaluate if the following task was successfully completed.\n\n" +
                "TASK GOAL: " + task.getName() + "\n" +
                "AGENT OUTPUT: " + task.getResponse() + "\n" +
                "OVERALL CONTEXT: " + context + "\n\n" +
                "CRITERIA:\n" +
                "1. Does the output directly address the goal?\n" +
                "2. Is the output technically sound and complete?\n\n" +
                "Output MUST be a valid JSON object. Schema:\n" +
                "{ \"success\": boolean, \"feedback\": \"Detailed explanation of why it failed and how to fix it\", \"comment\": \"Brief success message\" }";

        return sendRequest(orchestrator, evalPrompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null);
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
    private String executeTask(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback) throws Exception {
        String taskType = task.getType();

        if ("git".equalsIgnoreCase(taskType)) {
            return executeGitTool(project, orchestrator, task.getName());
        } else if ("maven".equalsIgnoreCase(taskType)) {
            return executeMavenTool(project, orchestrator);
        } else if ("file".equalsIgnoreCase(taskType)) {
            return executeFileTool(orchestrator, project, agent, task, context, lastFeedback);
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

            return sendRequest(orchestrator, prompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null);
        }
    }

    private String executeGitTool(IProject project, Orchestrator orchestrator, String taskName) throws Exception {
        java.io.File workingDir = (project != null) ? project.getLocation().toFile() : null;
        Git gitSettings = orchestrator.getGit();
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

        if (orchestrator.getMaven() != null && !orchestrator.getMaven().getGoals().isEmpty()) {
            mavenArgs.addAll(orchestrator.getMaven().getGoals());
        } else {
            mavenArgs.add("clean");
            mavenArgs.add("install");
        }

        String result = executeCommand(workingDir, mavenArgs.toArray(new String[0]));
        if (project != null) project.refreshLocal(IResource.DEPTH_INFINITE, null);
        return result;
    }

    private String executeFileTool(Orchestrator orchestrator, IProject project, Agent agent, Task task, String context, String lastFeedback) throws Exception {
        String taskName = task.getName();
        String agentType = (agent != null) ? agent.getType() : "programmer";
        String prompt = "You are acting as a " + agentType + ".\n" +
                "Context: " + context + "\n";
        if (lastFeedback != null) {
            prompt += "PREVIOUS ATTEMPT FAILED. Feedback: " + lastFeedback + "\nPlease correct your approach.\n";
        }
        prompt += "Your task: " + taskName + "\n" +
                "Provide ONLY the content of the file. Do not include any explanation or markdown code blocks unless they are part of the file content.";

        String content = sendRequest(orchestrator, prompt, (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null);

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
        return sendRequest(orchestrator, prompt, null);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl) throws Exception {
        if (orchestrator.getOllama() != null && orchestrator.getOllama().getUrl() != null && !orchestrator.getOllama().getUrl().isEmpty()) {
            return sendOllamaRequest(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel(), prompt, proxyUrl);
        } else if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null && !orchestrator.getAiChat().getUrl().isEmpty()) {
            return sendAiChatRequest(orchestrator.getAiChat().getUrl(), orchestrator.getAiChat().getToken(), prompt, proxyUrl);
        } else if (orchestrator.getNeuronAI() != null) {
            String url = orchestrator.getNeuronAI().getUrl();
            if (url == null || url.isEmpty() || url.equalsIgnoreCase("local")) {
                return new NeuronEngine().runModel(orchestrator.getNeuronAI().getType(), orchestrator.getNeuronAI().getModel(), prompt);
            }
            return sendNeuronAIRequest(url, orchestrator.getNeuronAI().getModel(), prompt, proxyUrl);
        }
        throw new Exception("No LLM service configured (Ollama, AI Chat or Neuron AI)");
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

    private HttpClient getClient(String proxyUrl) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (proxyUrl != null && !proxyUrl.isEmpty()) {
            try {
                URI proxyUri = URI.create(proxyUrl);
                builder.proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return builder.build();
    }

    private String sendAiChatRequest(String url, String token, String prompt, String proxyUrl) throws Exception {
        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("prompt", prompt);
        String json = jsonObject.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body()).getString("response");
    }

    private String sendOllamaRequest(String url, String model, String prompt, String proxyUrl) throws Exception {
        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("prompt", prompt);
        jsonObject.put("stream", false);
        String json = jsonObject.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.has("response") ? jsonResponse.getString("response") : jsonResponse.getString("solution");
    }

    private String sendNeuronAIRequest(String url, String model, String prompt, String proxyUrl) throws Exception {
        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("prompt", prompt);
        String json = jsonObject.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.optString("response", jsonResponse.optString("output", "No response from Neuron AI"));
    }

    public String[] getOllamaModels() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray models = jsonResponse.getJSONArray("models");
            List<String> modelNames = new ArrayList<>();
            for (int i = 0; i < models.length(); i++) {
                modelNames.add(models.getJSONObject(i).getString("name"));
            }
            return modelNames.toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }
}
