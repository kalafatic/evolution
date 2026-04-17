package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import org.json.JSONObject;

/**
 * Specialized agent for analyzing user prompts to determine intent,
 * category, and identify ambiguities before planning.
 */
public class AnalyticAgent extends BaseAiAgent {

    public AnalyticAgent() {
        super("Analytic", "Analytic");
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Specialized Analytic Agent for an AI Orchestration system (Evo).\n" +
                "Your goal is to deeply analyze the user's prompt BEFORE any planning or execution happens.\n\n" +
                "ANALYSIS CRITERIA:\n" +
                "1. CATEGORY: Determine if the request is CODING (writing/fixing code), RESEARCH (gathering info/analyzing files), " +
                "TOOL_USE (running maven, git, shell), or CHAT (general conversation/greetings).\n" +
                "2. OBJECTIVE: What is the main thing the user wants to achieve?\n" +
                "3. AMBIGUITY: Is the request clear enough to execute? \n" +
                "   - Example of ambiguous: 'fix the bug' (which bug? where?), 'create file' (what name? what content?).\n" +
                "   - Example of clear: 'Create a Java class named Hello in src/main/java that prints \"Hi\"'.\n" +
                "   - If 'create file' or similar is requested without details, YOUR clarificationQuestion MUST specifically ask: 'Where should the file/directory be created? What is the name, extension, and purpose?'.\n" +
                "   - NOTE: Simple greetings (e.g., 'hi', 'hello', 'good morning') are NEVER ambiguous. Categorize them as CHAT.\n" +
                "4. MISSING INFORMATION: List specific details needed to proceed.\n\n" +
                "OUTPUT FORMAT:\n" +
                "You MUST output a valid JSON object. Ensure no duplicate keys are present. All fields are REQUIRED.\n" +
                "Schema:\n" +
                "{\n" +
                "  \"category\": \"CODING\"|\"RESEARCH\"|\"TOOL_USE\"|\"CHAT\",\n" +
                "  \"objective\": \"Brief summary of the goal\",\n" +
                "  \"isAmbiguous\": boolean,\n" +
                "  \"missingInformation\": [\"list\", \"of\", \"missing\", \"details\"],\n" +
                "  \"clarificationQuestion\": \"A polite question to ask the user if the request is ambiguous.\",\n" +
                "  \"refinedPrompt\": \"An improved version of the prompt based on your analysis (if not ambiguous)\"\n" +
                "}\n\n" +
                "If the request is clear (including all CHAT requests), set isAmbiguous to false and missingInformation to an empty array.\n" +
                "If the user says 'Execute the simplest working solution.', set isAmbiguous to false and use the refinedPrompt to reconstruct the original goal from shared memory.";
    }

    @Override
    protected String getFooterInstructions() {
        return "You MUST output a valid JSON object. Do not include any conversational preamble or follow-up text outside the JSON structure.";
    }

    public JSONObject analyze(String prompt, TaskContext context) throws Exception {
        String fullPrompt = buildPrompt(prompt, context, null);
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);

        String cleaned = cleanResponse(response);
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        return new JSONObject(cleaned);
    }
}
