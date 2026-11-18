package eu.kalafatic.evolution.view.editors.pages;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.CognitiveContextService;
import eu.kalafatic.evolution.controller.cognitive.SessionContinuityService;
import eu.kalafatic.evolution.controller.cognitive.model.ContextPackage;
import eu.kalafatic.evolution.controller.cognitive.model.ContextRequest;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.ApplicationAccess;
import eu.kalafatic.evolution.servers.model.ApplicationIdentity;
import eu.kalafatic.evolution.servers.model.User;
import eu.kalafatic.evolution.servers.service.IdentityService;
import eu.kalafatic.evolution.servers.service.UserService;
import eu.kalafatic.evolution.servers.validation.ValidationPolicyService;
import eu.kalafatic.evolution.servers.validation.ValidationResult;
import eu.kalafatic.evolution.servers.validation.ValidationRule;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Cognitive Services Administration page: overview, applications &amp; access,
 * memory administration, sessions, context inspector, validation
 * administration and audit. All operations go through the native services
 * (no database logic in the UI); the HTML view renders results returned by
 * the Java bridge. Runs on the SWT UI thread through BrowserFunction.
 */
public class CognitiveAdminPage extends AEvoPage {

    private static final String ADMIN_USER = "admin";

    private Browser browser;
    private final DatabaseManager databaseManager = new DatabaseManager();
    private final IdentityService identityService = IdentityService.getInstance(databaseManager);
    private final UserService userService = new UserService(
            new eu.kalafatic.evolution.servers.repository.UserRepository(databaseManager));
    private final ValidationPolicyService validationPolicyService = ValidationPolicyService.getInstance(databaseManager);

    public CognitiveAdminPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        setLayout(new FillLayout());
        createControl();
    }

    private void createControl() {
        Composite content = toolkit.createComposite(this);
        content.setLayout(new GridLayout(1, false));
        this.setContent(content);

        this.browser = new Browser(content, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        installBridge();
        initBrowser();
    }

    private void initBrowser() {
        try {
            org.osgi.framework.Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.controller");
            if (bundle != null) {
                java.net.URL entry = bundle.getEntry("eu/kalafatic/evolution/controller/orchestration/cognitive.html");
                if (entry != null) {
                    browser.setUrl(FileLocator.toFileURL(entry).toString());
                } else {
                    browser.setText("<html><body><h1>cognitive.html not found in bundle</h1></body></html>");
                }
            } else {
                browser.setText("<html><body><h1>Controller bundle not found</h1></body></html>");
            }
        } catch (Exception e) {
            browser.setText("<html><body><h1>Error loading cognitive.html: " + e.getMessage() + "</h1></body></html>");
        }
    }

    private void installBridge() {
        if (browser == null || browser.isDisposed()) {
            return;
        }
        new BrowserFunction(browser, "cognitiveAction") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments == null || arguments.length < 1) {
                    return err("missing action").toString();
                }
                String action = String.valueOf(arguments[0]);
                JSONObject payload = new JSONObject();
                if (arguments.length > 1 && arguments[1] != null) {
                    try {
                        payload = new JSONObject(String.valueOf(arguments[1]));
                    } catch (Exception ignored) {
                    }
                }
                try {
                    return dispatch(action, payload).toString();
                } catch (Exception e) {
                    return err(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()).toString();
                }
            }
        };
    }

    private JSONObject err(String message) {
        return new JSONObject().put("ok", false).put("error", message);
    }

    private JSONObject ok() {
        return new JSONObject().put("ok", true);
    }

    // ------------------------------------------------------------------
    // Action dispatch (all through native services)
    // ------------------------------------------------------------------

    private JSONObject dispatch(String action, JSONObject payload) {
        switch (action) {
            case "overview":
                return overview();
            case "applications":
                return applications();
            case "registerApplication":
                return registerApplication(payload);
            case "issueToken":
                return issueToken(payload);
            case "accesses":
                return accesses();
            case "revokeToken":
                return revokeToken(payload);
            case "memorySearch":
                return memorySearch(payload);
            case "memoryRetract":
                return memoryRetract(payload);
            case "sessions":
                return sessions();
            case "contextGet":
                return contextGet(payload);
            case "validation":
                return validation();
            case "setGlobalStrict":
                return setGlobalStrict(payload);
            case "setRuleMode":
                return setRuleMode(payload);
            case "runChecks":
                return runChecks();
            case "restoreDefaults":
                return restoreDefaults();
            case "audit":
                return audit();
            default:
                return err("Unknown action: " + action);
        }
    }

    private JSONObject overview() {
        JSONObject result = ok();
        result.put("status", "OPERATIONAL");
        result.put("applications", identityService.listApplications().size());
        result.put("sessions", SessionContinuityService.getInstance().countSessions());
        result.put("memoryEntries", MemoryService.getInstance().getEntryCount());
        result.put("conversations", countConversations());
        result.put("warnings", validationWarnings());
        result.put("errors", validationErrors());
        result.put("recent", recentAuditSummary());
        return result;
    }

    private int countConversations() {
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM conversation_messages")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int validationWarnings() {
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(warnings),0) FROM validation_policy_config")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int validationErrors() {
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(errors),0) FROM validation_policy_config")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String recentAuditSummary() {
        StringBuilder sb = new StringBuilder();
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                     "SELECT timestamp, actor_username, action, resource_type, resource_id, outcome " +
                     "FROM audit_log ORDER BY id DESC LIMIT 8")) {
            while (rs.next()) {
                sb.append(rs.getString("timestamp")).append("  ")
                  .append(rs.getString("actor_username")).append("  ")
                  .append(rs.getString("action")).append("  ")
                  .append(rs.getString("resource_type")).append("/")
                  .append(rs.getString("resource_id")).append("  ")
                  .append(rs.getString("outcome")).append('\n');
            }
        } catch (Exception e) {
            return "audit log unavailable: " + e.getMessage();
        }
        return sb.length() > 0 ? sb.toString() : "No audit entries yet";
    }

    private JSONObject applications() {
        JSONObject result = ok();
        JSONArray array = new JSONArray();
        for (ApplicationIdentity app : identityService.listApplications()) {
            JSONObject node = new JSONObject();
            node.put("id", app.getId());
            node.put("applicationId", app.getApplicationId());
            node.put("name", app.getName());
            node.put("kind", app.getKind());
            node.put("enabled", app.isEnabled());
            array.put(node);
        }
        result.put("applications", array);
        return result;
    }

    private JSONObject registerApplication(JSONObject payload) {
        try {
            String applicationId = payload.optString("applicationId", "").trim();
            String name = payload.optString("name", "").trim();
            String kind = payload.optString("kind", "EXTERNAL_MCP").trim();
            String owner = payload.optString("owner", "").trim();
            Long ownerUserId = null;
            if (!owner.isEmpty()) {
                ownerUserId = userService.getUserByUsername(owner).map(User::getId).orElse(null);
            }
            identityService.registerApplication(applicationId, name, kind, ownerUserId, null);
            return ok();
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    private JSONObject issueToken(JSONObject payload) {
        try {
            String applicationId = payload.optString("applicationId", "").trim();
            String username = payload.optString("user", ADMIN_USER).trim();
            Long userId = userService.getUserByUsername(username).map(User::getId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
            JSONArray scopesArr = payload.optJSONArray("scopes");
            List<String> scopes = new java.util.ArrayList<>();
            if (scopesArr != null) {
                for (int i = 0; i < scopesArr.length(); i++) {
                    scopes.add(scopesArr.getString(i).trim());
                }
            }
            String plaintext = identityService.issueToken(applicationId, userId, scopes, null, null);
            return ok().put("token", plaintext);
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    private JSONObject accesses() {
        JSONObject result = ok();
        JSONArray array = new JSONArray();
        for (ApplicationAccess access : identityService.listAllAccesses()) {
            String applicationId = "";
            String username = "";
            try {
                applicationId = identityService.getApplicationRepository().findById(access.getApplicationId())
                        .map(ApplicationIdentity::getApplicationId).orElse("");
            } catch (Exception ignored) {
            }
            try {
                username = userService.getUserById(access.getUserId()).map(User::getUsername).orElse("");
            } catch (Exception ignored) {
            }
            JSONObject node = new JSONObject();
            node.put("id", access.getId());
            node.put("applicationId", applicationId);
            node.put("username", username);
            node.put("scopes", access.getScopesJson());
            node.put("revoked", access.isRevoked());
            node.put("issuedAt", access.getIssuedAt());
            node.put("lastUsed", access.getLastUsedAt());
            array.put(node);
        }
        result.put("accesses", array);
        return result;
    }

    private JSONObject revokeToken(JSONObject payload) {
        try {
            long id = payload.optLong("id", -1);
            if (id < 0) {
                return err("missing access id");
            }
            identityService.revokeToken(id, ADMIN_USER);
            return ok();
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    private JSONObject memorySearch(JSONObject payload) {
        String query = payload.optString("query", "").trim();
        String scope = payload.has("scope") && !payload.isNull("scope") ? payload.optString("scope") : null;
        String type = payload.has("type") && !payload.isNull("type") ? payload.optString("type") : null;
        eu.kalafatic.evolution.controller.memory.MemoryScope scopeEnum = null;
        eu.kalafatic.evolution.controller.memory.MemoryType typeEnum = null;
        try {
            if (scope != null && !scope.isEmpty()) scopeEnum = eu.kalafatic.evolution.controller.memory.MemoryScope.valueOf(scope);
        } catch (Exception ignored) {
        }
        try {
            if (type != null && !type.isEmpty()) typeEnum = eu.kalafatic.evolution.controller.memory.MemoryType.valueOf(type);
        } catch (Exception ignored) {
        }
        List<MemoryEntry> entries = MemoryService.getInstance().search(query, scopeEnum, typeEnum, null, 100);
        JSONObject result = ok();
        JSONArray array = new JSONArray();
        for (MemoryEntry entry : entries) {
            JSONObject node = entry.toJsonObject();
            node.put("freshnessMs", entry.getFreshnessMs());
            array.put(node);
        }
        result.put("results", array);
        result.put("count", array.length());
        return result;
    }

    private JSONObject memoryRetract(JSONObject payload) {
        String id = payload.optString("id", "").trim();
        if (id.isEmpty()) {
            return err("missing memory entry id");
        }
        MemoryEntry entry = MemoryService.getInstance().retractEntry(id,
                payload.optString("reason", "Administrator retraction"), ADMIN_USER);
        if (entry == null) {
            return err("Memory entry not found: " + id);
        }
        return ok().put("validity", entry.getValidity().name());
    }

    private JSONObject sessions() {
        JSONObject result = ok();
        JSONArray array = new JSONArray();
        for (eu.kalafatic.evolution.controller.cognitive.model.SessionDescriptor descriptor
                : SessionContinuityService.getInstance().listSessions(null, null, null, null, 100)) {
            array.put(descriptor.toJsonObject());
        }
        result.put("sessions", array);
        return result;
    }

    private JSONObject contextGet(JSONObject payload) {
        ContextRequest request = new ContextRequest();
        request.setUserId(payload.optString("userId", ADMIN_USER));
        request.setApplicationId(payload.optString("applicationId", "evo"));
        request.setProject(payload.has("project") && !payload.isNull("project") ? payload.optString("project") : null);
        request.setSessionId(payload.has("sessionId") && !payload.isNull("sessionId") ? payload.optString("sessionId") : null);
        request.setIntent(payload.optString("intent", ""));
        request.setBudgetTokens(payload.optInt("budgetTokens", 12000));
        JSONArray categories = payload.optJSONArray("categories");
        if (categories != null) {
            for (int i = 0; i < categories.length(); i++) {
                request.getCategories().add(categories.getString(i));
            }
        }
        ContextPackage pkg = CognitiveContextService.getInstance().getContext(request);
        JSONObject result = ok();
        result.put("package", pkg.toJsonObject());
        return result;
    }

    private JSONObject validation() {
        JSONObject result = ok();
        result.put("globalStrict", validationPolicyService.isGlobalStrict());
        JSONArray categoryArray = new JSONArray();
        for (Map.Entry<String, List<Map<String, Object>>> entry
                : validationPolicyService.getCategoryOverview().entrySet()) {
            JSONObject category = new JSONObject();
            category.put("category", entry.getKey());
            JSONArray rulesArray = new JSONArray();
            for (Map<String, Object> ruleMap : entry.getValue()) {
                JSONObject rule = new JSONObject();
                for (Map.Entry<String, Object> field : ruleMap.entrySet()) {
                    rule.put(field.getKey(), field.getValue());
                }
                rulesArray.put(rule);
            }
            category.put("rules", rulesArray);
            categoryArray.put(category);
        }
        result.put("categories", categoryArray);
        JSONArray historyArray = new JSONArray();
        for (Map<String, Object> entry : validationPolicyService.getHistory(100)) {
            JSONObject node = new JSONObject();
            for (Map.Entry<String, Object> field : entry.entrySet()) {
                node.put(field.getKey(), field.getValue());
            }
            historyArray.put(node);
        }
        result.put("history", historyArray);
        return result;
    }

    private JSONObject setGlobalStrict(JSONObject payload) {
        try {
            validationPolicyService.setGlobalStrict(payload.optBoolean("strict", false), ADMIN_USER);
            return ok();
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    private JSONObject setRuleMode(JSONObject payload) {
        try {
            String ruleId = payload.optString("ruleId", "").trim();
            String mode = payload.optString("mode", "WARN").trim();
            if (ruleId.isEmpty()) {
                return err("missing ruleId");
            }
            ValidationRule rule = validationPolicyService.getRule(ruleId);
            if (rule != null && rule.isSecurityFixed()) {
                return err("Security rule '" + ruleId + "' can never be disabled");
            }
            validationPolicyService.setRuleMode(ruleId,
                    eu.kalafatic.evolution.servers.validation.ValidationMode.fromString(mode),
                    ADMIN_USER, "Changed via Validation Administration UI");
            return ok();
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    private JSONObject runChecks() {
        try {
            Map<String, ValidationResult> results = validationPolicyService.runAllChecks();
            int evaluated = 0;
            int failed = 0;
            for (ValidationResult result : results.values()) {
                evaluated++;
                if (!result.isPassed()) {
                    failed++;
                }
            }
            return ok().put("executed", evaluated).put("failed", failed);
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    private JSONObject restoreDefaults() {
        try {
            validationPolicyService.resetDefaults(ADMIN_USER);
            return ok();
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    private JSONObject audit() {
        JSONObject result = ok();
        JSONArray array = new JSONArray();
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                     "SELECT timestamp, actor_username, action, resource_type, resource_id, outcome, details " +
                     "FROM audit_log ORDER BY id DESC LIMIT 100")) {
            while (rs.next()) {
                JSONObject node = new JSONObject();
                node.put("timestamp", rs.getString("timestamp"));
                node.put("actor", rs.getString("actor_username"));
                node.put("action", rs.getString("action"));
                node.put("resourceType", rs.getString("resource_type"));
                node.put("resourceId", rs.getString("resource_id"));
                node.put("outcome", rs.getString("outcome"));
                node.put("details", rs.getString("details"));
                array.put(node);
            }
        } catch (Exception e) {
            return err(e.getMessage());
        }
        result.put("entries", array);
        return result;
    }

    @Override
    protected void refreshUI() {
        // The HTML view drives its own refresh through the bridge.
    }

    @Override
    public void dispose() {
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
        super.dispose();
    }
}
