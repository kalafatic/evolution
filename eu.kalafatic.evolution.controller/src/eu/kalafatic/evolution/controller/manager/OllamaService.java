package eu.kalafatic.evolution.controller.manager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pure Java 21+ Ollama Chat - ZERO external dependencies
 */
public class OllamaService {

    private final String url;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final List<Message> messages = new ArrayList<>();

    // Advanced options
    private float temperature = 0.7f;
    private int numPredict = 1024;
    private float topP = 0.9f;
    private int topK = 40;
    private float repeatPenalty = 1.1f;

    public OllamaService(String url, String model) {
        this.baseUrl = url != null ? url : "http://localhost:11434";
        if (this.baseUrl.endsWith("/api/chat")) {
            this.url = this.baseUrl;
        } else {
            this.url = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/chat";
        }
        this.model = model != null ? model : "llama3.2:3b";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Optional system prompt
        messages.add(new Message("system", "You are a concise, helpful Java programming assistant."));
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
                String body = response.body();
                // Simple parsing for {"version": "0.1.2"}
                int start = body.indexOf("\"version\":\"");
                if (start != -1) {
                    start += 11;
                    int end = body.indexOf("\"", start);
                    if (end != -1) {
                        return body.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            // silent fail
        }
        return "Unknown";
    }

    public String chat(String userInput) throws Exception {
        messages.add(new Message("user", userInput));

        String jsonBody = buildJsonRequest(false); // false = non-streaming for simplicity

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama error: " + response.statusCode() + " - " + response.body());
        }

        String responseBody = response.body();

        // Simple parsing: extract content between "content\":\" and next \"
        String answer = extractContent(responseBody);

        messages.add(new Message("assistant", answer));

        return answer;
    }

    private String buildJsonRequest(boolean stream) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(this.model).append("\",");
        sb.append("\"stream\":").append(stream).append(",");

        // Add options
        sb.append("\"options\":{");
        sb.append("\"temperature\":").append(this.temperature).append(",");
        sb.append("\"num_predict\":").append(this.numPredict).append(",");
        sb.append("\"top_p\":").append(this.topP).append(",");
        sb.append("\"top_k\":").append(this.topK).append(",");
        sb.append("\"repeat_penalty\":").append(this.repeatPenalty);
        sb.append("},");

        sb.append("\"messages\":[");

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"role\":\"").append(msg.role)
              .append("\",\"content\":\"")
              .append(escapeJson(msg.content))
              .append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String extractContent(String jsonResponse) {
        // Very simple parser for non-streaming response
        int start = jsonResponse.indexOf("\"content\":\"");
        if (start == -1) return "Error parsing response";

        start += 11; // length of "content\":\"
        int end = -1;

        // Find next unescaped quote
        for (int i = start; i < jsonResponse.length(); i++) {
            if (jsonResponse.charAt(i) == '\"') {
                if (i > 0 && jsonResponse.charAt(i - 1) == '\\') {
                    // check if backslash is escaped
                    int backslashCount = 0;
                    for (int j = i - 1; j >= 0 && jsonResponse.charAt(j) == '\\'; j--) {
                        backslashCount++;
                    }
                    if (backslashCount % 2 == 0) {
                        end = i;
                        break;
                    }
                } else {
                    end = i;
                    break;
                }
            }
        }

        if (end == -1) return "Error parsing response";

        String content = jsonResponse.substring(start, end);
        // Replace escaped characters
        return content.replace("\\n", "\n")
                      .replace("\\\"", "\"")
                      .replace("\\\\", "\\")
                      .replace("\\t", "\t")
                      .replace("\\r", "\r");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Fetches the list of models from the Ollama API.
     * @return List of OllamaModel objects.
     */
    public List<OllamaModel> loadModels() {
        List<OllamaModel> result = new ArrayList<>();
        try {
            URL url = new URL(this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();

            JSONObject obj = new JSONObject(json.toString());
            JSONArray models = obj.getJSONArray("models");
            for (int i = 0; i < models.length(); i++) {
                JSONObject m = models.getJSONObject(i);
                String name = m.getString("name");
                long size = m.optLong("size", 0);
                result.add(new OllamaModel(name, size));
            }
        } catch (Exception e) {
            // silent fail or log
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
