package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * Validator for Darwin evolutionary branch trajectories.
 * Ensures structural and conceptual completeness of a single trajectory proposal.
 */
public class DarwinVariantValidator {

    /**
     * Validates a raw LLM response for a single Darwin trajectory.
     * @param rawResponse The raw text from the LLM.
     * @param expectedType The expected strategy type.
     * @param context Task context for logging.
     * @return A validated JSONObject, or null if invalid.
     */
    public JSONObject validate(String rawResponse, DarwinStrategyType expectedType, TaskContext context) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return null;
        }

        // 1. Prohibit reasoning tags in output
        if (rawResponse.contains("<think>") || rawResponse.contains("</think>")) {
            if (context != null) context.log("[VALIDATOR] Error: Response contains hidden reasoning tags.");
            return null;
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

        // 4. Validate Required Fields (ID is injected by Spawner if missing)
        List<String> requiredFields = List.of("strategy_type", "strategy", "survival_argument", "semantic_justification", "tradeoffs", "failure_risks", "actions");
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
        if (strategy.length() < 10 || strategy.contains("<") || strategy.contains(">") || strategy.contains("precise engineering strategy")) {
            if (context != null) context.log("[VALIDATOR] Error: Invalid or placeholder strategy.");
            return null;
        }

        String reasoningFocus = json.optString("reasoning_focus");
        if (reasoningFocus.contains("<") || reasoningFocus.contains(">") || reasoningFocus.contains("specific architectural focus")) {
            if (context != null) context.log("[VALIDATOR] Error: Invalid or placeholder reasoning_focus.");
            return null;
        }

        String survival = json.optString("survival_argument");
        if (survival.length() < 10 || survival.contains("<") || survival.contains(">")) {
            if (context != null) context.log("[VALIDATOR] Error: Invalid or placeholder survival_argument.");
            return null;
        }

        String philosophy = json.optString("semantic_justification");
        if (philosophy.length() < 10 || philosophy.contains("<") || philosophy.contains(">")) {
            if (context != null) context.log("[VALIDATOR] Error: Invalid or placeholder semantic_justification.");
            return null;
        }

        JSONArray actions = json.optJSONArray("actions");
        if (actions == null || actions.length() == 0) {
            if (context != null) context.log("[VALIDATOR] Error: 'actions' field must be a non-empty array.");
            return null;
        }

        JSONArray selectedFiles = json.optJSONArray("selected_files");
        if (selectedFiles != null) {
            for (int i = 0; i < selectedFiles.length(); i++) {
                String path = selectedFiles.optString(i, "");
                if (path.contains("<") || path.contains(">") || path.contains("path/to/file")) {
                    if (context != null) context.log("[VALIDATOR] Error: Placeholder detected in selected_files: '" + path + "'.");
                    return null;
                }
            }
        }

        for (int i = 0; i < actions.length(); i++) {
            JSONObject action = actions.optJSONObject(i);
            if (action != null) {
                String op = action.optString("operation", "");
                String target = action.optString("target", "");
                String domain = action.optString("domain", "");

                if (op.contains("|") || op.contains("<") || op.contains(">")) {
                    if (context != null) context.log("[VALIDATOR] Error: Placeholder detected in action operation: '" + op + "'. Do not use '|' or '< >'.");
                    return null;
                }
                if (target.contains("<") || target.contains(">") || target.contains("actual_file_path")) {
                    if (context != null) context.log("[VALIDATOR] Error: Placeholder detected in action target: '" + target + "'. Provide a real path.");
                    return null;
                }
                if (domain.contains("|")) {
                    if (context != null) context.log("[VALIDATOR] Error: Placeholder detected in action domain: '" + domain + "'.");
                    return null;
                }
            }
        }

        return json;
    }
}
