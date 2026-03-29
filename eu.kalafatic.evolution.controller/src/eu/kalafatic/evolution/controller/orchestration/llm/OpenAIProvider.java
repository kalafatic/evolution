package eu.kalafatic.evolution.controller.orchestration.llm;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * OpenAI LLM provider implementation.
 */
public class OpenAIProvider implements ILlmProvider {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl) throws Exception {
        String token = orchestrator.getOpenAiToken();
        String model = orchestrator.getOpenAiModel();

        if (token == null || token.isEmpty()) {
            throw new Exception("OpenAI token is not configured");
        }

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

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("temperature", temperature);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);
        jsonObject.put("messages", messages);

        String json = jsonObject.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("OpenAI error: " + response.statusCode() + " - " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }
}
