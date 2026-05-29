package eu.kalafatic.evolution.controller.agents;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Specialized agent for reviewing and critiquing plans.
 */
public class CriticAgent extends BaseAiAgent {

    public CriticAgent() {
        super("Critic", "Critic");
    }

    public CriticAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Critic", "Critic", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Critic specializing in evaluating development plans.\n" +
                "Your goal is to review a proposed plan (list of tasks) against the original user request and provide a structured critique.\n\n" +
                "CRITIQUE CRITERIA:\n" +
                "1. CORRECTNESS: Does the plan fully address the user's request?\n" +
                "2. AMBIGUITY: Are any tasks vaguely defined or open to misinterpretation?\n" +
                "3. COMPLETENESS: Are there any missing steps (e.g., testing, documentation, edge cases)?\n" +
                "4. ARCHITECTURE: Does the plan follow best practices and avoid architectural violations?\n" +
                "5. ASSUMPTIONS: Does the plan make invalid assumptions about the codebase or environment?\n\n" +
                "SCORING:\n" +
                "- qualityScore: A value between 0.0 and 1.0 representing the overall quality and readiness of the plan.\n" +
                "- isCorrect: true only if the plan is ready for execution without further repair.\n";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output MUST be a valid JSON object. Do not include any conversational preamble or follow-up text outside the JSON structure.\n" +
                "Schema:\n" +
                "{\n" +
                "  \"isCorrect\": boolean,\n" +
                "  \"qualityScore\": 0.0-1.0,\n" +
                "  \"critique\": {\n" +
                "    \"correctness\": \"...\",\n" +
                "    \"ambiguity\": \"...\",\n" +
                "    \"completeness\": \"...\",\n" +
                "    \"architecture\": \"...\",\n" +
                "    \"assumptions\": \"...\"\n" +
                "  },\n" +
                "  \"suggestions\": [\"suggestion 1\", \"suggestion 2\"]\n" +
                "}";
    }

    public JSONObject critique(String request, String planJson, TaskContext context) throws Exception {
        String input = "USER REQUEST: " + request + "\nPROPOSED PLAN: " + planJson;
        String fullPrompt = buildPrompt(input, context, null);

        context.log("Evo-Critic-Thinking: " + fullPrompt);
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-Critic-Response: " + response);

        JSONObject critique = JsonUtils.extractJsonObject(response);

        if (critique == null) {
            context.log("Critic: Warning - AI response is not a JSON object. Using fallback failure critique.");
            critique = new JSONObject();
            critique.put("isCorrect", false);
            critique.put("qualityScore", 0.0);
            JSONObject critiqueObj = new JSONObject();
            critiqueObj.put("correctness", "Failed to parse critic response.");
            critique.put("critique", critiqueObj);
        }
        return critique;
    }
}
