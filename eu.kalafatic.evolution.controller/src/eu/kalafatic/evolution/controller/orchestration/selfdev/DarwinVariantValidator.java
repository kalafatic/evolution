package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import java.util.List;
import java.util.ArrayList;

/**
 * Validator for Darwin evolutionary branch variants.
 * Ensures structural and semantic completeness of a single variant proposal.
 */
public class DarwinVariantValidator {

    /**
     * Validates a raw LLM response for a single Darwin variant.
     * @param rawResponse The raw text from the LLM.
     * @param expectedType The expected strategy type.
     * @param context Task context for logging.
     * @return A validated JSONObject, or null if invalid.
     */
    public JSONObject validate(String rawResponse, DarwinStrategyType expectedType, TaskContext context) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return null;
        }

        // 1. Prohibit Markdown and conversation text
        if (rawResponse.contains("```") || rawResponse.toLowerCase().contains("here is") || rawResponse.toLowerCase().contains("i have generated")) {
             // We try to extract JSON anyway but log a warning if it's too noisy
             if (context != null) context.log("[VALIDATOR] Warning: Noisy response detected. Attempting JSON extraction.");
        }

        // 2. Prohibit Arrays
        if (rawResponse.trim().startsWith("[")) {
            if (context != null) context.log("[VALIDATOR] Error: LLM returned an array instead of a single object.");
            return null;
        }

        // 3. Parse JSON
        JSONObject json = JsonUtils.extractJsonObject(rawResponse);
        if (json == null) {
            if (context != null) context.log("[VALIDATOR] Error: Failed to parse JSON from response.");
            return null;
        }

        // 4. Validate Required Fields
        List<String> requiredFields = List.of("id", "strategy_type", "strategy", "survival_argument", "actions");
        for (String field : requiredFields) {
            if (!json.has(field)) {
                if (context != null) context.log("[VALIDATOR] Error: Missing required field: " + field);
                return null;
            }
        }

        // 5. Validate strategy_type
        String actualTypeStr = json.optString("strategy_type");
        try {
            DarwinStrategyType actualType = DarwinStrategyType.valueOf(actualTypeStr.toUpperCase());
            if (actualType != expectedType) {
                if (context != null) context.log("[VALIDATOR] Error: Strategy type mismatch. Expected " + expectedType + " but got " + actualType);
                return null;
            }
        } catch (IllegalArgumentException e) {
            if (context != null) context.log("[VALIDATOR] Error: Invalid strategy type: " + actualTypeStr);
            return null;
        }

        // 6. Validate minimum completeness and PROHIBIT PLACEHOLDERS
        String strategy = json.optString("strategy");
        if (strategy.length() < 10) {
            if (context != null) context.log("[VALIDATOR] Error: Strategy description too short.");
            return null;
        }

        if (strategy.toLowerCase().contains("high-level intent description") || strategy.contains("<") || strategy.contains(">")) {
            if (context != null) context.log("[VALIDATOR] Error: Variant contains literal placeholder text in 'strategy'.");
            return null;
        }

        String survival = json.optString("survival_argument");
        if (survival.toLowerCase().contains("justification of why this trajectory is valuable") || survival.contains("<") || survival.contains(">")) {
            if (context != null) context.log("[VALIDATOR] Error: Variant contains literal placeholder text in 'survival_argument'.");
            return null;
        }

        JSONArray actions = json.optJSONArray("actions");
        if (actions == null) {
            if (context != null) context.log("[VALIDATOR] Error: 'actions' field must be an array.");
            return null;
        }

        // Prohibit reasoning tags in output
        String fullJson = json.toString();
        if (fullJson.contains("<think>") || fullJson.contains("</think>")) {
            if (context != null) context.log("[VALIDATOR] Error: Response contains hidden reasoning tags.");
            return null;
        }

        return json;
    }
}
