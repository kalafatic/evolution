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
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ollama LLM provider implementation.
 */
public class OllamaProvider implements ILlmProvider {

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        return sendRequestWithRetry(orchestrator, prompt, temperature, proxyUrl, context, 0);
    }

    private String sendRequestWithRetry(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context, int depth) throws Exception {
        if (depth > 3) {
            throw new Exception("Maximum fallback depth reached for Ollama requests");
        }

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
            String body = response.body();
            if (body.contains("requires more system memory") && body.contains("than is available")) {
                context.log("Ollama: Memory error detected. Attempting fallback...");
                String fallbackModel = findFallbackModel(baseUrl, body, context);
                if (fallbackModel != null && !fallbackModel.equals(model)) {
                    context.log("Ollama: Falling back to model: " + fallbackModel);
                    updateOrchestratorModel(orchestrator, fallbackModel);
                    // Retry with new model
                    return sendRequestWithRetry(orchestrator, prompt, temperature, proxyUrl, context, depth + 1);
                }
            }
            throw new Exception("Ollama error: " + response.statusCode() + " - " + body);
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.has("response") ? jsonResponse.getString("response") : jsonResponse.getString("solution");
    }

    private String findFallbackModel(String baseUrl, String errorBody, TaskContext context) {
        try {
            // Extract available memory from error message: "is available (4.9 GiB)"
            Pattern pattern = Pattern.compile("is available \\((\\d+\\.?\\d*)\\s*([KMGT]iB)\\)");
            Matcher matcher = pattern.matcher(errorBody);
            long availableBytes = Long.MAX_VALUE;
            if (matcher.find()) {
                double value = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2);
                availableBytes = (long) (value * getMultiplier(unit));
                context.log("Ollama: Available memory parsed: " + value + " " + unit + " (" + availableBytes + " bytes)");
            }

            // Fetch available models
            String tagsUrl = baseUrl;
            if (tagsUrl.contains("/api/generate")) {
                tagsUrl = tagsUrl.replace("/api/generate", "/api/tags");
            } else {
                tagsUrl = tagsUrl + (tagsUrl.endsWith("/") ? "" : "/") + "api/tags";
            }

            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(tagsUrl)).GET().build();
            HttpResponse<String> tagsResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (tagsResponse.statusCode() == 200) {
                JSONObject json = new JSONObject(tagsResponse.body());
                JSONArray models = json.getJSONArray("models");
                String bestFallback = null;
                long bestSize = -1;

                for (int i = 0; i < models.length(); i++) {
                    JSONObject m = models.getJSONObject(i);
                    String name = m.getString("name");
                    long size = m.optLong("size", 0);

                    // We want the largest model that fits in available memory
                    if (size > 0 && size < availableBytes * 0.9) { // 10% buffer
                        if (size > bestSize) {
                            bestSize = size;
                            bestFallback = name;
                        }
                    }
                }
                return bestFallback;
            }
        } catch (Exception e) {
            context.log("Ollama: Failed to find fallback model: " + e.getMessage());
        }
        return null;
    }

    private long getMultiplier(String unit) {
        return switch (unit) {
            case "KiB" -> 1024L;
            case "MiB" -> 1024L * 1024;
            case "GiB" -> 1024L * 1024 * 1024;
            case "TiB" -> 1024L * 1024 * 1024 * 1024;
            default -> 1L;
        };
    }

    private void updateOrchestratorModel(Orchestrator orchestrator, String newModel) {
        if (orchestrator.getOllama() != null) {
            orchestrator.getOllama().setModel(newModel);
        }

        AiMode mode = orchestrator.getAiMode();
        if (mode == AiMode.LOCAL) {
            orchestrator.setLocalModel(newModel);
        } else if (mode == AiMode.HYBRID) {
            orchestrator.setHybridModel(newModel);
        }
    }
}
