package eu.kalafatic.evolution.controller.orchestration.adapters;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

public class LLMAdapter {
    private final AiService aiService;

    public LLMAdapter(AiService aiService) {
        this.aiService = aiService;
    }

    public String generate(String prompt, TaskContext context) {
        try {
            return aiService.sendRequest(context.getOrchestrator(), prompt, context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T generateAndParse(String prompt, Class<T> targetClass, TaskContext context) {
        String response = generate(prompt, context);
        return JsonUtils.restoreFromMetadata(response, targetClass, "llm-response", context);
    }
}
