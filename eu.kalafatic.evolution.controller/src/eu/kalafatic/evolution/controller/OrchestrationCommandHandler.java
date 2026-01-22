package eu.kalafatic.evolution.controller;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.view.PropertiesView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class OrchestrationCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IViewPart view = page.findView(PropertiesView.ID);
                if (view instanceof PropertiesView) {
                    EObject orchestrator = ((PropertiesView) view).getRootObject();
                    if (orchestrator != null) {
                        Job job = new OrchestrationJob("Orchestration", orchestrator);
                        job.schedule();
                    }
                }
            }
        }
        return null;
    }

    private class OrchestrationJob extends Job {
        private EObject orchestrator;

        public OrchestrationJob(String name, EObject orchestrator) {
            super(name);
            this.orchestrator = orchestrator;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Orchestration", 4);
            try {
                executeOrchestration(orchestrator, monitor);
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(null, "Orchestration Failed", e.getMessage());
                });
                return Status.CANCEL_STATUS;
            }
            monitor.done();
            return Status.OK_STATUS;
        }
    }

    private void executeOrchestration(EObject orchestrator, IProgressMonitor monitor) throws Exception {
        // Get AI Chat properties
        monitor.subTask("Getting AI Chat properties");
        EObject aiChat = (EObject) orchestrator.eGet(orchestrator.eClass().getEStructuralFeature("aiChat"));
        String aiChatUrl = (String) aiChat.eGet(aiChat.eClass().getEStructuralFeature("url"));
        String aiChatToken = (String) aiChat.eGet(aiChat.eClass().getEStructuralFeature("token"));
        String aiChatPrompt = (String) aiChat.eGet(aiChat.eClass().getEStructuralFeature("prompt"));
        monitor.worked(1);

        // Get Ollama properties
        monitor.subTask("Getting Ollama properties");
        EObject ollama = (EObject) orchestrator.eGet(orchestrator.eClass().getEStructuralFeature("ollama"));
        String ollamaUrl = (String) ollama.eGet(ollama.eClass().getEStructuralFeature("url"));
        String ollamaModel = (String) ollama.eGet(ollama.eClass().getEStructuralFeature("model"));
        monitor.worked(1);

        // Get Git properties
        monitor.subTask("Getting Git properties");
        EObject git = (EObject) orchestrator.eGet(orchestrator.eClass().getEStructuralFeature("git"));
        String repositoryUrl = (String) git.eGet(git.eClass().getEStructuralFeature("repositoryUrl"));
        monitor.worked(1);

        // Get Maven properties
        monitor.subTask("Getting Maven properties");
        EObject maven = (EObject) orchestrator.eGet(orchestrator.eClass().getEStructuralFeature("maven"));
        String goals = String.join(" ", (java.util.List<String>) maven.eGet(maven.eClass().getEStructuralFeature("goals")));
        monitor.worked(1);

        // 1. AI Chat Request
        monitor.subTask("Sending AI Chat Request");
        String aiResponse = sendAiChatRequest(aiChatUrl, aiChatToken, aiChatPrompt);
        System.out.println("AI Chat Response: " + aiResponse);
        monitor.worked(1);

        // 2. Ollama LLM Solution
        monitor.subTask("Sending Ollama Request");
        String llmSolution = sendOllamaRequest(ollamaUrl, ollamaModel, aiResponse);
        System.out.println("LLM Solution: " + llmSolution);
        monitor.worked(1);

        // 3. Git Commit
        monitor.subTask("Committing to Git");
        // Sanitize the commit message to prevent it from being interpreted as a command-line option.
        if (llmSolution.startsWith("-")) {
            llmSolution = " " + llmSolution;
        }
        executeCommand("git", "add", ".");
        executeCommand("git", "commit", "-m", llmSolution);
        System.out.println("Git commit successful.");
        monitor.worked(1);

        // 4. Maven Build
        monitor.subTask("Running Maven Build");
        String mavenBuildOutput = executeCommand("mvn", goals);
        System.out.println("Maven Build Output: " + mavenBuildOutput);
        monitor.worked(1);
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
        String json = jsonObject.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getString("solution");
    }
}
