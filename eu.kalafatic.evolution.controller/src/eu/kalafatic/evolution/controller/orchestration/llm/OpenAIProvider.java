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
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;

/**
 * OpenAI-compatible LLM provider implementation.
 */
public class OpenAIProvider implements ILlmProvider {

    private static final String DEFAULT_OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String remoteModelName = orchestrator.getRemoteModel();

        // Use generalized resolution mechanism
        eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved =
                eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().resolve(orchestrator, remoteModelName);

        String token = (resolved != null) ? resolved.token : null;
        String apiUrl = (resolved != null && resolved.url != null) ? resolved.url : DEFAULT_OPENAI_URL;
        String model = (resolved != null && resolved.model != null) ? resolved.model : orchestrator.getOpenAiModel();

        if (token == null || token.isEmpty() || "YOUR_API_KEY".equals(token)) {
            if (context != null) {
                try {
                    token = context.requestToken(remoteModelName != null ? remoteModelName : "OpenAI").get();
                    orchestrator.setOpenAiToken(token);
                } catch (Exception e) {
                    throw new Exception("Failed to obtain token: " + e.getMessage());
                }
            } else {
                throw new Exception("OpenAI-compatible token is not configured (use OpenAI Token field)");
            }
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

        if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null && !orchestrator.getAiChat().getUrl().isEmpty()) {
            apiUrl = orchestrator.getAiChat().getUrl();
            if (!apiUrl.contains("/chat/completions")) {
                apiUrl = apiUrl + (apiUrl.endsWith("/") ? "" : "/") + "chat/completions";
            }
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Remote AI error (" + (remoteModelName != null ? remoteModelName : "openai") + "): " + response.statusCode() + " - " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }
}
