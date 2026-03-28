package eu.kalafatic.evolution.controller.orchestration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.engine.NeuronEngine;

/**
 * Service to handle AI requests, decoupled from Eclipse UI handlers.
 */
public class AiService {

    public String sendRequest(Orchestrator orchestrator, String prompt) throws Exception {
        return sendRequest(orchestrator, prompt, null);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl) throws Exception {
        float temperature = 0.7f;
        if (orchestrator.getLlm() != null) {
            temperature = orchestrator.getLlm().getTemperature();
        }

        if (orchestrator.getOllama() != null && orchestrator.getOllama().getUrl() != null && !orchestrator.getOllama().getUrl().isEmpty()) {
            return sendOllamaRequest(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel(), prompt, proxyUrl, temperature);
        } else if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null && !orchestrator.getAiChat().getUrl().isEmpty()) {
            return sendAiChatRequest(orchestrator.getAiChat().getUrl(), orchestrator.getAiChat().getToken(), prompt, proxyUrl, temperature);
        } else if (orchestrator.getNeuronAI() != null) {
            String url = orchestrator.getNeuronAI().getUrl();
            if (url == null || url.isEmpty() || url.equalsIgnoreCase("local")) {
                return new NeuronEngine().runModel(orchestrator.getNeuronAI().getType(), orchestrator.getNeuronAI().getModel(), prompt);
            }
            return sendNeuronAIRequest(url, orchestrator.getNeuronAI().getModel(), prompt, proxyUrl);
        }
        throw new Exception("No LLM service configured (Ollama, AI Chat or Neuron AI)");
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

    private String sendAiChatRequest(String url, String token, String prompt, String proxyUrl, float temperature) throws Exception {
        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("prompt", prompt);
        jsonObject.put("temperature", temperature);
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

    private String sendOllamaRequest(String url, String model, String prompt, String proxyUrl, float temperature) throws Exception {
        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("prompt", prompt);
        jsonObject.put("stream", false);
        JSONObject options = new JSONObject();
        options.put("temperature", temperature);
        jsonObject.put("options", options);
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

    public String[] getOllamaModels(String baseUrl) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/tags"))
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
