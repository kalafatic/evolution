package eu.kalafatic.evolution.servers.validation;

/**
 * Outcome of a single validation rule check.
 */
public class ValidationResult {
    private final String ruleId;
    private final boolean passed;
    private final String message;
    private final String severity;
    private final boolean blocking;

    public ValidationResult(String ruleId, boolean passed, String message, String severity, boolean blocking) {
        this.ruleId = ruleId;
        this.passed = passed;
        this.message = message;
        this.severity = severity;
        this.blocking = blocking;
    }

    public static ValidationResult ok(String ruleId) {
        return new ValidationResult(ruleId, true, null, "INFO", false);
    }

    public static ValidationResult failed(String ruleId, String message, String severity, boolean blocking) {
        return new ValidationResult(ruleId, false, message, severity, blocking);
    }

    public String getRuleId() { return ruleId; }
    public boolean isPassed() { return passed; }
    public String getMessage() { return message; }
    public String getSeverity() { return severity; }
    public boolean isBlocking() { return blocking; }
}
