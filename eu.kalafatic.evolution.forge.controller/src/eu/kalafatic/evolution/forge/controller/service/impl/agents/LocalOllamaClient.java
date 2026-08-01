package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.forge.controller.service.OllamaService;

public class LocalOllamaClient implements OllamaService {
    private final String baseUrl;
    private String modelName;

    public LocalOllamaClient(String baseUrl, String defaultModel) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.modelName = (defaultModel != null && !defaultModel.isEmpty()) ? defaultModel : "llama3.2:3b";
    }

    @Override
    public String generate(String prompt) throws Exception {
        // Resolve model name if needed
        String activeModel = resolveActiveModel();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JSONObject payload = new JSONObject();
        payload.put("model", activeModel);
        payload.put("prompt", prompt);
        payload.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JSONObject resObj = new JSONObject(response.body());
            return resObj.optString("response", "");
        } else {
            throw new RuntimeException("Ollama generate error: " + response.statusCode() + " - " + response.body());
        }
    }

    @Override
    public String chat(String message, String sessionId) throws Exception {
        return generate(message);
    }

    @Override
    public void pullModel(String modelName, Consumer<Double> progressCallback) throws Exception {
        // Simple mock/noop or pull call if required
    }

    @Override
    public void setModel(String modelName) {
        this.modelName = modelName;
    }

    private String resolveActiveModel() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject obj = new JSONObject(response.body());
                JSONArray models = obj.getJSONArray("models");
                for (int i = 0; i < models.length(); i++) {
                    String name = models.getJSONObject(i).getString("name");
                    if (name.equalsIgnoreCase(modelName) || name.startsWith(modelName + ":")) {
                        return name;
                    }
                }
                // fallback to first standard non-evo model
                for (int i = 0; i < models.length(); i++) {
                    String name = models.getJSONObject(i).getString("name");
                    if (!name.toLowerCase().contains("evo")) {
                        return name;
                    }
                }
                if (models.length() > 0) {
                    return models.getJSONObject(0).getString("name");
                }
            }
        } catch (Exception e) {
            // Ignore and use current modelName
        }
        return modelName;
    }
}
