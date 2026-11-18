package eu.kalafatic.evolution.servers.validation;

/**
 * Persisted configuration and last-check state of one validation rule.
 */
public class ValidationRule {
    private final String ruleId;
    private final ValidationCategory category;
    private final String description;
    private ValidationMode mode;
    private boolean enabled;
    private final String severity;
    private final boolean securityFixed;
    private final ValidationMode defaultMode;
    private String lastResult;
    private String lastCheckAt;

    public ValidationRule(String ruleId, ValidationCategory category, String description,
            ValidationMode defaultMode, String severity, boolean securityFixed) {
        this.ruleId = ruleId;
        this.category = category;
        this.description = description;
        this.defaultMode = defaultMode;
        this.mode = defaultMode;
        this.enabled = true;
        this.severity = severity != null ? severity : "WARNING";
        this.securityFixed = securityFixed;
    }

    public String getRuleId() { return ruleId; }
    public ValidationCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public ValidationMode getMode() { return mode; }
    public void setMode(ValidationMode mode) { this.mode = mode; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSeverity() { return severity; }
    public boolean isSecurityFixed() { return securityFixed; }
    public ValidationMode getDefaultMode() { return defaultMode; }
    public String getLastResult() { return lastResult; }
    public void setLastResult(String lastResult) { this.lastResult = lastResult; }
    public String getLastCheckAt() { return lastCheckAt; }
    public void setLastCheckAt(String lastCheckAt) { this.lastCheckAt = lastCheckAt; }
}
