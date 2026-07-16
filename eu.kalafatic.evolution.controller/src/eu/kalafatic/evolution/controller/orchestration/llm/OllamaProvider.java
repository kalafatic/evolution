package eu.kalafatic.evolution.controller.orchestration.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Ollama LLM provider implementation.
 * Delegating to the managed OllamaService.
 *
 * @evo:1:1 reason=delegate-to-managed-ollama-service
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

        // Use the managed service
        OllamaService service = OllamaManager.getInstance().getService(baseUrl);
        service.setModel(model);
        service.setTemperature(temperature);

        // Self-healing for missing forged models (e.g. "evo")
        if (model != null && model.toLowerCase().contains("evo")) {
            try {
                List<OllamaModel> available = service.loadModels();
                boolean found = false;
                for (OllamaModel m : available) {
                    if (m.getName().equalsIgnoreCase(model) || m.getName().startsWith(model + ":")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (context != null) context.log("Ollama: Forged model '" + model + "' not found in Ollama tags. Triggering self-healing registration...");
                    java.io.File ggufFile = new java.io.File(System.getProperty("user.home"), ".ollama/models/" + model + ".gguf");
                    if (!ggufFile.exists()) {
                        ggufFile = new java.io.File(System.getProperty("user.home"), ".ollama/models/evo.gguf");
                    }
                    if (!ggufFile.exists()) {
                        // Fallback to workspace source/models folder
                        String codebasePath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
                        if (codebasePath != null) {
                            java.io.File sourceModelsDir = new java.io.File(codebasePath, "source/models");
                            if (sourceModelsDir.exists() && sourceModelsDir.isDirectory()) {
                                java.io.File f = new java.io.File(sourceModelsDir, model + ".gguf");
                                if (f.exists()) {
                                    ggufFile = f;
                                } else {
                                    f = new java.io.File(sourceModelsDir, "evo.gguf");
                                    if (f.exists()) {
                                        ggufFile = f;
                                    }
                                }
                            }
                        }
                    }
                    if (!ggufFile.exists()) {
                        // Fallback to codebase dist folder
                        String codebasePath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
                        if (codebasePath != null) {
                            java.io.File distDir = new java.io.File(codebasePath, "dist");
                            if (distDir.exists() && distDir.isDirectory()) {
                                java.io.File[] subdirs = distDir.listFiles(java.io.File::isDirectory);
                                if (subdirs != null) {
                                    for (java.io.File subdir : subdirs) {
                                        if (subdir.getName().equalsIgnoreCase(model) || subdir.getName().startsWith("evo-")) {
                                            java.io.File f = new java.io.File(subdir, "evo.gguf");
                                            if (f.exists()) {
                                                ggufFile = f;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (ggufFile.exists()) {
                        // Resolve first available base model to avoid download freezes
                        String baseModel = "llama3.2:3b";
                        if (!available.isEmpty()) {
                            for (OllamaModel m : available) {
                                if (m.getName().contains("llama3.2:3b")) {
                                    baseModel = "llama3.2:3b";
                                    break;
                                }
                            }
                            if (baseModel.equals("llama3.2:3b") && !available.get(0).getName().equalsIgnoreCase(model)) {
                                baseModel = available.get(0).getName();
                            }
                        }

                        String ggufPathNormalized = ggufFile.getAbsolutePath().replace("\\", "/");
                        String modelfileContent = "FROM " + baseModel + "\n" +
                                                 "ADAPTER " + ggufPathNormalized + "\n" +
                                                 "PARAMETER temperature 0.7\n" +
                                                 "PARAMETER stop \"<EOS>\"\n" +
                                                 "SYSTEM \"\"\"You are an Evolution AI assistant specialized in this project codebase.\"\"\"";

                        if (context != null) context.log("Ollama: Programmatically creating model '" + model + "' from: " + ggufPathNormalized);
                        service.createModel(model, modelfileContent);
                        if (context != null) context.log("Ollama: Self-healing registration complete for '" + model + "'.");
                    } else {
                        if (context != null) context.log("Ollama: Warning - GGUF file for '" + model + "' was not found, cannot self-heal.");
                    }
                }
            } catch (Exception ex) {
                if (context != null) context.log("Ollama: Self-healing registration failed: " + ex.getMessage());
            }
        }

        try {
            String sessionId = context.getSessionId();
            if (sessionId == null) sessionId = "Default";
            String response = service.chat(prompt, sessionId);
            if (context != null) {
                context.log("Stage: LLM\nProvider: Ollama\nModel: " + model + "\nToken count: (estimated) " + (prompt.length() / 4) + "\nRaw response length: " + response.length());
            }
            return response;
        } catch (Exception e) {
            String errorBody = e.getMessage();
            if (errorBody != null && errorBody.contains("requires more system memory") && errorBody.contains("than is available")) {
                context.log("Ollama: Memory error detected. Attempting fallback...");
                String fallbackModel = findFallbackModel(service, errorBody, context);
                if (fallbackModel != null && !fallbackModel.equals(model)) {
                    context.log("Ollama: Falling back to model: " + fallbackModel);
                    updateOrchestratorModel(orchestrator, fallbackModel);
                    // Retry with new model
                    return sendRequestWithRetry(orchestrator, prompt, temperature, proxyUrl, context, depth + 1);
                }
            }
            throw e;
        }
    }

    private String findFallbackModel(OllamaService service, String errorBody, TaskContext context) {
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

            // Fetch available models via the service
            List<OllamaModel> models = service.loadModels();
            String bestFallback = null;
            long bestSize = -1;

            for (OllamaModel m : models) {
                String name = m.getName();
                long size = m.getSize();

                // We want the largest model that fits in available memory
                if (size > 0 && size < availableBytes * 0.9) { // 10% buffer
                    if (size > bestSize) {
                        bestSize = size;
                        bestFallback = name;
                    }
                }
            }
            return bestFallback;
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

        // Dynamic model update based on current operational context
        orchestrator.setLocalModel(newModel);
    }

    public String sendImageRequest(Orchestrator orchestrator, String prompt, String imagePath, TaskContext context) throws Exception {
        if (orchestrator.getOllama() == null || orchestrator.getOllama().getUrl() == null || orchestrator.getOllama().getUrl().isEmpty()) {
            throw new Exception("Ollama is not configured for Multi-Modal");
        }

        String baseUrl = orchestrator.getOllama().getUrl();
        String model = orchestrator.getOllama().getModel();

        OllamaService service = OllamaManager.getInstance().getService(baseUrl);
        service.setModel(model);

        return service.analyzeImage(prompt, imagePath);
    }
    
    public static int testLLM(String baseUrl, String model) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            String json = """
                {
                  "model":"%s",
                  "prompt":"Reply with exactly OK",
                  "stream":false
                }
                """.formatted(model);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("LLM failed: HTTP " + response.statusCode());
                return response.statusCode();
            }

            String body = response.body();

            // Simple check that a response was generated
            if (body.contains("\"response\"") && body.contains("OK")) {
                System.out.println("✓ LLM is working.");
                return 200;
            }

            System.err.println("LLM responded unexpectedly:");
            System.err.println(body);
            return 500;

        } catch (Exception e) {
            System.err.println("LLM test failed: " + e.getMessage());
            return 600;
        }
    }
    
    
    public static void main(String[] args) {
    	 testLLM("http://localhost:11434", "gemma3:1b");
	}
    
}
