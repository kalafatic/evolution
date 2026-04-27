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

    @Override
    public JSONObject classify(String input, TaskContext context) throws Exception {
        Orchestrator orchestrator = context.getOrchestrator();
        ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());

        StringBuilder sb = new StringBuilder();
        sb.append("You are an Intent Gate for an AI Orchestration system.\n");
        sb.append("Your goal is to classify the user's latest message based on the CURRENT STATE and conversation history.\n\n");

        sb.append("--- CURRENT STATE ---\n");
        sb.append(state.toJSON().toString(2)).append("\n");
        if (context.getPlatformMode() != null) {
            sb.append("Active Mode: ").append(context.getPlatformMode().getType()).append("\n");
        }
        sb.append("--- END STATE ---\n\n");

        sb.append("CLASSIFICATION CATEGORIES:\n");
        sb.append("- 'new': User requested a new action (e.g., create, fix, refactor, test, run). Even if a goal is active, specific new instructions are 'new' or 'continue'.\n");
        sb.append("- 'continue': User is providing info for, or acknowledging the current task/goal (e.g., 'ok', 'yes', 'go ahead').\n");
        sb.append("- 'chat': ONLY for greetings (hi, hello) or non-actionable remarks. Actionable requests are NEVER 'chat'.\n");
        sb.append("- 'unclear': Insufficient info or highly ambiguous intent.\n\n");

        sb.append("RULES:\n");
        sb.append("- ANY request to 'create', 'fix', 'generate', 'write', 'add', 'run', 'test' MUST be 'new' or 'continue', NEVER 'chat'.\n");
        sb.append("- 'hi', 'hello' should be 'chat'.\n");
        sb.append("- 'ok', 'yes', 'proceed' are 'continue' if a task is active.\n");
        sb.append("- If input is 'new', suggest a NEW 'goal_update'.\n\n");

        sb.append("EXAMPLES:\n");
        sb.append("- Input: \"fix the bug in Main.java\"\n");
        sb.append("  Output: {\"intent\": \"new\", \"goal_update\": \"Fix bug in Main.java\", \"needs_clarification\": false, \"confidence\": 0.9, \"reason\": \"User requested a specific coding action.\"}\n");
        sb.append("- Input: \"create java class Test.java which can print\"\n");
        sb.append("  Output: {\"intent\": \"new\", \"goal_update\": \"Create Test.java class\", \"needs_clarification\": false, \"confidence\": 1.0, \"reason\": \"User requested to create a new Java class.\"}\n");
        sb.append("- Input: \"yes, please proceed\"\n");
        sb.append("  Output: {\"intent\": \"continue\", \"needs_clarification\": false, \"confidence\": 1.0, \"reason\": \"User confirmed the current task.\"}\n\n");

        sb.append("Input: \"").append(input).append("\"\n\n");

        sb.append("Return ONLY valid JSON. No preamble. NO <think> blocks. NO reasoning text. Use ONLY the specified schema.\n");
        sb.append("Schema:\n");
        sb.append("{\n");
        sb.append("  \"intent\": \"new | continue | chat | unclear\",\n");
        sb.append("  \"goal_update\": \"...optional...\",\n");
        sb.append("  \"needs_clarification\": boolean,\n");
        sb.append("  \"confidence\": 0.0-1.0,\n");
        sb.append("  \"reason\": \"short explanation\"\n");
        sb.append("}");

        String prompt = sb.toString();
        context.log("Evo-IntentClassifier-Thinking: " + prompt);

        String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;
        String response = llmRouter.sendRequest(orchestrator, prompt, 0.0f, proxyUrl, context);
        context.log("Evo-IntentClassifier-Response: " + response);

        JSONObject classification = JsonUtils.extractJsonObject(response);
        if (classification == null) {
            context.log("Evo-IntentClassifier: ERROR - Failed to extract JSON classification. Returning unclear.");
            classification = new JSONObject();
            classification.put("intent", "unclear");
            classification.put("reason", "Failed to parse AI response.");
        }
        return classification;
    }
}
