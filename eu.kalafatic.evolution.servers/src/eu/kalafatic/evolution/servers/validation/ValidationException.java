package eu.kalafatic.evolution.servers.validation;

/**
 * Thrown when a STRICT-mode domain validation rule blocks an operation.
 * Security violations are never represented by this exception class.
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String ruleId;

    public ValidationException(String ruleId, String message) {
        super("Validation failed [" + ruleId + "]: " + message);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
