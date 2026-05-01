package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * Specialized agent for analyzing user prompts to determine intent,
 * category, and identify ambiguities before planning.
 * It also performs failure diagnosis in the ANALYZE phase of the PEV loop.
 *
 * @evo.lastModified: 21:A
 * @evo.origin: self
 * @evo:21:A reason=progress-tracking-diagnosis
 */
public class AnalyticAgent extends BaseAiAgent {

    public AnalyticAgent() {
        super("Analytic", "Analytic");
    }

    @Override
    // @evo:14:B reason=flexible-analysis
    protected String getAgentInstructions() {
        return "Role: Analytic Agent. Goal: Analyze user prompt or task failure.\n\n" +
                "STRICT OUTPUT RULE: You MUST output ONLY a single JSON object. No preamble, no conversational text.\n\n" +
                "ANALYSIS CRITERIA (for new requests):\n" +
                "1. CATEGORY: CODING, RESEARCH, TOOL_USE, CHAT.\n" +
                "2. AMBIGUITY: ATOMIC tasks (e.g., 'create class') are NOT ambiguous. If not ambiguous, 'clarificationQuestion' and 'missingInformation' MUST be empty.\n" +
                "3. REFINED PROMPT: Create an actionable version of the prompt with assumed defaults.\n\n" +
                "DIAGNOSIS CRITERIA (for failures):\n" +
                "1. ROOT CAUSE: syntactic, logical, or environment.\n" +
                "2. PROGRESS: IMPROVED, SAME, or WORSE compared to previous attempt.\n" +
                "3. STRATEGY: RETRY, REPAIR_AGENT, or ESCALATE.\n\n" +
                "OUTPUT SCHEMA (Choose ONLY ONE):\n" +
                "NEW REQUEST:\n" +
                "{\n" +
                "  \"category\": \"...\",\n" +
                "  \"objective\": \"...\",\n" +
                "  \"isAmbiguous\": boolean,\n" +
                "  \"missingInformation\": [],\n" +
                "  \"clarificationQuestion\": \"...\",\n" +
                "  \"refinedPrompt\": \"...\"\n" +
                "}\n" +
                "DIAGNOSIS:\n" +
                "{\n" +
                "  \"rootCause\": \"...\",\n" +
                "  \"repeatFailure\": boolean,\n" +
                "  \"progress\": \"IMPROVED | SAME | WORSE\",\n" +
                "  \"suggestedStrategy\": \"RETRY | REPAIR_AGENT | ESCALATE\",\n" +
                "  \"explanation\": \"...\"\n" +
                "}";
    }

    @Override
    protected String getFooterInstructions() {
        return "You MUST output a valid JSON object. Do not include any conversational preamble or follow-up text outside the JSON structure.";
    }

    // @evo:14:B reason=failure-analysis
    public JSONObject diagnose(String result, String feedback, TaskContext context) throws Exception {
        String diagnosisPrompt = "DIAGNOSE FAILURE:\n" +
                "Result: " + (result != null ? result : "N/A") + "\n" +
                "Feedback: " + (feedback != null ? feedback : "N/A") + "\n\n" +
                "Please analyze this failure and determine if it's a repetitive issue and suggest a strategy.";

        String fullPrompt = buildPrompt(diagnosisPrompt, context, feedback);
        context.log("Evo-Analytic-Diagnosis-Thinking: " + fullPrompt);
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-Analytic-Diagnosis-Response: " + response);

        JSONObject diagnosis = JsonUtils.extractJsonObject(response);
        if (diagnosis == null) {
            context.log("Evo-Analytic: ERROR - Failed to extract JSON diagnosis. Returning fallback.");
            diagnosis = new JSONObject();
            diagnosis.put("rootCause", "Unknown");
            diagnosis.put("repeatFailure", false);
            diagnosis.put("suggestedStrategy", "RETRY");
            diagnosis.put("explanation", "Failed to parse AI response.");
        }
        return diagnosis;
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
