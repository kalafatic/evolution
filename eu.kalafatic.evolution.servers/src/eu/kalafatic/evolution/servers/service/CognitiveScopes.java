package eu.kalafatic.evolution.servers.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Scope identifiers for EVO Cognitive Services access grants.
 */
public final class CognitiveScopes {

    // Context
    public static final String CONTEXT_GET = "context.get";
    public static final String CONTEXT_SEARCH = "context.search";
    public static final String CONTEXT_RECORD = "context.record";

    // Memory
    public static final String MEMORY_READ = "memory.read";
    public static final String MEMORY_WRITE = "memory.write";

    // User
    public static final String USER_READ = "user.read";
    public static final String USER_PREFERENCES_READ = "user.preferences.read";
    public static final String USER_PREFERENCES_WRITE = "user.preferences.write";

    // Sessions
    public static final String SESSION_CREATE = "session.create";
    public static final String SESSION_READ = "session.read";
    public static final String SESSION_WRITE = "session.write";

    // Decisions
    public static final String DECISION_READ = "decision.read";
    public static final String DECISION_WRITE = "decision.write";

    // Project
    public static final String PROJECT_READ = "project.read";
    public static final String PROJECT_WRITE = "project.write";

    // Cross-owner (admin-level)
    public static final String IDENTITY_READ_ANY = "identity.read.any";
    public static final String SESSION_READ_ANY = "session.read.any";
    public static final String MEMORY_READ_ANY = "memory.read.any";
    public static final String PROJECT_READ_ANY = "project.read.any";

    // Ecosystem
    public static final String ECOSYSTEM_ADMIN = "ecosystem.admin";

    /** Default scopes granted when registering a standard family application. */
    public static final List<String> DEFAULT_FAMILY_SCOPES = Arrays.asList(
            CONTEXT_GET, CONTEXT_SEARCH, CONTEXT_RECORD,
            MEMORY_READ, MEMORY_WRITE,
            USER_READ, USER_PREFERENCES_READ,
            SESSION_CREATE, SESSION_READ, SESSION_WRITE,
            DECISION_READ, DECISION_WRITE,
            PROJECT_READ, PROJECT_WRITE);

    /** Scopes available for selection in administration. */
    public static final List<String> ALL_SCOPES = Arrays.asList(
            CONTEXT_GET, CONTEXT_SEARCH, CONTEXT_RECORD,
            MEMORY_READ, MEMORY_WRITE,
            USER_READ, USER_PREFERENCES_READ, USER_PREFERENCES_WRITE,
            SESSION_CREATE, SESSION_READ, SESSION_WRITE,
            DECISION_READ, DECISION_WRITE,
            PROJECT_READ, PROJECT_WRITE,
            IDENTITY_READ_ANY, SESSION_READ_ANY, MEMORY_READ_ANY, PROJECT_READ_ANY,
            ECOSYSTEM_ADMIN);

    private CognitiveScopes() {}

    public static boolean isValidScope(String scope) {
        return ALL_SCOPES.contains(scope);
    }

    public static List<String> sanitizeScopes(List<String> requested) {
        List<String> result = new ArrayList<>();
        if (requested == null) return result;
        for (String s : requested) {
            if (s != null && isValidScope(s.trim()) && !result.contains(s.trim())) {
                result.add(s.trim());
            }
        }
        return result;
    }

    public static Optional<String> validate(Set<String> scopes, String required) {
        if (scopes == null) return Optional.empty();
        return scopes.contains(required) || scopes.contains(ECOSYSTEM_ADMIN)
                ? Optional.of(required) : Optional.empty();
    }
}
