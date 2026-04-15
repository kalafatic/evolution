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
        String intent = classification.optString("intent", "AMBIGUOUS");
        double confidence = classification.optDouble("confidence", 0.0);

        if (confidence < CONFIDENCE_THRESHOLD) {
            return "CLARIFY: Your request is a bit unclear. Could you please specify what you'd like me to do?";
        }

        Orchestrator orchestrator = context.getOrchestrator();
        String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;

        switch (intent) {
            case "GREETING":
                return "Hello! I'm Jules, your AI software engineer. How can I help you today?";
            case "CHIT_CHAT":
                return "I'm doing well, thank you! Ready to help with any coding or technical tasks.";
            case "QUESTION":
                String qPrompt = "Answer this question directly and concisely: \"" + input + "\"";
                return llmRouter.sendRequest(orchestrator, qPrompt, 0.7f, proxyUrl, context);
            case "AMBIGUOUS":
                return "CLARIFY: I'm not sure I follow. Could you provide more details about the action you want me to take?";
            case "SYSTEM_COMMAND":
                return "SYSTEM: Command recognized. For now, please use the UI buttons for most control actions.";
            case "ACTION_REQUEST":
                if (classification.optBoolean("requires_action", false)) {
                    return null; // Allow planning
                }
                return "CLARIFY: You mentioned an action, but I need more specifics to create a plan.";
            default:
                return "CLARIFY: I'm not sure how to handle that. Can you rephrase your request?";
        }
    }
}
