package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
     * Distinguishes between fatal structural errors and recoverable omissions.
     * @param rawResponse The raw text from the LLM.
     * @param expectedType The expected strategy type.
     * @param context Task context for logging.
     * @return A validated JSONObject, or null if fatally invalid.
     */
    public JSONObject validate(String rawResponse, DarwinStrategyType expectedType, TaskContext context) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return null;
        }

        // 1. Prohibit reasoning tags in output (Fatal)
        if (rawResponse.contains("<think>") || rawResponse.contains("</think>")) {
            if (context != null) context.log("[VALIDATOR] Fatal Error: Response contains hidden reasoning tags.");
            return null;
        }

        // 2. Prohibit Arrays (Fatal)
        if (rawResponse.trim().startsWith("[")) {
            if (context != null) context.log("[VALIDATOR] Fatal Error: LLM returned an array instead of a single object.");
            return null;
        }

        // 3. Parse JSON (Fatal)
        JSONObject json = JsonUtils.extractJsonObject(rawResponse);
        if (json == null) {
            if (context != null) {
                context.log("Stage: Parser\nJSON parsed: false\nFailure reason: Failed to parse JSON from response.");
                context.log("[VALIDATOR] Fatal Error: Failed to parse JSON from response.");
            }
            return null;
        }
        if (context != null) context.log("Stage: Parser\nJSON parsed: true");

        List<String> fatalErrors = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();

        // 4. Validate Mandatory Architectural Fields (Fatal if missing)
        if (!json.has("strategy") || json.optString("strategy").isEmpty()) {
            fatalErrors.add("Missing required field: strategy");
        }

        // 5. Validate Recoverable Fields (Warning if missing)
        if (!json.has("semantic_justification") && !json.has("semantic_anchor")) {
            warnings.add("Missing semantic field (semantic_justification or semantic_anchor) (Recoverable)");
        }

        List<String> recoverableFields = List.of("survival_argument", "tradeoffs", "failure_risks", "projected_steps");
        for (String field : recoverableFields) {
            if (!json.has(field) || json.isNull(field) || (json.get(field) instanceof String && ((String)json.get(field)).isEmpty())) {
                warnings.add("Missing field: " + field + " (Recoverable)");
            }
        }

        // 5.5 Validate Mandatory Action Field (Fatal if missing or empty)
        if (!json.has("actions") || json.isNull("actions") || json.optJSONArray("actions").length() == 0) {
            fatalErrors.add("Missing or empty required field: actions. Variants must contain at least one explicit action.");
        }

        // 6. Validate strategy_type if present (Recoverable - Spawner/Planner can fix)
        if (json.has("strategy_type")) {
            String actualTypeStr = json.optString("strategy_type");
            try {
                DarwinStrategyType actualType = DarwinStrategyType.valueOf(actualTypeStr.toUpperCase());
                if (actualType != expectedType) {
                    warnings.add("Strategy type mismatch. Expected " + expectedType + " but got " + actualType);
                }
            } catch (IllegalArgumentException e) {
                warnings.add("Invalid strategy type: " + actualTypeStr);
            }
        }

        // 7. Validate architectural completeness and PROHIBIT PLACEHOLDERS (Fatal)
        String strategy = json.optString("strategy");
        if (strategy.length() < 10 || strategy.contains("<") || strategy.contains(">") || strategy.contains("precise engineering strategy")) {
            fatalErrors.add("Invalid or placeholder strategy: " + strategy);
        }

        String philosophy = json.has("semantic_justification") ? json.optString("semantic_justification") : json.optString("semantic_anchor");
        if (philosophy != null && (philosophy.length() < 10 || philosophy.contains("<") || philosophy.contains(">"))) {
            fatalErrors.add("Invalid or placeholder semantic field: " + philosophy);
        }

        // 8. Validate Recoverable field quality (Warnings)
        String survival = json.optString("survival_argument");
        if (!survival.isEmpty() && (survival.length() < 10 || survival.contains("<") || survival.contains(">"))) {
            warnings.add("Invalid or placeholder survival_argument (Recoverable)");
        }

        JSONArray actions = json.optJSONArray("actions");
        if (actions != null) {
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.optJSONObject(i);
                if (action != null) {
                    String op = action.optString("operation", "");
                    String target = action.optString("target", "");
                    if (op.contains("<") || op.contains(">") || target.contains("<") || target.contains(">") || target.contains("actual_file_path") || target.equals(".") || target.equals("workspace")) {
                        fatalErrors.add("Placeholder or generic target detected in action: " + target);
                    }
                    if ("WRITE".equals(op) && (!action.has("implementation") || action.optString("implementation").isEmpty())) {
                        fatalErrors.add("WRITE operation missing implementation code for target: " + target);
                    }
                }
            }
        }

        if (context != null) {
            context.log("Stage: Validator\nFatal Errors: " + fatalErrors.size() + "\nWarnings: " + warnings.size());
            if (!fatalErrors.isEmpty()) context.log("[VALIDATOR] Fatal Errors: " + String.join("; ", fatalErrors));
            if (!warnings.isEmpty()) context.log("[VALIDATOR] Warnings: " + String.join("; ", warnings));
        }

        if (!fatalErrors.isEmpty()) {
            return null;
        }

        return json;
    }
}
