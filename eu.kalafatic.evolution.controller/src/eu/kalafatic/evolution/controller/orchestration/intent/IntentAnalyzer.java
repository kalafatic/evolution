package eu.kalafatic.evolution.controller.orchestration.intent;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * Component for extracting structured intent from user requests.
 */
public class IntentAnalyzer {

    private final AiService aiService;

    public IntentAnalyzer(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Analyzes the user prompt and extracts structured intent.
     * @param prompt The raw user request.
     * @param context The task context.
     * @return The structured intent analysis result.
     * @throws Exception if analysis fails.
     */
    public IntentAnalysisResult analyze(String prompt, TaskContext context) throws Exception {
        String systemPrompt = "You are an Intent Extraction specialist. Your task is to convert raw user requests into structured intent JSON.\n" +
                "STRICT RULES:\n" +
                "1. Output MUST be ONLY a single JSON object.\n" +
                "2. Do NOT generate any code.\n" +
                "3. Ensure all fields are present.\n" +
                "4. Confidence score must be between 0.0 and 1.0.\n\n" +
                "OUTPUT SCHEMA:\n" +
                "{\n" +
                "  \"goal\": \"string\",\n" +
                "  \"language\": \"string\",\n" +
                "  \"framework\": \"string\",\n" +
                "  \"targetPlatform\": \"string\",\n" +
                "  \"expectedOutput\": \"string\",\n" +
                "  \"constraints\": [\"string\"],\n" +
                "  \"missingInformation\": [\n" +
                "    { \"field\": \"string\", \"description\": \"string\" }\n" +
                "  ],\n" +
                "  \"ambiguities\": [\n" +
                "    { \"part\": \"string\", \"reason\": \"string\" }\n" +
                "  ],\n" +
                "  \"confidenceScore\": float\n" +
                "}";

        String userPrompt = "Analyze the following user request:\n\n" + prompt;

        context.log("[INTENT ANALYZER] Analyzing intent for: " + prompt);
        String response = aiService.sendRequest(context.getOrchestrator(), systemPrompt + "\n\n" + userPrompt, context);
        context.log("[INTENT ANALYZER] Raw response: " + response);

        JSONObject json = JsonUtils.extractJsonObject(response);
        if (json == null) {
            throw new Exception("Failed to parse intent analysis JSON from response: " + response);
        }

        IntentAnalysisResult result = parseResult(json);

        // Final sanity check/adjustment of confidence if needed
        if (result.getConfidenceScore() == 0 && result.getGoal() != null) {
            result.setConfidenceScore(ConfidenceEvaluator.evaluate(result));
        }

        logResult(result, context);
        return result;
    }

    private IntentAnalysisResult parseResult(JSONObject json) {
        IntentAnalysisResult result = new IntentAnalysisResult();
        result.setGoal(json.optString("goal", ""));
        result.setLanguage(json.optString("language", ""));
        result.setFramework(json.optString("framework", ""));
        result.setTargetPlatform(json.optString("targetPlatform", ""));
        result.setExpectedOutput(json.optString("expectedOutput", ""));
        result.setConfidenceScore(json.optDouble("confidenceScore", 0.0));

        JSONArray constraints = json.optJSONArray("constraints");
        if (constraints != null) {
            for (int i = 0; i < constraints.length(); i++) {
                result.getConstraints().add(constraints.getString(i));
            }
        }

        JSONArray missing = json.optJSONArray("missingInformation");
        if (missing != null) {
            for (int i = 0; i < missing.length(); i++) {
                Object obj = missing.get(i);
                if (obj instanceof JSONObject) {
                    JSONObject m = (JSONObject) obj;
                    result.getMissingInformation().add(new MissingRequirement(
                            m.optString("field", ""),
                            m.optString("description", "")
                    ));
                } else if (obj instanceof String) {
                    result.getMissingInformation().add(new MissingRequirement("unknown", (String) obj));
                }
            }
        }

        JSONArray ambiguities = json.optJSONArray("ambiguities");
        if (ambiguities != null) {
            for (int i = 0; i < ambiguities.length(); i++) {
                Object obj = ambiguities.get(i);
                if (obj instanceof JSONObject) {
                    JSONObject a = (JSONObject) obj;
                    result.getAmbiguities().add(new Ambiguity(
                            a.optString("part", ""),
                            a.optString("reason", "")
                    ));
                } else if (obj instanceof String) {
                    result.getAmbiguities().add(new Ambiguity("unknown", (String) obj));
                }
            }
        }

        return result;
    }

    private void logResult(IntentAnalysisResult result, TaskContext context) {
        context.log("[INTENT ANALYZER] Extracted Intent:");
        context.log("  Goal: " + result.getGoal());
        context.log("  Language: " + result.getLanguage());
        context.log("  Framework: " + result.getFramework());
        context.log("  Confidence: " + result.getConfidenceScore());
        if (!result.getMissingInformation().isEmpty()) {
            context.log("  Missing Information: " + result.getMissingInformation());
        }
        if (!result.getAmbiguities().isEmpty()) {
            context.log("  Ambiguities: " + result.getAmbiguities());
        }
    }
}
