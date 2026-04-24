package eu.kalafatic.evolution.controller.manager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pure Java 21+ Ollama Chat - Centralized service logic.
 *
 * @evo:1:1 reason=cache-and-refactor-ollama-service
 */
public class OllamaService {

    private final String baseUrl;
    private String model;
    private final HttpClient httpClient;
    private final List<Message> messages = new ArrayList<>();

    private List<OllamaModel> cachedModels = null;
    private long lastModelRefresh = 0;
    private static final long CACHE_TTL = Duration.ofMinutes(5).toMillis();

    // Advanced options
    private float temperature = 0.7f;
    private int numPredict = 1024;
    private float topP = 0.9f;
    private int topK = 40;
    private float repeatPenalty = 1.1f;

    public OllamaService(String url, String model) {
        this.baseUrl = (url != null && !url.isEmpty()) ? url : "http://localhost:11434";
        this.model = (model != null && !model.isEmpty()) ? model : "llama3.2:3b";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Optional system prompt
        messages.add(new Message("system", "You are a concise, helpful Java programming assistant."));
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        if (model != null && !model.isEmpty()) {
            this.model = model;
        }
    }

    public OllamaService setTemperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public OllamaService setNumPredict(int numPredict) {
        this.numPredict = numPredict;
        return this;
    }

    public OllamaService setTopP(float topP) {
        this.topP = topP;
        return this;
    }

    public OllamaService setTopK(int topK) {
        this.topK = topK;
        return this;
    }

    public OllamaService setRepeatPenalty(float repeatPenalty) {
        this.repeatPenalty = repeatPenalty;
        return this;
    }

    /**
     * Pings the Ollama server to check if it is reachable.
     * @return true if reachable, false otherwise.
     */
    public boolean ping() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.baseUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetches the Ollama version.
     * @return version string or "Unknown"
     */
    public String getVersion() {
        try {
            String versionUrl = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/version";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(versionUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject obj = new JSONObject(response.body());
                return obj.optString("version", "Unknown");
            }
        } catch (Exception e) {
            // silent fail
        }
        return "Unknown";
    }

    public String chat(String userInput) throws Exception {
        messages.add(new Message("user", userInput));

        String chatUrl = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/chat";
        String jsonBody = buildChatJsonRequest(false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chatUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama error: " + response.statusCode() + " - " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        String answer = jsonResponse.getJSONObject("message").getString("content");

        messages.add(new Message("assistant", answer));

        return answer;
    }

    /**
     * Sends a generation request (api/generate)
     */
    public String generate(String prompt) throws Exception {
        String genUrl = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/generate";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", this.model);
        jsonObject.put("prompt", prompt);
        jsonObject.put("stream", false);
        JSONObject options = new JSONObject();
        options.put("temperature", this.temperature);
        options.put("num_predict", this.numPredict);
        jsonObject.put("options", options);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(genUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama generate error: " + response.statusCode() + " - " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.has("response") ? jsonResponse.getString("response") : jsonResponse.optString("solution", "");
    }

    private String buildChatJsonRequest(boolean stream) {
        JSONObject json = new JSONObject();
        json.put("model", this.model);
        json.put("stream", stream);

        JSONObject options = new JSONObject();
        options.put("temperature", this.temperature);
        options.put("num_predict", this.numPredict);
        options.put("top_p", this.topP);
        options.put("top_k", this.topK);
        options.put("repeat_penalty", this.repeatPenalty);
        json.put("options", options);

        JSONArray msgs = new JSONArray();
        for (Message msg : messages) {
            JSONObject m = new JSONObject();
            m.put("role", msg.role);
            m.put("content", msg.content);
            msgs.put(m);
        }
        json.put("messages", msgs);

        return json.toString();
    }

    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Pulls a model from Ollama.
     */
    public void pullModel(String modelName, Consumer<ProgressUpdate> progressCallback) throws Exception {
        String pullUrl = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/pull";
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", modelName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofHours(1))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofLines());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama pull error: " + response.statusCode());
        }

        try (java.util.stream.Stream<String> lines = response.body()) {
            lines.forEach(line -> {
                if (line != null && !line.isBlank()) {
                    JSONObject obj = new JSONObject(line);
                    String status = obj.optString("status");
                    long completed = obj.optLong("completed", 0);
                    long total = obj.optLong("total", 0);
                    progressCallback.accept(new ProgressUpdate(status, completed, total));
                }
            });
        }
        // Force refresh models list after pulling
        refreshModels();
    }

    public static record ProgressUpdate(String status, long completed, long total) {}

    /**
     * Fetches the list of models from the Ollama API, with caching.
     * @return List of OllamaModel objects.
     */
    public List<OllamaModel> loadModels() {
        if (cachedModels != null && (System.currentTimeMillis() - lastModelRefresh < CACHE_TTL)) {
            return cachedModels;
        }
        return refreshModels();
    }

    /**
     * Forces a refresh of the models list.
     */
    public List<OllamaModel> refreshModels() {
        List<OllamaModel> result = new ArrayList<>();
        try {
            String tagsUrl = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/tags";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tagsUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject obj = new JSONObject(response.body());
                JSONArray models = obj.getJSONArray("models");
                for (int i = 0; i < models.length(); i++) {
                    JSONObject m = models.getJSONObject(i);
                    String name = m.getString("name");
                    long size = m.optLong("size", 0);
                    result.add(new OllamaModel(name, size));
                }
                this.cachedModels = Collections.unmodifiableList(result);
                this.lastModelRefresh = System.currentTimeMillis();
            }
        } catch (Exception e) {
            // silent fail or return empty
        }
        return result;
    }

    // Inner class for messages
    public static class Message {
        String role;
        String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
