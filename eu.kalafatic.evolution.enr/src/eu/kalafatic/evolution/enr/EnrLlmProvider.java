package eu.kalafatic.evolution.enr;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Bridges the ENR runtime into the Darwin LLM provider abstraction ({@link ILlmProvider}),
 * making ENR selectable by the existing LlmRouter (model selection, routing and reasoning
 * protocol parsing stay unchanged). Only {@link #sendRequest} is implemented; the default
 * {@link ILlmProvider#sendLlmRequest} reasoning-protocol parsing is reused as-is.
 */
public class EnrLlmProvider implements ILlmProvider {

    private final EnrInferenceEngine engine;

    public EnrLlmProvider(EnrInferenceEngine engine) {
        this.engine = engine == null ? EnrRuntime.instance() : engine;
    }

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
            String proxyUrl, TaskContext context) {
        String model = resolveModel(orchestrator, context);
        List<EnrInferenceEngine.EnrChatMessage> messages = new ArrayList<>();
        messages.add(EnrInferenceEngine.EnrChatMessage.user(
                prompt == null ? "" : prompt));
        EnrInferenceEngine.EnrChatResponse response = engine.chat(
                new EnrInferenceEngine.EnrChatRequest(model, messages, temperature, 512));
        return response.content();
    }

    private static String resolveModel(Orchestrator orchestrator, TaskContext context) {
        if (context != null && context.getMetadata() != null) {
            Object model = context.getMetadata().get("model");
            if (model != null && !String.valueOf(model).isBlank()) {
                return String.valueOf(model);
            }
        }
        if (orchestrator != null) {
            if (orchestrator.getLocalModel() != null
                    && !orchestrator.getLocalModel().isBlank()) {
                return orchestrator.getLocalModel();
            }
            if (orchestrator.getOllama() != null
                    && orchestrator.getOllama().getModel() != null
                    && !orchestrator.getOllama().getModel().isBlank()) {
                return orchestrator.getOllama().getModel();
            }
        }
        return "llama3.2:3b";
    }
}
