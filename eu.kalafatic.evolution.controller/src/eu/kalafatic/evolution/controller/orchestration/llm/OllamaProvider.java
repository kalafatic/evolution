package eu.kalafatic.evolution.controller.orchestration.llm;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Ollama LLM provider implementation.
 */
public class OllamaProvider implements ILlmProvider {

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl) throws Exception {
        if (orchestrator.getOllama() == null || orchestrator.getOllama().getUrl() == null || orchestrator.getOllama().getUrl().isEmpty()) {
            throw new Exception("Ollama is not configured");
        }

        String baseUrl = orchestrator.getOllama().getUrl();
        String model = orchestrator.getOllama().getModel();

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));
        if (proxyUrl != null && !proxyUrl.isEmpty()) {
            try {
                URI proxyUri = URI.create(proxyUrl);
                builder.proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
            } catch (Exception e) {
                // Ignore proxy error
            }
        }
        HttpClient client = builder.build();

        String fullUrl = baseUrl;
        if (!fullUrl.contains("/api/")) {
            fullUrl = fullUrl + (fullUrl.endsWith("/") ? "" : "/") + "api/generate";
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("prompt", prompt);
        jsonObject.put("stream", false);
        JSONObject options = new JSONObject();
        options.put("temperature", temperature);
        jsonObject.put("options", options);
        String json = jsonObject.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Ollama error: " + response.statusCode() + " - " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.has("response") ? jsonResponse.getString("response") : jsonResponse.getString("solution");
    }
}
