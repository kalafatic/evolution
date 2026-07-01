package eu.kalafatic.evolution.controller.agents;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionEngine;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
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

    public AnalyticAgent(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("Analytic", "Analytic", container);
    }

    @Override
    // @evo:14:B reason=flexible-analysis
    protected String getAgentInstructions() {
        return "Role: Analytic Agent. Goal: Analyze user prompt or task failure with repository-first awareness.\n\n" +
                "STRICT OUTPUT RULE: You MUST output ONLY a single, valid JSON object. Do NOT include markdown code blocks (```json), conversational preamble, or follow-up text. Never output two JSON objects.\n\n" +
                "ANALYSIS CRITERIA (for new requests):\n" +
                "0. ARCHITECTURE FIRST: You must prioritize understanding the project's architecture and repository context before formulating any plans. Reference actual classes, methods, and orchestration patterns where relevant.\n" +
                "1. CATEGORY: CODING, RESEARCH, TOOL_USE, CHAT. Note: 'analyze', 'investigate', 'report', and 'summarize' tasks should be categorized as RESEARCH to ensure they follow the standard iterative flow and bypass evolutionary mutation.\n" +
                "2. INTENT: 'new' (task request), 'continue' (follow-up), 'chat' (greeting/casual), 'unclear'.\n" +
                "3. AMBIGUITY: ATOMIC tasks (e.g., 'create class', 'write file') are NOT ambiguous. If isAmbiguous is false, 'clarificationQuestion', 'missingInformation' and 'contradictions' MUST be empty strings/arrays. DO NOT hallucinate requirements not in the original prompt.\n" +
                "4. CONTRADICTIONS: Detect if the user request contains conflicting instructions (e.g., 'use Java but also use Python for the same class').\n" +
                "5. REFINED PROMPT: Create an actionable version of the prompt with assumed defaults. It should stay faithful to the original intent.\n\n" +
                "DIAGNOSIS CRITERIA (for failures):\n" +
                "1. ROOT CAUSE: syntactic, logical, or environment.\n" +
                "2. PROGRESS: IMPROVED, SAME, or WORSE compared to previous attempt.\n" +
                "3. STRATEGY: RETRY, REPAIR_AGENT, or ESCALATE.\n\n" +
                "OUTPUT SCHEMA (Choose ONLY ONE based on the task - either NEW REQUEST or DIAGNOSIS):\n" +
                "NEW REQUEST (for fresh prompts):\n" +
                "{\n" +
                "  \"intent\": \"new | continue | chat | unclear\",\n" +
                "  \"confidence\": 0.0-1.0,\n" +
                "  \"category\": \"...\",\n" +
                "  \"objective\": \"...\",\n" +
                "  \"isAmbiguous\": boolean,\n" +
                "  \"missingInformation\": [],\n" +
                "  \"contradictions\": [],\n" +
                "  \"clarificationQuestion\": \"...\",\n" +
                "  \"refinedPrompt\": \"...\"\n" +
                "}\n" +
                "DIAGNOSIS (for failure analysis):\n" +
                "{\n" +
                "  \"rootCause\": \"...\",\n" +
                "  \"repeatFailure\": boolean,\n" +
                "  \"progress\": \"IMPROVED | SAME | WORSE\",\n" +
                "  \"suggestedStrategy\": \"RETRY | REPAIR_AGENT | ESCALATE\",\n" +
                "  \"explanation\": \"...\"\n" +
                "}";
    }

    public JSONObject diagnose(String result, String feedback, TaskContext context) throws Exception {
        String input = "EXECUTION RESULT: " + result + "\nFEEDBACK: " + feedback;
        String fullPrompt = buildPrompt(input, context, null);

        context.log("Evo-Analytic-Diagnosis-Thinking: " + fullPrompt);
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-Analytic-Diagnosis-Response: " + response);

        JSONObject diagnosis = JsonUtils.extractJsonObject(response);

        if (diagnosis == null) {
            diagnosis = new JSONObject();
            diagnosis.put("rootCause", "Unknown");
            diagnosis.put("repeatFailure", false);
            diagnosis.put("suggestedStrategy", "RETRY");
            diagnosis.put("explanation", "Failed to parse AI response as diagnosis: " + response);
        }
        return diagnosis;
    }

    // @evo:14:B reason=traceability-support
    public JSONObject analyze(String prompt, TaskContext context) throws Exception {
        // Step 1: Perform Deep Intent Analysis via Expansion Engine
        IntentExpansionEngine intentExpansionEngine = new IntentExpansionEngine(getSessionContainer());
        intentExpansionEngine.setAiService(aiService);
        IntentExpansionResult intentResult = intentExpansionEngine.expand(prompt, context);

        // Step 2: Fallback to existing logic for Category/RefinedPrompt/Clarification if needed
        // or map intentResult back to expected JSON for IterationManager compatibility
        String fullPrompt = buildPrompt(prompt, context, null);
        context.log("Evo-Analytic-Thinking: " + fullPrompt);
        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-Analytic-Response: " + response);

        JSONObject analysis = JsonUtils.extractJsonObject(response);

        if (analysis == null) {
             analysis = new JSONObject();
             analysis.put("intent", "new");
             analysis.put("confidence", intentResult.getConfidence().getOverallConfidence());
             analysis.put("category", "CODING");
             analysis.put("isAmbiguous", intentResult.isAmbiguous());
             analysis.put("refinedPrompt", prompt);
        }

        // Ensure mandatory keys for Policy Engine if not present
        if (!analysis.has("intent")) {
            analysis.put("intent", "new");
        }

        // Enrich with structured intent if available
        analysis.put("structuredIntent", new JSONObject()
            .put("goal", intentResult.getDominantIntent())
            .put("language", intentResult.getLanguage())
            .put("framework", intentResult.getFramework())
            .put("targetPlatform", intentResult.getTargetPlatform())
            .put("expectedOutput", intentResult.getExpectedOutput())
            .put("constraints", intentResult.getConstraints())
            .put("confidence", intentResult.getConfidence().getOverallConfidence()));

        if (!analysis.has("confidence")) {
            analysis.put("confidence", intentResult.getConfidence().getOverallConfidence());
        }

        return analysis;
    }
}
