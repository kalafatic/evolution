package eu.kalafatic.evolution.controller;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import eu.kalafatic.evolution.model.orchestration.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Orchestrator orchestrator = getOrchestrator(event);
        if (orchestrator != null) {
            Job job = new OrchestrationJob("Orchestration", orchestrator);
            job.schedule();
        }
        return null;
    }

    private class OrchestrationJob extends Job {
        private Orchestrator orchestrator;

        public OrchestrationJob(String name, Orchestrator orchestrator) {
            super(name);
            this.orchestrator = orchestrator;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                executeOrchestration(orchestrator, monitor);
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

    private void executeOrchestration(Orchestrator orchestrator, IProgressMonitor monitor) throws Exception {
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

        // 2. Execution Loop with Specialized Roles and Hand-off
        String handOffContext = orchestrator.getAiChat().getPrompt();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            String taskName = task.getName();
            double progress = 0.1 + (0.7 * (double) i / taskCount);
            OrchestrationStatusManager.getInstance().updateStatus(id, progress, "Executing " + taskName);
            monitor.subTask("Agent working on: " + taskName);

            task.setStatus(TaskStatus.RUNNING);

            Agent agent = findAgentForTask(orchestrator, task);
            String response = executeTask(orchestrator, agent, task, handOffContext);

            task.setResponse(response);
            task.setStatus(TaskStatus.DONE);

            // Hand-off: output of this task becomes context for the next
            handOffContext += "\n\nPrevious task [" + taskName + "] output:\n" + response;

            monitor.worked(70 / taskCount);
        }

        // 3. Final Integration (Git & Maven)
        OrchestrationStatusManager.getInstance().updateStatus(id, 0.85, "Git Commit...");
        monitor.subTask("Committing changes");
        String finalOutput = tasks.isEmpty() ? "No tasks completed" : tasks.get(tasks.size() - 1).getResponse();

        // Sanitize commit message
        String commitMsg = "AI Evolution: " + finalOutput.replaceAll("\\r|\\n", " ");
        if (commitMsg.length() > 100) commitMsg = commitMsg.substring(0, 97) + "...";
        if (commitMsg.startsWith("-")) commitMsg = " " + commitMsg;

        executeCommand("git", "add", ".");
        executeCommand("git", "commit", "-m", commitMsg);
        monitor.worked(10);

        OrchestrationStatusManager.getInstance().updateStatus(id, 0.95, "Maven Build...");
        monitor.subTask("Running build");
        List<String> mavenArgs = new ArrayList<>();
        mavenArgs.add("mvn");
        mavenArgs.addAll(orchestrator.getMaven().getGoals());
        executeCommand(mavenArgs.toArray(new String[0]));
        monitor.worked(10);

        OrchestrationStatusManager.getInstance().updateStatus(id, 1.0, "Completed");
        monitor.done();
    }

    private List<Task> decomposeTasks(Orchestrator orchestrator) throws Exception {
        String plannerPrompt = "Decompose the following request into a sequence of specialized tasks for an AI agent team. " +
                "Return ONLY a JSON array of objects with 'id', 'name', and 'role' fields. Do not include any other text.\n" +
                "Request: " + orchestrator.getAiChat().getPrompt();

        String response = sendRequest(orchestrator, plannerPrompt);

        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start == -1 || end == -1 || end <= start) {
            throw new Exception("LLM failed to return a valid JSON array of tasks. Response was: " + response);
        }
        String jsonStr = response.substring(start, end + 1);
        JSONArray jsonArray = new JSONArray(jsonStr);

        List<Task> tasks = new ArrayList<>();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Task task = factory.createTask();
            task.setId(obj.optString("id", "task" + i));
            task.setName(obj.optString("name", "Task " + i));
            // Role info can be stored in name for now as Task model lacks 'role' attribute
            tasks.add(task);
        }
        return tasks;
    }

    private Agent findAgentForTask(Orchestrator orchestrator, Task task) {
        String target = task.getName().toLowerCase();
        return orchestrator.getAgents().stream()
                .filter(a -> target.contains(a.getType().toLowerCase()) || target.contains(a.getId().toLowerCase()))
                .findFirst()
                .orElse(orchestrator.getAgents().isEmpty() ? null : orchestrator.getAgents().get(0));
    }

    private String executeTask(Orchestrator orchestrator, Agent agent, Task task, String context) throws Exception {
        String agentType = (agent != null) ? agent.getType() : "general assistant";
        String prompt = "You are acting as a " + agentType + ".\n" +
                "Context: " + context + "\n\n" +
                "Your task: " + task.getName() + "\n" +
                "Provide your response below:";

        return sendRequest(orchestrator, prompt);
    }

    private String sendRequest(Orchestrator orchestrator, String prompt) throws Exception {
        if (orchestrator.getOllama() != null && orchestrator.getOllama().getUrl() != null && !orchestrator.getOllama().getUrl().isEmpty()) {
            return sendOllamaRequest(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel(), prompt);
        } else if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null && !orchestrator.getAiChat().getUrl().isEmpty()) {
            return sendAiChatRequest(orchestrator.getAiChat().getUrl(), orchestrator.getAiChat().getToken(), prompt);
        }
        throw new Exception("No LLM service configured (Ollama or AI Chat)");
    }

    private String executeCommand(String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private String sendAiChatRequest(String url, String token, String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
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
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getString("response");
    }

    private String sendOllamaRequest(String url, String model, String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
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
        if (jsonResponse.has("response")) {
            return jsonResponse.getString("response");
        } else if (jsonResponse.has("solution")) {
            return jsonResponse.getString("solution");
        }
        throw new Exception("Unexpected Ollama response format: " + response.body());
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
