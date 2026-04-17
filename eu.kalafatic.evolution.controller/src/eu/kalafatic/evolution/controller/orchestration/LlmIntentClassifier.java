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
        ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());

        StringBuilder sb = new StringBuilder();
        sb.append("You are an Intent Gate for an AI Orchestration system.\n");
        sb.append("Your goal is to classify the user's latest message based on the CURRENT STATE and conversation history.\n\n");

        sb.append("--- CURRENT STATE ---\n");
        sb.append(state.toJSON().toString(2)).append("\n");
        sb.append("--- END STATE ---\n\n");

        sb.append("CLASSIFICATION CATEGORIES:\n");
        sb.append("- 'new': User explicitly requested a new action unrelated to the current goal.\n");
        sb.append("- 'continue': User is continuing, providing info for, or acknowledging the current task/goal. Also used for confirming or responding to AI's previous clarification or proposal.\n");
        sb.append("- 'chat': General conversation, greetings (e.g. 'hi', 'hello'), or non-actionable polite remarks.\n");
        sb.append("- 'unclear': Insufficient info or highly ambiguous intent that doesn't fit the above.\n\n");

        sb.append("RULES:\n");
        sb.append("- 'hi', 'hello' should be 'chat'.\n");
        sb.append("- 'ok', 'yes', 'proceed' are 'continue' if a task is active.\n");
        sb.append("- If input is 'new', suggest a NEW 'goal_update'.\n\n");

        sb.append("Return ONLY JSON:\n");
        sb.append("{\n");
        sb.append("  \"intent\": \"new | continue | chat | unclear\",\n");
        sb.append("  \"goal_update\": \"...optional...\",\n");
        sb.append("  \"needs_clarification\": boolean,\n");
        sb.append("  \"confidence\": 0.0-1.0,\n");
        sb.append("  \"reason\": \"short explanation\"\n");
        sb.append("}\n\n");
        sb.append("Input: \"").append(input).append("\"");

        String prompt = sb.toString();

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
