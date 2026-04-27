package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Hardcoded decision logic based on intent classification.
 */
public class RuleBasedPolicyEngine implements IPolicyEngine {
    private static final double CONFIDENCE_THRESHOLD = 0.6;
    private final LlmRouter llmRouter = new LlmRouter();

    @Override
    public String evaluate(JSONObject classification, String input, TaskContext context) throws Exception {
        String intent = classification.optString("intent", "unclear").trim().toLowerCase();
        double confidence = classification.optDouble("confidence", 0.0);
        boolean needsClarification = classification.optBoolean("needs_clarification", false);

        // Robust greeting detection before other checks
        boolean isGreeting = ("chat".equals(intent) ||
                             input.toLowerCase().matches("^\\s*(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\s*[!.]*\\s*$"));

        if (isGreeting) {
            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());
            if (state.getGoal().isEmpty()) {
                return "Hello! I'm Evo, your AI software engineer. How can I help you today?";
            }
        }

        if ("unclear".equals(intent) || (confidence < CONFIDENCE_THRESHOLD && !needsClarification)) {
            return "CLARIFY: " + classification.optString("reason", "I'm not sure I understand your request. Could you please provide more details?");
        }

        if ("new".equals(intent) || "continue".equals(intent)) {
            // Check if it's just a simple greeting disguised as 'continue' when no goal exists.
            // Use word boundaries to avoid catching words like 'hello' inside longer commands (e.g. 'Say Hello').
            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());
            if (state.getGoal().isEmpty() && input.toLowerCase().matches(".*\\b(hi|hello|hey)\\b.*") && input.length() < 15) {
                return "Hello! I'm Evo, your AI software engineer. How can I help you today?";
            }

            if (needsClarification) {
                return null;
            }

            // Allow planning or execution continuation
            return null;
        }

        if ("chat".equals(intent)) {
            // Safety check: if classification is 'chat' but input contains actionable keywords,
            // we override it to allow the request to proceed. Small models often hallucinate 'chat' for short requests.
            if (input.toLowerCase().matches(".*\\b(create|fix|add|run|test|generate|write|refactor|modify|delete|check)\\b.*")) {
                return null; // Proceed instead of blocking with greeting
            }
            return "I'm here to help with your project. You can ask me to create files, fix bugs, or run tests.";
        }

        return "CLARIFY: I'm not sure how to handle that. Can you rephrase your request?";
    }
}
