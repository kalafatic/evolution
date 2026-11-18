package eu.kalafatic.evolution.servers.validation;

/**
 * Behaviour mode of a validation rule or category.
 * DISABLED skips optional domain checks, WARN reports but continues,
 * STRICT blocks the operation on failure. Security checks are never
 * subject to this policy.
 */
public enum ValidationMode {
    DISABLED, WARN, STRICT;

    public static ValidationMode fromString(String value) {
        if (value == null) return DISABLED;
        try {
            return ValidationMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DISABLED;
        }
    }
}
