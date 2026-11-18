package eu.kalafatic.evolution.controller.cognitive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.model.ContextSource;
import eu.kalafatic.evolution.servers.database.DatabaseManager;

/**
 * Typed user preferences with EXPLICIT/INFERRED/TEMPORARY sources and
 * deterministic precedence: explicit always beats inferred; project scope
 * beats user scope for project context; temporary overrides both for its
 * lifetime. Contradictions are surfaced, never silently resolved.
 */
public class PersonalizationService {
    private static final Logger LOGGER = Logger.getLogger(PersonalizationService.class.getName());

    private static PersonalizationService instance;

    private final DatabaseManager databaseManager;

    private PersonalizationService() {
        this.databaseManager = new DatabaseManager();
    }

    public static synchronized PersonalizationService getInstance() {
        if (instance == null) {
            instance = new PersonalizationService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /** Effective preferences for a user in a scope context, applying precedence. */
    public List<JSONObject> getEffectivePreferences(String username, String projectRef) {
        List<JSONObject> all = loadByUsername(username);
        List<JSONObject> effective = new ArrayList<>();
        for (JSONObject pref : all) {
            String scope = pref.optString("scope", "USER");
            String prefProject = pref.optString("projectRef", null);
            boolean applicable = "USER".equals(scope)
                    || ("PROJECT".equals(scope) && projectRef != null && projectRef.equals(prefProject))
                    || ("APPLICATION".equals(scope));
            if (!applicable) {
                continue;
            }
            effective.add(pref);
        }
        return applyPrecedence(effective);
    }

    public List<JSONObject> getEffectivePreferencesByUserId(long userId, String projectRef) {
        String username = resolveUsername(userId);
        return username != null ? getEffectivePreferences(username, projectRef) : List.of();
    }

    /** Effective value of a single key; explicit wins. */
    public JSONObject getEffectivePreference(String username, String projectRef, String key) {
        List<JSONObject> effective = getEffectivePreferences(username, projectRef);
        JSONObject best = null;
        for (JSONObject pref : effective) {
            if (!key.equals(pref.optString("key"))) {
                continue;
            }
            if (best == null || beats(pref, best, projectRef)) {
                best = pref;
            }
        }
        return best;
    }

    /** True when candidate has higher precedence than current. */
    private boolean beats(JSONObject candidate, JSONObject current, String projectRef) {
        String cSource = candidate.optString("source", "EXPLICIT");
        String curSource = current.optString("source", "EXPLICIT");
        // TEMPORARY > EXPLICIT (session-scoped override, rendered with expiry)
        if ("TEMPORARY".equals(cSource) && !"TEMPORARY".equals(curSource)) return true;
        if ("TEMPORARY".equals(curSource) && !"TEMPORARY".equals(cSource)) return false;
        // EXPLICIT > INFERRED
        if ("EXPLICIT".equals(cSource) && "INFERRED".equals(curSource)) return true;
        if ("EXPLICIT".equals(curSource) && "INFERRED".equals(cSource)) return false;
        // PROJECT > USER for project context
        String cScope = candidate.optString("scope", "USER");
        String curScope = current.optString("scope", "USER");
        if ("PROJECT".equals(cScope) && !"PROJECT".equals(curScope)) return true;
        if ("PROJECT".equals(curScope) && !"PROJECT".equals(cScope)) return false;
        // newer wins on equal precedence
        return candidate.optString("updatedAt", "").compareTo(current.optString("updatedAt", "")) > 0;
    }

    private List<JSONObject> applyPrecedence(List<JSONObject> prefs) {
        List<JSONObject> result = new ArrayList<>();
        for (JSONObject pref : prefs) {
            String source = pref.optString("source", "EXPLICIT");
            if ("TEMPORARY".equals(source) && isExpired(pref)) {
                continue;
            }
            result.add(pref);
        }
        return result;
    }

    private boolean isExpired(JSONObject pref) {
        String expires = pref.optString("expiresAt", null);
        if (expires == null || expires.isEmpty()) {
            return false;
        }
        try {
            return LocalDateTime.parse(expires, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .isBefore(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Writes (precedence-enforced)
    // ------------------------------------------------------------------

    /**
     * Explicit writes always succeed. Inferred writes are rejected when an
     * explicit value already exists for the same key (overwriteExplicit=false).
     * Returns acceptance info plus any detected conflicts.
     */
    public JSONObject updateExplicit(long userId, String scope, String projectRef, String key, String valueJson,
            double confidence) {
        JSONObject pref = new JSONObject();
        pref.put("userId", userId);
        pref.put("scope", scope != null ? scope : "USER");
        pref.put("projectRef", projectRef);
        pref.put("key", key);
        pref.put("value", valueJson);
        pref.put("source", "EXPLICIT");
        pref.put("confidence", confidence);
        pref.put("status", "ACTIVE");
        pref.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        persist(pref);
        JSONObject result = new JSONObject();
        result.put("accepted", true);
        result.put("preference", pref);
        return result;
    }

    public JSONObject recordInference(long userId, String scope, String projectRef, String key, String valueJson,
            double confidence, boolean overwriteExplicit, String evidenceRef) {
        JSONObject existing = findActive(userId, scope, projectRef, key);
        JSONObject result = new JSONObject();
        if (existing != null && "EXPLICIT".equals(existing.optString("source", "EXPLICIT")) && !overwriteExplicit) {
            result.put("accepted", false);
            result.put("reason", "Explicit preference exists; inferred write rejected");
            result.put("existing", existing);
            return result;
        }
        JSONObject pref = new JSONObject();
        pref.put("userId", userId);
        pref.put("scope", scope != null ? scope : "USER");
        pref.put("projectRef", projectRef);
        pref.put("key", key);
        pref.put("value", valueJson);
        pref.put("source", "INFERRED");
        pref.put("confidence", confidence);
        pref.put("status", "ACTIVE");
        pref.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (evidenceRef != null) {
            pref.put("evidenceRefs", new org.json.JSONArray().put(evidenceRef));
        }
        persist(pref);
        result.put("accepted", true);
        result.put("preference", pref);
        return result;
    }

    public JSONObject recordTemporary(long userId, String scope, String projectRef, String key, String valueJson,
            long ttlMinutes) {
        JSONObject pref = new JSONObject();
        pref.put("userId", userId);
        pref.put("scope", scope != null ? scope : "USER");
        pref.put("projectRef", projectRef);
        pref.put("key", key);
        pref.put("value", valueJson);
        pref.put("source", "TEMPORARY");
        pref.put("confidence", 1.0);
        pref.put("status", "ACTIVE");
        pref.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        pref.put("expiresAt", LocalDateTime.now().plusMinutes(ttlMinutes > 0 ? ttlMinutes : 60)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        persist(pref);
        JSONObject result = new JSONObject();
        result.put("accepted", true);
        result.put("preference", pref);
        return result;
    }

    /**
     * User correction of an inferred preference: source downgraded, confidence
     * reset, evidence updated. Repeated contradiction is handled by callers
     * via supersedePreference.
     */
    public JSONObject handleCorrection(long userId, String scope, String projectRef, String key, String correctedValueJson) {
        JSONObject existing = findActive(userId, scope, projectRef, key);
        if (existing != null && "INFERRED".equals(existing.optString("source", "EXPLICIT"))) {
            existing.put("source", "EXPLICIT");
            existing.put("value", correctedValueJson);
            existing.put("confidence", 0.3);
            existing.put("corrected", true);
            existing.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            persist(existing);
        } else {
            updateExplicit(userId, scope, projectRef, key, correctedValueJson, 1.0);
        }
        JSONObject result = new JSONObject();
        result.put("accepted", true);
        result.put("key", key);
        return result;
    }

    /** Detects two active EXPLICIT entries for the same key and marks both. */
    public List<JSONObject> detectExplicitConflicts(long userId, String projectRef) {
        List<JSONObject> all = loadByUserId(userId);
        List<JSONObject> conflicts = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            JSONObject a = all.get(i);
            if (!"EXPLICIT".equals(a.optString("source"))) {
                continue;
            }
            for (int j = i + 1; j < all.size(); j++) {
                JSONObject b = all.get(j);
                if (!"EXPLICIT".equals(b.optString("source"))) {
                    continue;
                }
                if (a.optString("key").equals(b.optString("key"))
                        && a.optString("scope").equals(b.optString("scope"))
                        && !a.optString("value").equals(b.optString("value"))) {
                    JSONObject conflict = new JSONObject();
                    conflict.put("key", a.optString("key"));
                    conflict.put("first", a);
                    conflict.put("second", b);
                    conflicts.add(conflict);
                }
            }
        }
        return conflicts;
    }

    private JSONObject findActive(long userId, String scope, String projectRef, String key) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, user_id, scope, project_ref, key, value_json, source, confidence, status, expires_at, updated_at " +
                     "FROM user_preferences WHERE user_id = ? AND scope = ? AND key = ? AND status = 'ACTIVE' " +
                     "AND (project_ref = ? OR project_ref IS NULL) ORDER BY updated_at DESC LIMIT 1")) {
            ps.setLong(1, userId);
            ps.setString(2, scope);
            ps.setString(3, key);
            ps.setString(4, projectRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (Exception e) {
            LOGGER.warning("PersonalizationService: findActive failed: " + e.getMessage());
            return null;
        }
    }

    private List<JSONObject> loadByUserId(long userId) {
        List<JSONObject> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, user_id, scope, project_ref, key, value_json, source, confidence, status, expires_at, updated_at " +
                     "FROM user_preferences WHERE user_id = ? AND status = 'ACTIVE' ORDER BY updated_at DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("PersonalizationService: loadByUserId failed: " + e.getMessage());
        }
        return list;
    }

    private List<JSONObject> loadByUsername(String username) {
        List<JSONObject> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT p.id, p.user_id, p.scope, p.project_ref, p.key, p.value_json, p.source, p.confidence, " +
                     "p.status, p.expires_at, p.updated_at FROM user_preferences p JOIN users u ON u.id = p.user_id " +
                     "WHERE u.username = ? AND p.status = 'ACTIVE' ORDER BY p.updated_at DESC")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("PersonalizationService: loadByUsername failed: " + e.getMessage());
        }
        return list;
    }

    private String resolveUsername(long userId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject mapRow(ResultSet rs) throws java.sql.SQLException {
        JSONObject pref = new JSONObject();
        pref.put("id", rs.getLong("id"));
        pref.put("userId", rs.getLong("user_id"));
        pref.put("scope", rs.getString("scope"));
        pref.put("projectRef", rs.getString("project_ref"));
        pref.put("key", rs.getString("key"));
        pref.put("value", rs.getString("value_json"));
        pref.put("source", rs.getString("source"));
        pref.put("confidence", rs.getDouble("confidence"));
        pref.put("status", rs.getString("status"));
        pref.put("expiresAt", rs.getString("expires_at"));
        pref.put("updatedAt", rs.getString("updated_at"));
        return pref;
    }

    private void persist(JSONObject pref) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO user_preferences (user_id, scope, project_ref, key, value_json, source, confidence, status, expires_at, updated_at) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, pref.optLong("userId"));
            ps.setString(2, pref.optString("scope", "USER"));
            ps.setString(3, pref.has("projectRef") ? pref.optString("projectRef") : null);
            ps.setString(4, pref.getString("key"));
            ps.setString(5, pref.optString("value"));
            ps.setString(6, pref.optString("source", "EXPLICIT"));
            ps.setDouble(7, pref.optDouble("confidence", 1.0));
            ps.setString(8, pref.optString("status", "ACTIVE"));
            ps.setString(9, pref.has("expiresAt") ? pref.optString("expiresAt") : null);
            ps.setString(10, pref.optString("updatedAt",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("PersonalizationService: persist failed: " + e.getMessage());
            throw new IllegalStateException("Failed to persist preference", e);
        }
    }

    /** Sources for context provenance rendering of a preference. */
    public ContextSource toContextSource(JSONObject pref, String contribution) {
        String sourceType = "EXPLICIT".equals(pref.optString("source")) ? "USER_PREFERENCE" : "INFERRED_PREFERENCE";
        return new ContextSource(sourceType, pref.optString("key"), contribution,
                pref.optDouble("confidence", 1.0), System.currentTimeMillis());
    }
}
