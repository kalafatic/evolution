package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * LLM-based intent classification optimized for small models.
 */
public class LlmIntentClassifier implements IIntentClassifier {
    private final LlmRouter llmRouter = new LlmRouter();

    @Override
    public JSONObject classify(String input, TaskContext context) throws Exception {
        Orchestrator orchestrator = context.getOrchestrator();

        String prompt = "Classify user input into EXACTLY ONE category:\n" +
                "GREETING: hello, hi, etc.\n" +
                "CHIT_CHAT: casual talk.\n" +
                "QUESTION: asking info.\n" +
                "ACTION_REQUEST: do something (create, fix, run).\n" +
                "AMBIGUOUS: unclear.\n" +
                "SYSTEM_COMMAND: /help, /stop.\n\n" +
                "Rules:\n" +
                "- No explicit verb = not ACTION_REQUEST.\n" +
                "- 'hi' = GREETING.\n\n" +
                "Return ONLY JSON:\n" +
                "{\n" +
                "  \"intent\": \"GREETING|CHIT_CHAT|QUESTION|ACTION_REQUEST|AMBIGUOUS|SYSTEM_COMMAND\",\n" +
                "  \"confidence\": 0.0-1.0,\n" +
                "  \"requires_action\": bool,\n" +
                "  \"requires_clarification\": bool,\n" +
                "  \"reason\": \"str\"\n" +
                "}\n\n" +
                "Input: \"" + input + "\"";

        String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;
        String response = llmRouter.sendRequest(orchestrator, prompt, 0.0f, proxyUrl, context);

        return new JSONObject(cleanResponse(response));
    }

    private String cleanResponse(String response) {
        String trimmed = response.trim();
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
