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
            if (context != null) {
                context.log("Stage: Parser\nJSON parsed: false\nFailure reason: Failed to parse JSON from response.");
                context.log("[VALIDATOR] Error: Failed to parse JSON from response.");
            }
            return null;
        }
        if (context != null) context.log("Stage: Parser\nJSON parsed: true");

        // 4. Validate Required Fields (ID and strategy_type are injected by Spawner if missing)
        List<String> requiredFields = List.of("strategy", "survival_argument", "tradeoffs", "failure_risks", "actions");
        List<String> errors = new java.util.ArrayList<>();
        for (String field : requiredFields) {
            if (!json.has(field)) {
                errors.add("Missing required field: " + field);
            }
        }

        // Semantic field flexibility
        if (!json.has("semantic_justification") && !json.has("semantic_anchor")) {
            errors.add("Missing semantic field (semantic_justification or semantic_anchor)");
        }

        // 5. Validate strategy_type if present
        if (json.has("strategy_type")) {
            String actualTypeStr = json.optString("strategy_type");
            try {
                DarwinStrategyType actualType = DarwinStrategyType.valueOf(actualTypeStr.toUpperCase());
                if (actualType != expectedType) {
                    errors.add("Strategy type mismatch. Expected " + expectedType + " but got " + actualType);
                }
            } catch (IllegalArgumentException e) {
                errors.add("Invalid strategy type: " + actualTypeStr);
            }
        }

        // 6. Validate minimum completeness and PROHIBIT PLACEHOLDERS
        String strategy = json.optString("strategy");
        if (strategy.length() < 10 || strategy.contains("<") || strategy.contains(">") || strategy.contains("precise engineering strategy")) {
            errors.add("Invalid or placeholder strategy: " + strategy);
        }

        String reasoningFocus = json.optString("reasoning_focus");
        if (reasoningFocus.contains("<") || reasoningFocus.contains(">") || reasoningFocus.contains("specific architectural focus")) {
            errors.add("Invalid or placeholder reasoning_focus: " + reasoningFocus);
        }

        String survival = json.optString("survival_argument");
        if (survival.length() < 10 || survival.contains("<") || survival.contains(">")) {
            errors.add("Invalid or placeholder survival_argument: " + survival);
        }

        String philosophy = json.has("semantic_justification") ? json.optString("semantic_justification") : json.optString("semantic_anchor");
        if (philosophy.length() < 10 || philosophy.contains("<") || philosophy.contains(">")) {
            errors.add("Invalid or placeholder semantic field: " + philosophy);
        }

        JSONArray actions = json.optJSONArray("actions");
        if (actions == null || actions.length() == 0) {
            errors.add("'actions' field must be a non-empty array.");
        } else {
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.optJSONObject(i);
                if (action != null) {
                    String op = action.optString("operation", "");
                    String target = action.optString("target", "");
                    String domain = action.optString("domain", "");

                    if (op.contains("|") || op.contains("<") || op.contains(">")) {
                        errors.add("Placeholder detected in action operation: '" + op + "'.");
                    }
                    if (target.contains("<") || target.contains(">") || target.contains("actual_file_path")) {
                        errors.add("Placeholder detected in action target: '" + target + "'.");
                    }
                    if (domain.contains("|")) {
                        errors.add("Placeholder detected in action domain: '" + domain + "'.");
                    }
                }
            }
        }

        JSONArray selectedFiles = json.optJSONArray("selected_files");
        if (selectedFiles != null) {
            for (int i = 0; i < selectedFiles.length(); i++) {
                String path = selectedFiles.optString(i, "");
                if (path.contains("<") || path.contains(">") || path.contains("path/to/file")) {
                    errors.add("Placeholder detected in selected_files: '" + path + "'.");
                }
            }
        }

        if (context != null) {
            context.log("Stage: Validator\nValid: " + errors.isEmpty() + "\nErrors: " + String.join("; ", errors));
        }

        if (!errors.isEmpty()) {
            for (String err : errors) {
                if (context != null) context.log("[VALIDATOR] Error: " + err);
            }
            return null;
        }

        return json;
    }
}
