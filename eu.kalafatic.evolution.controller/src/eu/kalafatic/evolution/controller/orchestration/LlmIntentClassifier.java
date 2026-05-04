package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * LLM-based intent classification optimized for small models.
 */
public class LlmIntentClassifier implements IIntentClassifier {
    private final LlmRouter llmRouter = new LlmRouter();
    private AiService aiService = new AiService();

    @Override
    public JSONObject classify(String input, TaskContext context) throws Exception {
        if (input == null) input = "";
        String lowerInput = input.toLowerCase();

        context.log("[INTENT] Classifying: " + input);

        // 1. Fast-track common intents (Bypass LLM)
        if (lowerInput.matches("^\\s*(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\s*[!.]*\\s*$")) {
            JSONObject result = new JSONObject();
            result.put("intent", "chat");
            result.put("confidence", 1.0);
            result.put("reason", "Fast-track greeting detection.");
            return result;
        }

        String prompt = "Classify intent: " + input;
        String response = aiService.sendRequest(context.getOrchestrator(), prompt, 0.0f, null, context);
        return JsonUtils.extractJsonObject(response);
    }

    public void setAiService(AiService aiService) {
        this.aiService = aiService;
    }
}
