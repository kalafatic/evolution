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
        return "You are a Specialized Analytic Agent for an AI Orchestration system (Evo).\n" +
                "Your goal is to deeply analyze the user's prompt BEFORE any planning or execution happens.\n\n" +
                "ANALYSIS CRITERIA:\n" +
                "1. CATEGORY: Determine if the request is CODING (writing/fixing code), RESEARCH (gathering info/analyzing files), " +
                "TOOL_USE (running maven, git, shell), or CHAT (general conversation/greetings).\n" +
                "2. OBJECTIVE: What is the main thing the user wants to achieve?\n" +
                "3. AMBIGUITY: Is the request clear enough to execute? \n" +
                "   - Example of ambiguous: 'fix the bug' (which bug? where?).\n" +
                "   - Example of clear: 'Create a Java class that prints \"Hi\"'. In such cases, DO NOT MARK AS AMBIGUOUS. Instead, infer sensible default names and paths (e.g., src/main/java/HelloWorld.java) in your refinedPrompt.\n" +
                "   - Mandatory clarification is ONLY required for destructive actions (DELETE) with missing targets or highly vague requests where no reasonable default can be inferred.\n" +
                "   - NOTE: Simple greetings (e.g., 'hi', 'hello', 'good morning') are NEVER ambiguous. Categorize them as CHAT.\n" +
                "4. MISSING INFORMATION: List specific details needed to proceed. If you inferred defaults, list them here as well.\n\n" +
                "OUTPUT FORMAT:\n" +
                "You MUST output a valid JSON object. Ensure no duplicate keys are present. All fields are REQUIRED.\n" +
                "Schema:\n" +
                "{\n" +
                "  \"category\": \"CODING\"|\"RESEARCH\"|\"TOOL_USE\"|\"CHAT\",\n" +
                "  \"objective\": \"Brief summary of the goal\",\n" +
                "  \"isAmbiguous\": boolean,\n" +
                "  \"missingInformation\": [\"list\", \"of\", \"missing\", \"details\"],\n" +
                "  \"clarificationQuestion\": \"A polite question to ask the user if the request is ambiguous.\",\n" +
                "  \"refinedPrompt\": \"An improved version of the prompt based on your analysis (including inferred defaults if any)\"\n" +
                "}\n\n" +
                "If the request is clear (including all CHAT requests and requests where defaults can be inferred), set isAmbiguous to false and missingInformation to an empty array (or list of inferred defaults).\n" +
                "If the user says 'Execute the simplest working solution.', set isAmbiguous to false and use the refinedPrompt to reconstruct the original goal from shared memory.";
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
