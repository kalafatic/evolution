package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import org.json.JSONArray;
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
        return "Role: Analytic Agent. Goal: Analyze user prompt or task failure with repository-first awareness.\n\n" +
                "STRICT OUTPUT RULE: You MUST output ONLY a single, valid JSON object. Do NOT include markdown code blocks (```json), conversational preamble, or follow-up text. Never output two JSON objects.\n\n" +
                "ANALYSIS CRITERIA (for new requests):\n" +
                "0. ARCHITECTURE FIRST: You must prioritize understanding the project's architecture and repository context before formulating any plans. Reference actual classes, methods, and orchestration patterns where relevant.\n" +
                "1. CATEGORY: CODING, RESEARCH, TOOL_USE, CHAT. Note: 'analyze', 'investigate', 'report', 'summarize', and 'discovery' tasks MUST be categorized as RESEARCH to ensure they follow the standard iterative flow and bypass evolutionary mutation.\n" +
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
        // Step 1: Perform Deep Intent Analysis using the specialized IntentAnalyzer
        IntentAnalyzer intentAnalyzer = new IntentAnalyzer(aiService);
        IntentAnalysisResult intentResult = intentAnalyzer.analyze(prompt, context);

        // Step 2: Map intentResult to the expected AnalyticAgent JSON schema
        // This avoids a second redundant LLM call for general prompts while ensuring
        // the kernel receives the structured data it needs.
        JSONObject analysis = new JSONObject();
        analysis.put("intent", "new");
        analysis.put("confidence", intentResult.getConfidenceScore());

        // Determine category based on intent goals and constraints
        String goalLower = intentResult.getGoal().toLowerCase();
        if (goalLower.matches(".*\\b(analyze|investigate|report|summarize|discovery|inspect|trace|audit)\\b.*")) {
            analysis.put("category", "RESEARCH");
        } else if (goalLower.matches(".*\\b(chat|hello|hi|hey|greetings)\\b.*")) {
            analysis.put("category", "CHAT");
        } else {
            analysis.put("category", "CODING");
        }

        analysis.put("objective", intentResult.getGoal());
        analysis.put("isAmbiguous", intentResult.isAmbiguous());

        JSONArray missingInfo = new JSONArray();
        intentResult.getMissingInformation().forEach(m -> {
            JSONObject mObj = new JSONObject();
            mObj.put("field", m.getField());
            mObj.put("description", m.getDescription());
            missingInfo.put(mObj);
        });
        analysis.put("missingInformation", missingInfo);

        analysis.put("contradictions", new JSONArray(intentResult.getContradictions()));
        analysis.put("clarificationQuestion", intentResult.getClarificationQuestion());

        // Construct a refined prompt if not provided by LLM (or use the original as base)
        analysis.put("refinedPrompt", prompt);

        // Enrich with structured intent for downstream components
        analysis.put("structuredIntent", new JSONObject()
            .put("goal", intentResult.getGoal())
            .put("language", intentResult.getLanguage())
            .put("framework", intentResult.getFramework())
            .put("targetPlatform", intentResult.getTargetPlatform())
            .put("expectedOutput", intentResult.getExpectedOutput())
            .put("constraints", intentResult.getConstraints())
            .put("confidence", intentResult.getConfidenceScore()));

        return analysis;
    }
}
