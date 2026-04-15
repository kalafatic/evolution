package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;

/**
 * Service to handle AI requests, decoupled from Eclipse UI handlers.
 * Refactored to delegate to LlmRouter for unified routing logic.
 */
public class AiService {

    private final LlmRouter llmRouter = new LlmRouter();

    public String sendRequest(Orchestrator orchestrator, String prompt) throws Exception {
        return sendRequest(orchestrator, prompt, null, null);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, TaskContext context) throws Exception {
        return sendRequest(orchestrator, prompt, null, context);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl, TaskContext context) throws Exception {
        float temperature = 0.7f;
        try {
            if (orchestrator != null && orchestrator.getLlm() != null) {
                temperature = orchestrator.getLlm().getTemperature();
            }
        } catch (Throwable t) {
            // Ignore and use default
        }

        // Unified routing via LlmRouter
        return llmRouter.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    // Keep getOllamaModels for UI/Compatibility if needed, but it should ideally be moved too.
    // However, LlmRouter doesn't have it yet.
    public String[] getOllamaModels(String baseUrl) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/tags"))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response.body());
            org.json.JSONArray models = jsonResponse.getJSONArray("models");
            java.util.List<String> modelNames = new java.util.ArrayList<>();
            for (int i = 0; i < models.length(); i++) {
                modelNames.add(models.getJSONObject(i).getString("name"));
            }
            return modelNames.toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }
}
