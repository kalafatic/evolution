package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Lightweight Intent Gate to classify user input before planning.
 */
public class IntentGate {
    private final LlmRouter llmRouter = new LlmRouter();

    public String process(String input, TaskContext context) throws Exception {
        Orchestrator orchestrator = context.getOrchestrator();

        // Step 1: Intent Classification
        String prompt = "Classify the user input into EXACTLY ONE category:\n\n" +
                "GREETING = simple greeting (hi, hello, hey)\n" +
                "CHAT = casual conversation\n" +
                "QUESTION = asking for information\n" +
                "ACTION = explicit request to do something (create, write, fix, generate code, etc.)\n" +
                "AMBIGUOUS = unclear intent\n\n" +
                "Rules:\n" +
                "- DO NOT assume hidden intent\n" +
                "- \"hi\" is GREETING\n" +
                "- Only ACTION if explicit verb exists\n\n" +
                "Return ONLY JSON:\n\n" +
                "{\n" +
                "  \"intent\": \"GREETING|CHAT|QUESTION|ACTION|AMBIGUOUS\",\n" +
                "  \"confidence\": 0.0-1.0\n" +
                "}\n\n" +
                "User input:\n" +
                "\"" + input + "\"";

        float temperature = 0.0f; // Minimal randomness for classification
        String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;

        String response = llmRouter.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
        JSONObject json = new JSONObject(cleanResponse(response));

        String intent = json.optString("intent", "AMBIGUOUS");
        double confidence = json.optDouble("confidence", 0.0);

        // Step 2: Hardcoded Decision Logic (Java)
        if (confidence < 0.6) {
            return "CLARIFY: Your request is a bit unclear. Could you please specify what you'd like me to do?";
        }

        switch (intent) {
            case "GREETING":
                return "Hello! How can I help you today?";
            case "CHAT":
                // Basic chat response (could be more sophisticated, but keeping it minimal)
                return "I'm Jules, your AI software engineer. I'm here to help with your coding tasks.";
            case "QUESTION":
                // If it's a question, we should probably answer it directly using the LLM without a planner
                String questionPrompt = "Answer the following user question directly and concisely. User question: \"" + input + "\"";
                return llmRouter.sendRequest(orchestrator, questionPrompt, 0.7f, proxyUrl, context);
            case "AMBIGUOUS":
                return "CLARIFY: I'm not sure I understand. Could you please provide more details about your request?";
            case "ACTION":
                // Let the orchestrator continue to the planner
                return null;
            default:
                return "CLARIFY: I'm not sure how to handle that request. Could you rephrase it?";
        }
    }

    private String cleanResponse(String response) {
        String trimmed = response.trim();
        int firstBackticks = trimmed.indexOf("```");
        if (firstBackticks != -1) {
            int firstNewline = trimmed.indexOf("\n", firstBackticks);
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        return trimmed;
    }
}
