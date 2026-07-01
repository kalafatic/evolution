package eu.kalafatic.evolution.controller.parsers.structured;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Validates the semantic content of an LLM response.
 */
public class SemanticValidator {

    public static class SemanticResult {
        private final boolean valid;
        private final List<String> errors;

        public SemanticResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }

    public SemanticResult validate(JSONObject json) {
        List<String> errors = new ArrayList<>();

        // 1. Reject template echoes or placeholders in common fields
        checkPlaceholders(json, errors, 0);

        // 2. Reject empty dimensions if unresolvedDimensions is expected
        if (json.has("unresolvedDimensions")) {
            JSONArray dimensions = json.optJSONArray("unresolvedDimensions");
            if (dimensions != null) {
                if (dimensions.length() == 0 && "CLEAR".equals(json.optString("state"))) {
                    // This might be okay, but often it's a failure to expand
                }

                Set<String> ids = new HashSet<>();
                for (int i = 0; i < dimensions.length(); i++) {
                    JSONObject dim = dimensions.optJSONObject(i);
                    if (dim != null) {
                        String id = dim.optString("id");
                        if (id == null || id.trim().isEmpty() || id.contains("|") || id.contains("string")) {
                            errors.add("Dimension " + i + " has invalid or placeholder ID: " + id);
                        }
                        if (ids.contains(id)) {
                            errors.add("Duplicate dimension ID: " + id);
                        }
                        ids.add(id);

                        String desc = dim.optString("description");
                        if (desc == null || desc.length() < 5 || desc.contains("string")) {
                            errors.add("Dimension " + id + " has insufficient or placeholder description");
                        }
                    }
                }
            }
        }

        return new SemanticResult(errors.isEmpty(), errors);
    }

    private void checkPlaceholders(JSONObject json, List<String> errors, int depth) {
        if (depth > 10) return; // Prevent infinite recursion
        for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            Object val = json.get(key);
            if (val instanceof String) {
                String s = (String) val;
                if (s.contains("|") && (s.contains("STRATEGY") || s.contains("PHILOSOPHY") || s.contains("CLEAR") || s.contains("NEEDS_CLARIFICATION"))) {
                    errors.add("Field '" + key + "' contains template placeholder: " + s);
                }
                if ("string".equals(s) || "your_rationale_here".equalsIgnoreCase(s)) {
                    errors.add("Field '" + key + "' contains generic placeholder: " + s);
                }
            } else if (val instanceof JSONObject) {
                checkPlaceholders((JSONObject) val, errors, depth + 1);
            } else if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                for (int i = 0; i < arr.length(); i++) {
                    Object item = arr.opt(i);
                    if (item instanceof JSONObject) {
                        checkPlaceholders((JSONObject) item, errors, depth + 1);
                    }
                }
            }
        }
    }
}
