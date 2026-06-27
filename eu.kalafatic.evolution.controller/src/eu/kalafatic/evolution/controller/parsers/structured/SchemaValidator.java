package eu.kalafatic.evolution.controller.parsers.structured;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates a JSONObject against a set of schema rules.
 */
public class SchemaValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }

    public ValidationResult validate(JSONObject json, Map<String, Class<?>> requiredFields) {
        List<String> errors = new ArrayList<>();

        if (json == null) {
            errors.add("JSON object is null");
            return new ValidationResult(false, errors);
        }

        for (Map.Entry<String, Class<?>> entry : requiredFields.entrySet()) {
            String field = entry.getKey();
            Class<?> type = entry.getValue();

            if (!json.has(field)) {
                errors.add("Missing required field: " + field);
                continue;
            }

            Object value = json.get(field);
            if (value == null || value == JSONObject.NULL) {
                errors.add("Required field is null: " + field);
                continue;
            }

            if (!type.isInstance(value)) {
                // Special handling for numbers since optDouble/optInt might return different types
                if (type == Double.class && (value instanceof Integer || value instanceof Long || value instanceof Double || value instanceof Float)) {
                    continue;
                }
                if (type == Integer.class && value instanceof Integer) {
                    continue;
                }

                errors.add("Field '" + field + "' has incorrect type. Expected " + type.getSimpleName() + " but got " + value.getClass().getSimpleName());
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
