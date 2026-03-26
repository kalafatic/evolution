package eu.kalafatic.evolution.controller.manager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java 21+ Ollama Chat - ZERO external dependencies
 */
public class OllamaService {

    private final String url;
    private final String model;
    private final HttpClient httpClient;
    private final List<Message> messages = new ArrayList<>();

    public OllamaService(String url, String model) {
        String baseUrl = url != null ? url : "http://localhost:11434";
        if (baseUrl.endsWith("/api/chat")) {
            this.url = baseUrl;
        } else {
            this.url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/chat";
        }
        this.model = model != null ? model : "llama3.2:3b";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Optional system prompt
        messages.add(new Message("system", "You are a concise, helpful Java programming assistant."));
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
