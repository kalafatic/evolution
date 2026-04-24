package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        try {
            return service.generate(prompt);
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

        AiMode mode = orchestrator.getAiMode();
        if (mode == AiMode.LOCAL) {
            orchestrator.setLocalModel(newModel);
        } else if (mode == AiMode.HYBRID) {
            orchestrator.setHybridModel(newModel);
        }
    }
}
