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
        boolean isGreeting = "chat".equals(intent) ||
                             input.toLowerCase().matches("^\\s*(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\s*[!.]*\\s*$");

        if (isGreeting) {
            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());
            if (state.getGoal().isEmpty()) {
                return "Hello! I'm Evo, your AI software engineer. How can I help you today?";
            }
        }

        if (confidence < CONFIDENCE_THRESHOLD || "unclear".equals(intent) || needsClarification) {
            return "CLARIFY: " + classification.optString("reason", "I'm not sure I understand your request. Could you please provide more details?");
        }

        if ("new".equals(intent) || "continue".equals(intent)) {
            // Check if it's just a simple greeting disguised as 'continue' when no goal exists
            ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());
            if (state.getGoal().isEmpty() && (input.toLowerCase().contains("hi") || input.toLowerCase().contains("hello"))) {
                return "Hello! I'm Evo, your AI software engineer. How can I help you today?";
            }

            // Allow planning or execution continuation
            return null;
        }

        if ("chat".equals(intent)) {
            return "I'm here to help with your project. You can ask me to create files, fix bugs, or run tests.";
        }

        return "CLARIFY: I'm not sure how to handle that. Can you rephrase your request?";
    }
}
