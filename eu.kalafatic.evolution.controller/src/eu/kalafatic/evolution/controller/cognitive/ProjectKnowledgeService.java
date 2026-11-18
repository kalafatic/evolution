package eu.kalafatic.evolution.controller.cognitive;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.servers.database.DatabaseManager;

/**
 * Persistent project knowledge store: components, architectural decisions,
 * known issues, build/test history and repository snapshots. Producers are
 * the existing genome pipeline, RepoArchitectureScanner and Evaluator; this
 * service persists and queries their output. Historical information is
 * explicitly marked - the store never pretends to be the current source code.
 */
public class ProjectKnowledgeService {
    private static final Logger LOGGER = Logger.getLogger(ProjectKnowledgeService.class.getName());

    private static ProjectKnowledgeService instance;

    private final DatabaseManager databaseManager;

    private ProjectKnowledgeService() {
        this.databaseManager = new DatabaseManager();
    }

    public static synchronized ProjectKnowledgeService getInstance() {
        if (instance == null) {
            instance = new ProjectKnowledgeService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    /** Stable project key: canonical path hash, portable across apps. */
    public static String projectKey(String rootPath) {
        if (rootPath == null) {
            return null;
        }
        String canonical = new File(rootPath).getAbsoluteFile().getPath();
        return Integer.toHexString(canonical.hashCode());
    }

    public long getOrCreateProject(String projectKey, String projectName, String rootPath) {
        if (projectKey == null) {
            projectKey = projectKey(rootPath);
        }
        Long existing = findProjectId(projectKey);
        if (existing != null) {
            return existing;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO project_knowledge (project_key, project_name, root_path, last_scanned_at) VALUES (?,?,?,?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, projectKey);
            ps.setString(2, projectName);
            ps.setString(3, rootPath);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("ProjectKnowledgeService: getOrCreateProject failed: " + e.getMessage());
        }
        return -1;
    }

    private Long findProjectId(String projectKey) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM project_knowledge WHERE project_key = ?")) {
            ps.setString(1, projectKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Architectural decisions
    // ------------------------------------------------------------------

    public JSONObject recordDecision(String projectRef, String decisionId, String title, String context,
            String decision, String rationale, String status) {
        long projectId = getOrCreateProject(null, projectRef, projectRef);
        if (decisionId == null || decisionId.isEmpty()) {
            decisionId = "ADR-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        long now = System.currentTimeMillis();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO architectural_decisions (project_knowledge_id, decision_id, title, context, decision, rationale, status, created_at, decided_at) " +
                     "VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, projectId);
            ps.setString(2, decisionId);
            ps.setString(3, title);
            ps.setString(4, context);
            ps.setString(5, decision);
            ps.setString(6, rationale);
            ps.setString(7, status != null ? status : "ACCEPTED");
            ps.setLong(8, now);
            ps.setLong(9, now);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("ProjectKnowledgeService: recordDecision failed: " + e.getMessage());
            throw new IllegalStateException("Failed to record decision", e);
        }
        JSONObject result = new JSONObject();
        result.put("decisionId", decisionId);
        result.put("project", projectRef);
        result.put("status", status != null ? status : "ACCEPTED");
        return result;
    }

    /** ACCEPTED decisions plus their supersession chain context. */
    public List<JSONObject> searchDecisions(String projectRef, String query, String status, int limit) {
        List<JSONObject> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT d.id, d.decision_id, d.title, d.context, d.decision, d.rationale, d.status, d.superseded_by, d.created_at, d.decided_at " +
                "FROM architectural_decisions d JOIN project_knowledge p ON p.id = d.project_knowledge_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (projectRef != null) {
            sql.append(" AND p.root_path = ?");
            params.add(projectRef);
        }
        if (status != null) {
            sql.append(" AND d.status = ?");
            params.add(status);
        }
        if (query != null && !query.trim().isEmpty()) {
            // Intent-style relevance: any meaningful token may match any
            // searchable column (OR semantics across tokens and columns).
            StringBuilder clause = new StringBuilder(" AND (");
            List<String> tokens = new ArrayList<>();
            for (String token : query.trim().split("\\s+")) {
                if (token.length() > 2) {
                    tokens.add(token);
                }
            }
            if (tokens.isEmpty()) {
                tokens.add(query.trim());
            }
            for (int i = 0; i < tokens.size(); i++) {
                if (i > 0) {
                    clause.append(" OR ");
                }
                clause.append("(COALESCE(d.title,'') LIKE ? OR COALESCE(d.decision,'') LIKE ? OR COALESCE(d.context,'') LIKE ?)");
                String pattern = "%" + tokens.get(i) + "%";
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
            }
            clause.append(")");
            sql.append(clause);
        }
        sql.append(" ORDER BY d.decided_at DESC LIMIT ").append(limit > 0 ? limit : 50);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject decision = new JSONObject();
                    decision.put("decisionId", rs.getString("decision_id"));
                    decision.put("title", rs.getString("title"));
                    decision.put("context", rs.getString("context"));
                    decision.put("decision", rs.getString("decision"));
                    decision.put("rationale", rs.getString("rationale"));
                    decision.put("status", rs.getString("status"));
                    decision.put("supersededBy", rs.getString("superseded_by"));
                    decision.put("decidedAt", rs.getLong("decided_at"));
                    list.add(decision);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("ProjectKnowledgeService: searchDecisions failed: " + e.getMessage());
        }
        return list;
    }

    public JSONObject getDecision(String projectRef, String decisionId) {
        JSONObject result = new JSONObject();
        List<JSONObject> matches = searchDecisions(projectRef, null, null, 1000);
        for (JSONObject d : matches) {
            if (decisionId.equals(d.optString("decisionId"))) {
                result.put("decision", d);
                result.put("supersededBy", d.optString("supersededBy"));
                return result;
            }
        }
        return result;
    }

    public void supersedeDecision(String projectRef, String decisionId, String supersedingDecisionId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE architectural_decisions SET status='SUPERSEDED', superseded_by=? " +
                     "WHERE decision_id=? AND project_knowledge_id=(SELECT id FROM project_knowledge WHERE root_path=?)")) {
            ps.setString(1, supersedingDecisionId);
            ps.setString(2, decisionId);
            ps.setString(3, projectRef);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.warning("ProjectKnowledgeService: supersedeDecision failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Known issues
    // ------------------------------------------------------------------

    public void recordKnownIssue(String projectRef, String title, String description, String severity,
            String status, String workaround) {
        long projectId = getOrCreateProject(null, projectRef, projectRef);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO known_issues (project_knowledge_id, title, description, severity, status, workaround) VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, projectId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, severity != null ? severity : "MEDIUM");
            ps.setString(5, status != null ? status : "OPEN");
            ps.setString(6, workaround);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("ProjectKnowledgeService: recordKnownIssue failed: " + e.getMessage());
        }
    }

    public List<JSONObject> listKnownIssues(String projectRef, String status, int limit) {
        List<JSONObject> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT i.title, i.description, i.severity, i.status, i.workaround FROM known_issues i " +
                "JOIN project_knowledge p ON p.id = i.project_knowledge_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (projectRef != null) {
            sql.append(" AND p.root_path = ?");
            params.add(projectRef);
        }
        if (status != null) {
            sql.append(" AND i.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY i.rowid DESC LIMIT ").append(limit > 0 ? limit : 50);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject issue = new JSONObject();
                    issue.put("title", rs.getString("title"));
                    issue.put("description", rs.getString("description"));
                    issue.put("severity", rs.getString("severity"));
                    issue.put("status", rs.getString("status"));
                    issue.put("workaround", rs.getString("workaround"));
                    list.add(issue);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("ProjectKnowledgeService: listKnownIssues failed: " + e.getMessage());
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Components
    // ------------------------------------------------------------------

    public void recordComponent(String projectRef, String componentPath, String componentType, String summary,
            String tagsJson, String criticality) {
        long projectId = getOrCreateProject(null, projectRef, projectRef);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO project_components (project_knowledge_id, component_path, component_type, summary, tags_json, criticality) " +
                     "VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, projectId);
            ps.setString(2, componentPath);
            ps.setString(3, componentType);
            ps.setString(4, summary);
            ps.setString(5, tagsJson);
            ps.setString(6, criticality);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.warning("ProjectKnowledgeService: recordComponent failed: " + e.getMessage());
        }
    }

    public List<JSONObject> listComponents(String projectRef, int limit) {
        List<JSONObject> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c.component_path, c.component_type, c.summary, c.tags_json, c.criticality " +
                     "FROM project_components c JOIN project_knowledge p ON p.id = c.project_knowledge_id " +
                     "WHERE p.root_path = ? ORDER BY c.rowid ASC LIMIT ?")) {
            ps.setString(1, projectRef);
            ps.setInt(2, limit > 0 ? limit : 200);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject component = new JSONObject();
                    component.put("path", rs.getString("component_path"));
                    component.put("type", rs.getString("component_type"));
                    component.put("summary", rs.getString("summary"));
                    component.put("tags", rs.getString("tags_json"));
                    component.put("criticality", rs.getString("criticality"));
                    list.add(component);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("ProjectKnowledgeService: listComponents failed: " + e.getMessage());
        }
        return list;
    }

    public int countComponents(String projectRef) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM project_components c JOIN project_knowledge p ON p.id = c.project_knowledge_id " +
                     "WHERE p.root_path = ?")) {
            ps.setString(1, projectRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // Build and test results
    // ------------------------------------------------------------------

    public long recordBuild(String projectRef, String command, boolean success, long durationMs, String summary,
            String sessionId) {
        long projectId = getOrCreateProject(null, projectRef, projectRef);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO build_results (project_knowledge_id, command, success, duration_ms, summary, session_id, timestamp) " +
                     "VALUES (?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, projectId);
            ps.setString(2, command);
            ps.setBoolean(3, success);
            ps.setLong(4, durationMs);
            ps.setString(5, summary);
            ps.setString(6, sessionId);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("ProjectKnowledgeService: recordBuild failed: " + e.getMessage());
        }
        return -1;
    }

    public void recordTest(String projectRef, String suite, int run, int failures, int errors, int skipped,
            double passRate, String failingTestsJson, long buildResultId) {
        long projectId = getOrCreateProject(null, projectRef, projectRef);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO test_results (project_knowledge_id, suite, run, failures, errors, skipped, pass_rate, failing_tests_json, build_result_id, timestamp) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, projectId);
            ps.setString(2, suite);
            ps.setInt(3, run);
            ps.setInt(4, failures);
            ps.setInt(5, errors);
            ps.setInt(6, skipped);
            ps.setDouble(7, passRate);
            ps.setString(8, failingTestsJson);
            if (buildResultId > 0) {
                ps.setLong(9, buildResultId);
            } else {
                ps.setNull(9, java.sql.Types.BIGINT);
            }
            ps.setLong(10, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.warning("ProjectKnowledgeService: recordTest failed: " + e.getMessage());
        }
    }

    public JSONObject getLatestBuild(String projectRef) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.command, b.success, b.duration_ms, b.summary, b.session_id, b.timestamp " +
                     "FROM build_results b JOIN project_knowledge p ON p.id = b.project_knowledge_id " +
                     "WHERE p.root_path = ? ORDER BY b.timestamp DESC LIMIT 1")) {
            ps.setString(1, projectRef);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                JSONObject build = new JSONObject();
                build.put("command", rs.getString("command"));
                build.put("success", rs.getBoolean("success"));
                build.put("durationMs", rs.getLong("duration_ms"));
                build.put("summary", rs.getString("summary"));
                build.put("sessionId", rs.getString("session_id"));
                build.put("timestamp", rs.getLong("timestamp"));
                return build;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject getLatestTest(String projectRef) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT t.suite, t.run, t.failures, t.errors, t.skipped, t.pass_rate, t.failing_tests_json, t.timestamp " +
                     "FROM test_results t JOIN project_knowledge p ON p.id = t.project_knowledge_id " +
                     "WHERE p.root_path = ? ORDER BY t.timestamp DESC LIMIT 1")) {
            ps.setString(1, projectRef);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                JSONObject test = new JSONObject();
                test.put("suite", rs.getString("suite"));
                test.put("run", rs.getInt("run"));
                test.put("failures", rs.getInt("failures"));
                test.put("errors", rs.getInt("errors"));
                test.put("skipped", rs.getInt("skipped"));
                test.put("passRate", rs.getDouble("pass_rate"));
                test.put("failingTests", rs.getString("failing_tests_json"));
                test.put("timestamp", rs.getLong("timestamp"));
                return test;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // State package
    // ------------------------------------------------------------------

    /**
     * Current project state for context reconstruction. The timestamp fields
     * let consumers warn when knowledge may be outdated relative to the
     * working tree - historical information is never presented as current.
     */
    public JSONObject getState(String projectRef) {
        JSONObject state = new JSONObject();
        state.put("project", projectRef);
        state.put("projectKey", projectKey(projectRef));
        Long projectId = findProjectId(projectKey(projectRef));
        state.put("known", projectId != null);
        if (projectId == null) {
            state.put("warnings", new JSONArray().put("No stored project knowledge yet - state is empty"));
            return state;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT last_scanned_at FROM project_knowledge WHERE id = ?")) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    state.put("lastScannedAt", rs.getLong("last_scanned_at"));
                }
            }
        } catch (Exception ignored) {
        }
        JSONObject build = getLatestBuild(projectRef);
        if (build != null) {
            state.put("lastBuild", build);
        }
        JSONObject test = getLatestTest(projectRef);
        if (test != null) {
            state.put("lastTest", test);
        }
        state.put("openIssues", listKnownIssues(projectRef, "OPEN", 50).size());
        state.put("componentCount", countComponents(projectRef));
        state.put("activeDecisionCount", searchDecisions(projectRef, null, "ACCEPTED", 500).size());
        return state;
    }

    public void recordImplementation(String projectRef, String summary, String changeSummary, String commitRef,
            String sessionId) {
        long projectId = getOrCreateProject(null, projectRef, projectRef);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO implementation_history (project_knowledge_id, summary, change_summary, commit_ref, session_id, timestamp) " +
                     "VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, projectId);
            ps.setString(2, summary);
            ps.setString(3, changeSummary);
            ps.setString(4, commitRef);
            ps.setString(5, sessionId);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.warning("ProjectKnowledgeService: recordImplementation failed: " + e.getMessage());
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
