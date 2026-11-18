package eu.kalafatic.evolution.servers.mcp.server;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.User;
import eu.kalafatic.evolution.servers.repository.SessionRepository;
import eu.kalafatic.evolution.servers.repository.UserRepository;
import eu.kalafatic.evolution.servers.service.AuthService;
import eu.kalafatic.evolution.servers.service.CognitiveScopes;
import eu.kalafatic.evolution.servers.service.IdentityService;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;

/**
 * Bearer authenticator for the MCP server. Accepts two token classes:
 * <ul>
 *   <li>application tokens (IdentityService) - bind (application, user) to
 *       granted scopes;</li>
 *   <li>user session tokens (AuthService) - represent the logged-in user;
 *       scopes derived from the user role.</li>
 * </ul>
 * Client-supplied identity parameters are never trusted; only the token
 * establishes the principal.
 */
public class BearerTokenAuthenticator implements McpAuthenticator {
    private static final Logger LOGGER = Logger.getLogger(BearerTokenAuthenticator.class.getName());

    private final IdentityService identityService;
    private final AuthService authService;

    public BearerTokenAuthenticator() {
        DatabaseManager dbManager = new DatabaseManager();
        this.identityService = IdentityService.getInstance(dbManager);
        this.authService = new AuthService(new UserRepository(dbManager), new SessionRepository(dbManager));
    }

    @Override
    public Optional<ValidatedPrincipal> authenticate(String bearerToken) {
        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            return Optional.empty();
        }
        String token = bearerToken.trim();

        // 1. Application token
        try {
            Optional<ValidatedPrincipal> appPrincipal = identityService.validateToken(token);
            if (appPrincipal.isPresent()) {
                return appPrincipal;
            }
        } catch (Exception e) {
            LOGGER.warning("BearerTokenAuthenticator: application token validation failed: " + e.getMessage());
        }

        // 2. User session token
        try {
            Optional<User> user = authService.validateSession(token);
            if (user.isPresent()) {
                Set<String> scopes = scopesForRole(user.get().getRole());
                return Optional.of(new ValidatedPrincipal(user.get().getUsername(), "evo",
                        -1L, scopes));
            }
        } catch (Exception e) {
            LOGGER.warning("BearerTokenAuthenticator: session token validation failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Role-derived scopes for user session tokens. */
    public static Set<String> scopesForRole(String role) {
        Set<String> scopes = new HashSet<>();
        scopes.add(CognitiveScopes.CONTEXT_GET);
        scopes.add(CognitiveScopes.CONTEXT_SEARCH);
        scopes.add(CognitiveScopes.MEMORY_READ);
        scopes.add(CognitiveScopes.USER_READ);
        scopes.add(CognitiveScopes.USER_PREFERENCES_READ);
        scopes.add(CognitiveScopes.SESSION_READ);
        scopes.add(CognitiveScopes.DECISION_READ);
        scopes.add(CognitiveScopes.PROJECT_READ);
        if (role != null && "ADMIN".equalsIgnoreCase(role)) {
            scopes.add(CognitiveScopes.CONTEXT_RECORD);
            scopes.add(CognitiveScopes.MEMORY_WRITE);
            scopes.add(CognitiveScopes.USER_PREFERENCES_WRITE);
            scopes.add(CognitiveScopes.SESSION_CREATE);
            scopes.add(CognitiveScopes.SESSION_WRITE);
            scopes.add(CognitiveScopes.DECISION_WRITE);
            scopes.add(CognitiveScopes.PROJECT_WRITE);
            scopes.add(CognitiveScopes.SESSION_READ_ANY);
            scopes.add(CognitiveScopes.MEMORY_READ_ANY);
            scopes.add(CognitiveScopes.PROJECT_READ_ANY);
            scopes.add(CognitiveScopes.ECOSYSTEM_ADMIN);
        }
        return scopes;
    }
}
