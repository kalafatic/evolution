package eu.kalafatic.evolution.controller.cognitive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.model.ContextSource;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;

/**
 * Assembles the user context block: identity, profile, and effective
 * preferences filtered by scope. Explicit preferences are rendered as user
 * instructions; inferred preferences as observed behavior.
 */
public class UserContextService {
    private static final Logger LOGGER = Logger.getLogger(UserContextService.class.getName());

    private static UserContextService instance;

    private final DatabaseManager databaseManager;
    private final PersonalizationService personalizationService;

    private UserContextService() {
        this.databaseManager = new DatabaseManager();
        this.personalizationService = PersonalizationService.getInstance();
    }

    public static synchronized UserContextService getInstance() {
        if (instance == null) {
            instance = new UserContextService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    /** Loads the profile block for a username; empty object when absent. */
    public JSONObject getProfile(String username) {
        JSONObject profile = new JSONObject();
        if (username == null) {
            return profile;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT u.id, u.username, u.display_name, u.role, u.enabled, u.description, " +
                     "p.display_name AS profile_display, p.locale, p.timezone, p.hints_json " +
                     "FROM users u LEFT JOIN user_profiles p ON p.user_id = u.id WHERE u.username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return profile;
                }
                profile.put("userId", rs.getLong("id"));
                profile.put("username", rs.getString("username"));
                profile.put("displayName", rs.getString("display_name"));
                profile.put("role", rs.getString("role"));
                profile.put("enabled", rs.getBoolean("enabled"));
                profile.put("description", rs.getString("description"));
                profile.put("locale", rs.getString("locale"));
                profile.put("timezone", rs.getString("timezone"));
                profile.put("hints", rs.getString("hints_json"));
            }
        } catch (Exception e) {
            LOGGER.warning("UserContextService: getProfile failed: " + e.getMessage());
        }
        return profile;
    }

    /** Resolves the canonical username for a principal (token-backed). */
    public String resolveUsername(ValidatedPrincipal principal, String requestedUserId) {
        if (principal == null) {
            // Internal SYSTEM callers may specify an explicit target user.
            return requestedUserId;
        }
        if (principal.isEcosystemAdmin() && requestedUserId != null) {
            return requestedUserId;
        }
        // Identity params never override the authenticated principal.
        return principal.getUsername();
    }

    /**
     * Builds the user context block: identity + explicit and inferred
     * preferences separated so the renderer can never mix them.
     */
    public JSONObject buildUserContext(String username, String projectRef, List<ContextSource> sources) {
        JSONObject context = new JSONObject();
        JSONObject profile = getProfile(username);
        context.put("identity", profile);
        if (profile.has("userId")) {
            long userId = profile.getLong("userId");
            List<JSONObject> prefs = personalizationService.getEffectivePreferencesByUserId(userId, projectRef);
            JSONArray explicitArr = new JSONArray();
            JSONArray inferredArr = new JSONArray();
            for (JSONObject pref : prefs) {
                String source = pref.optString("source", "EXPLICIT");
                if ("INFERRED".equals(source)) {
                    inferredArr.put(pref);
                    if (sources != null) {
                        sources.add(personalizationService.toContextSource(pref, "inferred preference"));
                    }
                } else {
                    explicitArr.put(pref);
                    if (sources != null) {
                        sources.add(personalizationService.toContextSource(pref, "explicit preference"));
                    }
                }
            }
            context.put("preferences", explicitArr);
            context.put("inferredPreferences", inferredArr);
            List<JSONObject> conflicts = personalizationService.detectExplicitConflicts(userId, projectRef);
            if (!conflicts.isEmpty()) {
                JSONArray conflictArr = new JSONArray();
                for (JSONObject conflict : conflicts) {
                    conflictArr.put(conflict);
                }
                context.put("preferenceConflicts", conflictArr);
            }
        }
        return context;
    }
}
