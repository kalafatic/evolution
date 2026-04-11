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
 * Gemini LLM provider implementation.
 */
public class GeminiProvider implements ILlmProvider {

    private static final String DEFAULT_GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1/models/%s:generateContent";

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String remoteModelName = orchestrator.getRemoteModel();
        String token = null;
        String apiUrl = null;
        String model = null;

        // Try to find custom provider in orchestrator
        eu.kalafatic.evolution.model.orchestration.AIProvider customProvider = null;
        if (orchestrator.getAiProviders() != null) {
            customProvider = orchestrator.getAiProviders().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(remoteModelName))
                    .findFirst().orElse(null);
        }

        if (customProvider != null) {
            token = eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().getToken(customProvider);
            apiUrl = customProvider.getUrl();
            model = customProvider.getDefaultModel();
            if (model == null || model.isEmpty()) model = remoteModelName;
        } else {
            token = orchestrator.getOpenAiToken(); // Using OpenAiToken as general API key
            ProviderConfig config = AiProviders.PROVIDERS.get(remoteModelName != null ? remoteModelName.toLowerCase() : "gemini");
            apiUrl = (config != null) ? config.getUrl() : DEFAULT_GEMINI_URL_TEMPLATE;
            model = (config != null) ? config.getDefaultModel() : remoteModelName;
        }

        if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null && !orchestrator.getAiChat().getUrl().isEmpty()) {
            apiUrl = orchestrator.getAiChat().getUrl();
        }

        if (model == null || model.isEmpty() || model.equalsIgnoreCase("gemini")) {
            model = "gemini-1.5-pro";
        }

        if (token == null || token.isEmpty() || "YOUR_API_KEY".equals(token)) {
            if (context != null) {
                try {
                    token = context.requestToken(remoteModelName != null ? remoteModelName : "Gemini").get();
                    orchestrator.setOpenAiToken(token);
                } catch (Exception e) {
                    throw new Exception("Failed to obtain token: " + e.getMessage());
                }
            } else {
                throw new Exception("API Token is not configured (use OpenAI Token field)");
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

        // Gemini payload structure:
        // {
        //   "contents": [{
        //     "parts":[{
        //       "text": "prompt"
        //     }]
        //   }],
        //   "generationConfig": {
        //     "temperature": 0.7
        //   }
        // }
        JSONObject jsonObject = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        jsonObject.put("contents", contents);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", temperature);
        jsonObject.put("generationConfig", generationConfig);

        String json = jsonObject.toString();

        if (apiUrl.contains("%s")) {
            apiUrl = String.format(apiUrl, model);
        }
        apiUrl = apiUrl + (apiUrl.contains("?") ? "&" : "?") + "key=" + token;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Gemini error: " + response.statusCode() + " - " + response.body());
        }

        // Gemini response structure:
        // {
        //   "candidates": [{
        //     "content": {
        //       "parts": [{
        //         "text": "..."
        //       }]
        //     }
        //   }]
        // }
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }
}
