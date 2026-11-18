package eu.kalafatic.evolution.servers.validation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.service.AuthorizationService;

/**
 * Central validation policy for Cognitive Services domain validation.
 *
 * Security checks (authentication, authorization, isolation, input safety,
 * database integrity) are NEVER governed by this policy - they are enforced
 * unconditionally in the services themselves. This service controls only
 * optional domain-level completeness/consistency validation.
 *
 * Default installation: Strict Application Validation = OFF (global mode
 * DISABLED). Categories can later be switched to WARN or STRICT without code
 * changes; changes are persisted, audited and take effect for new operations.
 */
public class ValidationPolicyService {
    private static final Logger LOGGER = Logger.getLogger(ValidationPolicyService.class.getName());

    public static final String GLOBAL_STRICT_RULE_ID = "global.strict";

    private static ValidationPolicyService instance;

    private final DatabaseManager dbManager;
    private final Map<String, ValidationRule> rules = new ConcurrentHashMap<>();
    private final Map<String, Supplier<ValidationResult>> checks = new ConcurrentHashMap<>();
    private boolean seeded = false;

    public static synchronized ValidationPolicyService getInstance() {
        if (instance == null) {
            instance = new ValidationPolicyService(new DatabaseManager());
        }
        return instance;
    }

    public static synchronized ValidationPolicyService getInstance(DatabaseManager dbManager) {
        if (instance == null) {
            instance = new ValidationPolicyService(dbManager);
        }
        return instance;
    }

    public ValidationPolicyService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        seedCatalog();
        load();
    }

    // ------------------------------------------------------------------
    // Rule catalog
    // ------------------------------------------------------------------

    private void seedCatalog() {
        rules.put(GLOBAL_STRICT_RULE_ID,
                new ValidationRule(GLOBAL_STRICT_RULE_ID, ValidationCategory.DATA_COMPLETENESS,
                        "Global strict application validation switch", ValidationMode.DISABLED, "INFO", false));

        catalog("USER_PROFILE.displayName", ValidationCategory.USER_PROFILE, "User profile has a display name", false);
        catalog("USER_PROFILE.locale", ValidationCategory.USER_PROFILE, "User profile has a locale", false);
        catalog("USER_PROFILE.workingHints", ValidationCategory.USER_PROFILE, "User profile has working hints", false);

        catalog("USER_PREFERENCES.confidence", ValidationCategory.USER_PREFERENCES, "Preferences carry a valid confidence value", false);
        catalog("USER_PREFERENCES.expiration", ValidationCategory.USER_PREFERENCES, "Temporary preferences carry an expiration", false);
        catalog("USER_PREFERENCES.contradiction", ValidationCategory.USER_PREFERENCES, "Contradicting explicit preferences are detected", false);

        catalog("APPLICATION_METADATA.description", ValidationCategory.APPLICATION_METADATA, "Applications have a description", false);
        catalog("APPLICATION_METADATA.kind", ValidationCategory.APPLICATION_METADATA, "Applications have a known kind", false);
        catalog("APPLICATION_METADATA.owner", ValidationCategory.APPLICATION_METADATA, "Applications have an owner user", false);

        catalog("SESSION.projectRef", ValidationCategory.SESSION, "Sessions carry a project reference when applicable", false);
        catalog("SESSION.ownerRef", ValidationCategory.SESSION, "Sessions carry an owner reference", false);
        catalog("SESSION.parentRef", ValidationCategory.SESSION, "Session relationships reference existing sessions", false);

        catalog("CONVERSATION.toolRecords", ValidationCategory.CONVERSATION, "Tool calls are recorded with results", false);
        catalog("CONVERSATION.messageRoles", ValidationCategory.CONVERSATION, "Conversation messages carry valid roles", false);
        catalog("CONVERSATION.compaction", ValidationCategory.CONVERSATION, "Conversations are compacted when exceeding budget", false);

        catalog("MEMORY.contentRequired", ValidationCategory.MEMORY, "Memory entries have content", false);
        catalog("MEMORY.classification", ValidationCategory.MEMORY, "Memory entries have a type and scope classification", false);
        catalog("MEMORY.relationships", ValidationCategory.MEMORY, "Memory relationships reference existing entries", false);
        catalog("MEMORY.confidenceRange", ValidationCategory.MEMORY, "Memory confidence is within 0..1", false);
        catalog("MEMORY.provenance", ValidationCategory.MEMORY, "Memory entries carry provenance", false);

        catalog("PROJECT_KNOWLEDGE.componentSummaries", ValidationCategory.PROJECT_KNOWLEDGE, "Project components have summaries", false);
        catalog("PROJECT_KNOWLEDGE.decisionStatus", ValidationCategory.PROJECT_KNOWLEDGE, "Decisions carry a valid status", false);
        catalog("PROJECT_KNOWLEDGE.buildRefs", ValidationCategory.PROJECT_KNOWLEDGE, "Build and test results reference a project", false);

        catalog("CONTEXT.categoriesComplete", ValidationCategory.CONTEXT, "Requested context categories are covered", false);
        catalog("CONTEXT.budgetFits", ValidationCategory.CONTEXT, "Context package fits the token budget", false);
        catalog("CONTEXT.sourcesAnnotated", ValidationCategory.CONTEXT, "Context items carry source annotations", false);

        catalog("KNOWLEDGE_RELATIONSHIPS.relatedTargets", ValidationCategory.KNOWLEDGE_RELATIONSHIPS, "Knowledge relationships reference existing targets", false);
        catalog("KNOWLEDGE_RELATIONSHIPS.supersessionChain", ValidationCategory.KNOWLEDGE_RELATIONSHIPS, "Supersession chains are complete", false);

        catalog("DATA_COMPLETENESS.requiredFields", ValidationCategory.DATA_COMPLETENESS, "Required fields are present", false);
        catalog("DATA_COMPLETENESS.metadataQuality", ValidationCategory.DATA_COMPLETENESS, "Metadata is complete enough for retrieval", false);

        // Security-fixed rules: informational, never disableable, always active.
        catalog("SECURITY.authentication", ValidationCategory.SECURITY, "Caller authentication is verified", true);
        catalog("SECURITY.authorization", ValidationCategory.SECURITY, "Caller authorization and scopes are verified", true);
        catalog("SECURITY.applicationIdentity", ValidationCategory.SECURITY, "Application identity is validated", true);
        catalog("SECURITY.userDataAccess", ValidationCategory.SECURITY, "User data access is validated", true);
        catalog("SECURITY.appIsolation", ValidationCategory.SECURITY, "Application data isolation is enforced", true);
        catalog("SECURITY.projectAccess", ValidationCategory.SECURITY, "Project access is validated", true);
        catalog("SECURITY.sessionAccess", ValidationCategory.SECURITY, "Session access is validated", true);
        catalog("SECURITY.inputValidation", ValidationCategory.SECURITY, "Inputs are validated", true);
        catalog("SECURITY.sqlInjection", ValidationCategory.SECURITY, "Parameterized queries are used", true);
        catalog("SECURITY.unauthorizedWrite", ValidationCategory.SECURITY, "Unauthorized writes are blocked", true);
        catalog("SECURITY.dbIntegrity", ValidationCategory.SECURITY, "Database integrity is maintained", true);
        catalog("SECURITY.resourceLimits", ValidationCategory.SECURITY, "Input size and resource limits are enforced", true);
        catalog("SECURITY.sessionIsolation", ValidationCategory.SECURITY, "Foreign session access is blocked", true);
        catalog("SECURITY.audit", ValidationCategory.SECURITY, "Security-relevant operations are audited", true);
    }

    private void catalog(String ruleId, ValidationCategory category, String description, boolean securityFixed) {
        rules.put(ruleId, new ValidationRule(ruleId, category, description, ValidationMode.DISABLED,
                securityFixed ? "CRITICAL" : "WARNING", securityFixed));
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    private synchronized void ensureSeeded() {
        if (seeded) return;
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String upsert = "INSERT OR IGNORE INTO validation_policy_config (rule_id, category, description, mode, enabled, severity, security_fixed, default_mode, warnings, errors, last_result, last_check_at, updated_at, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, NULL, NULL, ?, 'SYSTEM')";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsert)) {
            for (ValidationRule rule : rules.values()) {
                pstmt.setString(1, rule.getRuleId());
                pstmt.setString(2, rule.getCategory().name());
                pstmt.setString(3, rule.getDescription());
                pstmt.setString(4, rule.getDefaultMode().name());
                pstmt.setBoolean(5, rule.isEnabled());
                pstmt.setString(6, rule.getSeverity());
                pstmt.setBoolean(7, rule.isSecurityFixed());
                pstmt.setString(8, rule.getDefaultMode().name());
                pstmt.setString(9, now);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            seeded = true;
        } catch (SQLException e) {
            LOGGER.severe("Failed to seed validation policy: " + e.getMessage());
        }
    }

    private synchronized void load() {
        ensureSeeded();
        String sql = "SELECT * FROM validation_policy_config";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String ruleId = rs.getString("rule_id");
                ValidationRule rule = rules.get(ruleId);
                if (rule == null) {
                    rule = new ValidationRule(ruleId,
                            ValidationCategory.valueOf(rs.getString("category")),
                            rs.getString("description"),
                            ValidationMode.fromString(rs.getString("default_mode")),
                            rs.getString("severity"),
                            rs.getBoolean("security_fixed"));
                    rules.put(ruleId, rule);
                }
                rule.setMode(ValidationMode.fromString(rs.getString("mode")));
                rule.setEnabled(rs.getBoolean("enabled"));
                rule.setLastResult(rs.getString("last_result"));
                rule.setLastCheckAt(rs.getString("last_check_at"));
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load validation policy: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------------

    /** Returns the effective mode for a rule considering global strict switch and enablement. */
    public ValidationMode getEffectiveMode(String ruleId) {
        ValidationRule rule = rules.get(ruleId);
        if (rule == null) return ValidationMode.DISABLED;
        if (rule.isSecurityFixed()) return ValidationMode.STRICT;
        if (!rule.isEnabled()) return ValidationMode.DISABLED;
        ValidationRule global = rules.get(GLOBAL_STRICT_RULE_ID);
        if (global != null && global.getMode() == ValidationMode.STRICT) {
            return ValidationMode.STRICT;
        }
        return rule.getMode();
    }

    /**
     * Evaluates a domain check honoring the policy mode. Never throws for
     * domain rules; blocking results are flagged for callers that choose to
     * enforce them.
     */
    public ValidationResult evaluate(String ruleId, Supplier<ValidationResult> check) {
        ValidationMode mode = getEffectiveMode(ruleId);
        if (mode == ValidationMode.DISABLED) {
            return ValidationResult.ok(ruleId);
        }
        ValidationResult result;
        try {
            result = check.get();
            if (result == null) {
                result = ValidationResult.ok(ruleId);
            }
        } catch (Exception e) {
            result = ValidationResult.failed(ruleId, e.getMessage(), "ERROR", mode == ValidationMode.STRICT);
        }
        boolean blocking = mode == ValidationMode.STRICT && !result.isPassed();
        ValidationResult effective = blocking
                ? ValidationResult.failed(ruleId, result.getMessage(), result.getSeverity(), true)
                : result;
        recordResult(ruleId, effective);
        return effective;
    }

    /** Strict enforcement variant: throws ValidationException when a STRICT rule fails. */
    public void enforce(String ruleId, Supplier<ValidationResult> check) {
        ValidationResult result = evaluate(ruleId, check);
        if (result.isBlocking() || (getEffectiveMode(ruleId) == ValidationMode.STRICT && !result.isPassed())) {
            throw new ValidationException(ruleId, result.getMessage());
        }
    }

    private void recordResult(String ruleId, ValidationResult result) {
        ValidationRule rule = rules.get(ruleId);
        if (rule == null) return;
        rule.setLastResult(result.isPassed() ? "PASSED" : "FAILED: " + result.getMessage());
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        rule.setLastCheckAt(now);
        String sql;
        if (result.isPassed()) {
            sql = "UPDATE validation_policy_config SET last_result = ?, last_check_at = ? WHERE rule_id = ?";
        } else if (result.isBlocking()) {
            sql = "UPDATE validation_policy_config SET last_result = ?, last_check_at = ?, errors = errors + 1 WHERE rule_id = ?";
        } else {
            sql = "UPDATE validation_policy_config SET last_result = ?, last_check_at = ?, warnings = warnings + 1 WHERE rule_id = ?";
        }
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rule.getLastResult());
            pstmt.setString(2, now);
            pstmt.setString(3, ruleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("Failed to record validation result: " + e.getMessage());
        }
    }

    /**
     * Registers the executable implementation of a catalog rule. Registered
     * checks are run by runCheck/runAllChecks (administration "Run checks").
     */
    public void registerCheck(String ruleId, Supplier<ValidationResult> check) {
        if (rules.containsKey(ruleId) && check != null) {
            checks.put(ruleId, check);
        }
    }

    public boolean hasCheck(String ruleId) {
        return checks.containsKey(ruleId);
    }

    public ValidationResult runCheck(String ruleId) {
        Supplier<ValidationResult> check = checks.get(ruleId);
        if (check == null) {
            ValidationResult noCheck = new ValidationResult(ruleId, true, null, "INFO", false);
            recordResult(ruleId, noCheck);
            return noCheck;
        }
        return evaluate(ruleId, check);
    }

    /** Runs all registered checks and returns results for rules with implementations. */
    public Map<String, ValidationResult> runAllChecks() {
        Map<String, ValidationResult> results = new LinkedHashMap<>();
        for (String ruleId : checks.keySet()) {
            results.put(ruleId, runCheck(ruleId));
        }
        return results;
    }

    // ------------------------------------------------------------------
    // Administration
    // ------------------------------------------------------------------

    public boolean isGlobalStrict() {
        ValidationRule global = rules.get(GLOBAL_STRICT_RULE_ID);
        return global != null && global.getMode() == ValidationMode.STRICT;
    }

    public void setGlobalStrict(boolean strict, String changedBy) {
        ValidationRule global = rules.get(GLOBAL_STRICT_RULE_ID);
        if (global == null) return;
        changeMode(global.getRuleId(), strict ? ValidationMode.STRICT : ValidationMode.DISABLED, changedBy, "Global strict validation switch");
        reloadRule(global.getRuleId());
    }

    public void setRuleMode(String ruleId, ValidationMode mode, String changedBy, String reason) {
        ValidationRule rule = rules.get(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown validation rule: " + ruleId);
        }
        if (rule.isSecurityFixed() && mode != ValidationMode.STRICT) {
            throw new IllegalArgumentException("Security rule cannot be modified: " + ruleId);
        }
        changeMode(ruleId, mode, changedBy, reason);
        reloadRule(ruleId);
    }

    public void setRuleEnabled(String ruleId, boolean enabled, String changedBy) {
        ValidationRule rule = rules.get(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown validation rule: " + ruleId);
        }
        if (rule.isSecurityFixed() && !enabled) {
            throw new IllegalArgumentException("Security rule cannot be disabled: " + ruleId);
        }
        String sql = "UPDATE validation_policy_config SET enabled = ?, updated_at = ?, updated_by = ? WHERE rule_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, enabled);
            pstmt.setString(2, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(3, changedBy);
            pstmt.setString(4, ruleId);
            pstmt.executeUpdate();
            appendHistory(ruleId, rule.getCategory().name(), "ENABLED=" + enabled, "ENABLED=" + !enabled, changedBy, "Rule enablement change");
        } catch (SQLException e) {
            LOGGER.severe("Failed to change rule enablement: " + e.getMessage());
        }
        reloadRule(ruleId);
    }

    public void setCategoryMode(ValidationCategory category, ValidationMode mode, String changedBy) {
        for (ValidationRule rule : rules.values()) {
            if (rule.getCategory() == category && !rule.isSecurityFixed()) {
                setRuleMode(rule.getRuleId(), mode, changedBy, "Category-level mode change");
            }
        }
    }

    public void resetDefaults(String changedBy) {
        for (ValidationRule rule : rules.values()) {
            if (rule.isSecurityFixed()) continue;
            setRuleMode(rule.getRuleId(), rule.getDefaultMode(), changedBy, "Reset to defaults");
            setRuleEnabled(rule.getRuleId(), true, changedBy);
        }
        setGlobalStrict(false, changedBy);
    }

    private void changeMode(String ruleId, ValidationMode mode, String changedBy, String reason) {
        String sql = "UPDATE validation_policy_config SET mode = ?, updated_at = ?, updated_by = ? WHERE rule_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, mode.name());
            pstmt.setString(2, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(3, changedBy);
            pstmt.setString(4, ruleId);
            pstmt.executeUpdate();
            appendHistory(ruleId, rules.containsKey(ruleId) ? rules.get(ruleId).getCategory().name() : "UNKNOWN",
                    mode.name(), rules.containsKey(ruleId) ? rules.get(ruleId).getMode().name() : "UNKNOWN",
                    changedBy, reason);
        } catch (SQLException e) {
            LOGGER.severe("Failed to change rule mode: " + e.getMessage());
        }
    }

    private void appendHistory(String ruleId, String category, String newMode, String oldMode, String changedBy, String reason) {
        String sql = "INSERT INTO validation_policy_history (timestamp, rule_id, category, old_mode, new_mode, changed_by, change_reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(2, ruleId);
            pstmt.setString(3, category);
            pstmt.setString(4, oldMode);
            pstmt.setString(5, newMode);
            pstmt.setString(6, changedBy);
            pstmt.setString(7, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("Failed to append validation history: " + e.getMessage());
        }
        try {
            AuthorizationService.getInstance(dbManager).logAudit(null, changedBy, "VALIDATION_POLICY_CHANGED",
                    "VALIDATION", ruleId, "SUCCESS", oldMode + " -> " + newMode + " (" + reason + ")");
        } catch (Exception e) {
            LOGGER.warning("Failed to audit validation change: " + e.getMessage());
        }
    }

    private void reloadRule(String ruleId) {
        String sql = "SELECT * FROM validation_policy_config WHERE rule_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ruleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ValidationRule rule = rules.get(ruleId);
                    if (rule != null) {
                        rule.setMode(ValidationMode.fromString(rs.getString("mode")));
                        rule.setEnabled(rs.getBoolean("enabled"));
                        rule.setLastResult(rs.getString("last_result"));
                        rule.setLastCheckAt(rs.getString("last_check_at"));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to reload rule: " + e.getMessage());
        }
    }

    public List<ValidationRule> getRules() {
        ensureSeeded();
        List<ValidationRule> list = new ArrayList<>(rules.values());
        list.sort(Comparator.comparing(r -> r.getRuleId()));
        return list;
    }

    public ValidationRule getRule(String ruleId) {
        return rules.get(ruleId);
    }

    /** Grouped view of rules with counters for administration. */
    public Map<String, List<Map<String, Object>>> getCategoryOverview() {
        Map<String, List<Map<String, Object>>> overview = new LinkedHashMap<>();
        for (ValidationRule rule : getRules()) {
            String category = rule.getCategory().name();
            List<Map<String, Object>> categoryRules = overview.computeIfAbsent(category, k -> new ArrayList<>());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ruleId", rule.getRuleId());
            item.put("description", rule.getDescription());
            item.put("mode", getEffectiveMode(rule.getRuleId()).name());
            item.put("configuredMode", rule.getMode().name());
            item.put("enabled", rule.isEnabled());
            item.put("securityFixed", rule.isSecurityFixed());
            item.put("severity", rule.getSeverity());
            item.put("lastResult", rule.getLastResult());
            item.put("lastCheckAt", rule.getLastCheckAt());
            int[] counters = readCounters(rule.getRuleId());
            item.put("warnings", counters[0]);
            item.put("errors", counters[1]);
            categoryRules.add(item);
        }
        return overview;
    }

    private int[] readCounters(String ruleId) {
        int[] counters = {0, 0};
        String sql = "SELECT warnings, errors FROM validation_policy_config WHERE rule_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ruleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    counters[0] = rs.getInt("warnings");
                    counters[1] = rs.getInt("errors");
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return counters;
    }

    public List<Map<String, Object>> getHistory(int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT * FROM validation_policy_history ORDER BY id DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Math.max(1, Math.min(limit, 500)));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("timestamp", rs.getString("timestamp"));
                    item.put("ruleId", rs.getString("rule_id"));
                    item.put("category", rs.getString("category"));
                    item.put("oldMode", rs.getString("old_mode"));
                    item.put("newMode", rs.getString("new_mode"));
                    item.put("changedBy", rs.getString("changed_by"));
                    item.put("reason", rs.getString("change_reason"));
                    history.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to read validation history: " + e.getMessage());
        }
        return history;
    }
}
