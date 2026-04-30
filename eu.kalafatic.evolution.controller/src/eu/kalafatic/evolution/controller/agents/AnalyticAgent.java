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
                "2. SCOPE: Determine if the request is ATOMIC (single file, simple fix) or ARCHITECTURAL (system design, multiple modules).\n" +
                "3. AMBIGUITY:\n" +
                "   - For ATOMIC tasks: Technical tasks with implied defaults (e.g., 'create class') are NOT ambiguous. Just proceed with defaults (e.g., src/Main.java).\n" +
                "   - For ARCHITECTURAL tasks: Asking for 'purpose', 'usage' or 'big picture' IS encouraged if it helps build a better system model.\n" +
                "4. REFINED PROMPT: Create a version of the prompt that reflects the scope.\n" +
                "   - For ATOMIC: Keep it technical and concise.\n" +
                "   - For ARCHITECTURAL: Incorporate known context while leaving room for architectural decisions.\n\n" +
                "OUTPUT JSON:\n" +
                "{\n" +
                "  \"category\": \"...\",\n" +
                "  \"objective\": \"...\",\n" +
                "  \"isAmbiguous\": boolean,\n" +
                "  \"missingInformation\": [],\n" +
                "  \"clarificationQuestion\": \"(Leave empty if isAmbiguous is false)\",\n" +
                "  \"refinedPrompt\": \"(Prompt for the planner)\"\n" +
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
