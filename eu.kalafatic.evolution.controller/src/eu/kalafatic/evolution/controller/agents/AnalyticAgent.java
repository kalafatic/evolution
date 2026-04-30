package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * Specialized agent for analyzing user prompts to determine intent,
 * category, and identify ambiguities before planning.
 *
 * @evo.lastModified: 14:B
 * @evo.origin: self
 */
public class AnalyticAgent extends BaseAiAgent {

    public AnalyticAgent() {
        super("Analytic", "Analytic");
    }

    @Override
    // @evo:14:B reason=flexible-analysis
    protected String getAgentInstructions() {
        return "You are an Analytic Agent. Your goal is to analyze the user's prompt for the Orchestrator.\n\n" +
                "ANALYSIS CRITERIA:\n" +
                "1. CATEGORY: CODING, RESEARCH, TOOL_USE, CHAT.\n" +
                "2. AMBIGUITY: Is it clear? Greetings (hi, hello) are NEVER ambiguous. Technical tasks with implied defaults (e.g., 'create class') are NOT ambiguous.\n" +
                "3. REFINED PROMPT: Create a technically concise version of the prompt. \n" +
                "   - DO NOT add educational context, purpose, or intended use.\n" +
                "   - DO NOT ask for clarification unless the request is fundamentally missing a target (e.g., 'delete file' without a name).\n" +
                "   - Infer sensible defaults (e.g., src/Main.java) and put them in the refinedPrompt.\n\n" +
                "OUTPUT JSON:\n" +
                "{\n" +
                "  \"category\": \"...\",\n" +
                "  \"objective\": \"...\",\n" +
                "  \"isAmbiguous\": boolean,\n" +
                "  \"missingInformation\": [],\n" +
                "  \"clarificationQuestion\": \"(Leave empty if isAmbiguous is false)\",\n" +
                "  \"refinedPrompt\": \"(Concise technical prompt)\"\n" +
                "}";
    }

    @Override
    protected String getFooterInstructions() {
        return "You MUST output a valid JSON object. Do not include any conversational preamble or follow-up text outside the JSON structure.";
    }

    // @evo:14:B reason=traceability-support
    public JSONObject analyze(String prompt, TaskContext context) throws Exception {
        String fullPrompt = buildPrompt(prompt, context, null);
        context.log("Evo-Analytic-Thinking: " + fullPrompt);
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-Analytic-Response: " + response);

        JSONObject analysis = JsonUtils.extractJsonObject(response);
        if (analysis == null) {
            context.log("Evo-Analytic: ERROR - Failed to extract JSON analysis. Returning fallback.");
            analysis = new JSONObject();
            analysis.put("category", "CHAT");
            analysis.put("objective", prompt);
            analysis.put("isAmbiguous", false);
            analysis.put("missingInformation", new org.json.JSONArray());
            analysis.put("clarificationQuestion", "");
            analysis.put("refinedPrompt", prompt);
        }
        return analysis;
    }
}
