package eu.kalafatic.evolution.servers.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;

import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.ApplicationAccess;
import eu.kalafatic.evolution.servers.model.ApplicationIdentity;
import eu.kalafatic.evolution.servers.model.User;
import eu.kalafatic.evolution.servers.repository.ApplicationAccessRepository;
import eu.kalafatic.evolution.servers.repository.ApplicationRepository;
import eu.kalafatic.evolution.servers.repository.UserRepository;
import eu.kalafatic.evolution.servers.security.TokenHasher;

/**
 * Application identity and token management for external MCP consumers.
 * Tokens are stored only as SHA-256 hashes; the plaintext is returned exactly
 * once at issuance. Registration of an application never grants automatic
 * access to user data - every token is bound to a user and a scope list.
 */
public class IdentityService {
    private static final Logger LOGGER = Logger.getLogger(IdentityService.class.getName());

    private static IdentityService instance;

    private final DatabaseManager dbManager;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAccessRepository accessRepository;
    private final UserRepository userRepository;

    public static synchronized IdentityService getInstance() {
        if (instance == null) {
            instance = new IdentityService(new DatabaseManager());
        }
        return instance;
    }

    public static synchronized IdentityService getInstance(DatabaseManager dbManager) {
        if (instance == null) {
            instance = new IdentityService(dbManager);
        }
        return instance;
    }

    public IdentityService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.applicationRepository = new ApplicationRepository(dbManager);
        this.accessRepository = new ApplicationAccessRepository(dbManager);
        this.userRepository = new UserRepository(dbManager);
    }

    public DatabaseManager getDbManager() { return dbManager; }
    public ApplicationRepository getApplicationRepository() { return applicationRepository; }
    public ApplicationAccessRepository getAccessRepository() { return accessRepository; }

    // ------------------------------------------------------------------
    // Applications
    // ------------------------------------------------------------------

    public ApplicationIdentity registerApplication(String applicationId, String name, String kind,
            Long ownerUserId, String description) throws IllegalArgumentException {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            throw new IllegalArgumentException("applicationId is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        try {
            Optional<ApplicationIdentity> existing = applicationRepository.findByApplicationId(applicationId.trim());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Application already registered: " + applicationId);
            }
            ApplicationIdentity app = new ApplicationIdentity(null, applicationId.trim(), name.trim(),
                    kind != null ? kind.trim() : "EXTERNAL_MCP", ownerUserId, true,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), null, description);
            applicationRepository.save(app);
            audit(null, "APPLICATION_REGISTERED", app.getApplicationId(), "SUCCESS", "Registered application " + app.getApplicationId());
            return app;
        } catch (SQLException e) {
            LOGGER.severe("Failed to register application: " + e.getMessage());
            throw new IllegalArgumentException("Failed to register application", e);
        }
    }

    public List<ApplicationIdentity> listApplications() {
        try {
            return applicationRepository.findAll();
        } catch (SQLException e) {
            LOGGER.severe("Failed to list applications: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<ApplicationIdentity> findApplication(String applicationId) {
        try {
            return applicationRepository.findByApplicationId(applicationId);
        } catch (SQLException e) {
            LOGGER.severe("Failed to find application: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void updateApplication(Long id, String name, String kind, Long ownerUserId,
            boolean enabled, String description) throws IllegalArgumentException {
        try {
            Optional<ApplicationIdentity> existing = applicationRepository.findById(id);
            if (existing.isEmpty()) {
                throw new IllegalArgumentException("Application not found: " + id);
            }
            ApplicationIdentity app = existing.get();
            app.setName(name != null ? name : app.getName());
            if (kind != null) app.setKind(kind);
            app.setOwnerUserId(ownerUserId != null ? ownerUserId : app.getOwnerUserId());
            app.setEnabled(enabled);
            if (description != null) app.setDescription(description);
            applicationRepository.update(app);
            audit(null, "APPLICATION_UPDATED", app.getApplicationId(), "SUCCESS", "Updated application " + app.getApplicationId());
        } catch (SQLException e) {
            LOGGER.severe("Failed to update application: " + e.getMessage());
            throw new IllegalArgumentException("Failed to update application", e);
        }
    }

    public void deleteApplication(Long id) throws IllegalArgumentException {
        try {
            Optional<ApplicationIdentity> existing = applicationRepository.findById(id);
            if (existing.isEmpty()) {
                throw new IllegalArgumentException("Application not found: " + id);
            }
            applicationRepository.delete(id);
            audit(null, "APPLICATION_DELETED", existing.get().getApplicationId(), "SUCCESS", "Deleted application " + existing.get().getApplicationId());
        } catch (SQLException e) {
            LOGGER.severe("Failed to delete application: " + e.getMessage());
            throw new IllegalArgumentException("Failed to delete application", e);
        }
    }

    // ------------------------------------------------------------------
    // Tokens
    // ------------------------------------------------------------------

    /**
     * Issues a new token for (application, user). Returns the plaintext token
     * exactly once; only its hash is persisted.
     */
    public String issueToken(String applicationId, Long userId, List<String> scopes,
            String expiresAt, String description) throws IllegalArgumentException {
        try {
            Optional<ApplicationIdentity> app = applicationRepository.findByApplicationId(applicationId);
            if (app.isEmpty()) {
                throw new IllegalArgumentException("Application not found: " + applicationId);
            }
            if (!app.get().isEnabled()) {
                throw new IllegalArgumentException("Application is disabled: " + applicationId);
            }
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty() || !user.get().isEnabled()) {
                throw new IllegalArgumentException("User not found or disabled: " + userId);
            }
            List<String> sanitized = CognitiveScopes.sanitizeScopes(scopes);
            if (sanitized.isEmpty()) {
                throw new IllegalArgumentException("At least one valid scope is required");
            }
            String plaintext = TokenHasher.generateToken();
            JSONArray arr = new JSONArray();
            for (String s : sanitized) arr.put(s);
            ApplicationAccess access = new ApplicationAccess(null, app.get().getId(), userId,
                    TokenHasher.hash(plaintext), arr.toString(), false,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), expiresAt, null, description);
            accessRepository.save(access);
            audit(userId, "APPLICATION_TOKEN_ISSUED", app.get().getApplicationId(), "SUCCESS",
                    "Issued token for application " + app.get().getApplicationId() + " to user " + userId);
            return plaintext;
        } catch (SQLException e) {
            LOGGER.severe("Failed to issue token: " + e.getMessage());
            throw new IllegalArgumentException("Failed to issue token", e);
        }
    }

    /**
     * Validates a bearer token. Returns the authenticated principal or empty
     * when the token is unknown, revoked, expired, or its application/user is
     * disabled.
     */
    public Optional<ValidatedPrincipal> validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            Optional<ApplicationAccess> access = accessRepository.findByTokenHash(TokenHasher.hash(token.trim()));
            if (access.isEmpty() || access.get().isRevoked()) {
                return Optional.empty();
            }
            if (access.get().getExpiresAt() != null && !access.get().getExpiresAt().isEmpty()) {
                try {
                    LocalDateTime expires = LocalDateTime.parse(access.get().getExpiresAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    if (expires.isBefore(LocalDateTime.now())) {
                        return Optional.empty();
                    }
                } catch (Exception e) {
                    return Optional.empty();
                }
            }
            Optional<ApplicationIdentity> app = applicationRepository.findById(access.get().getApplicationId());
            if (app.isEmpty() || !app.get().isEnabled()) {
                return Optional.empty();
            }
            Optional<User> user = userRepository.findById(access.get().getUserId());
            if (user.isEmpty() || !user.get().isEnabled()) {
                return Optional.empty();
            }
            Set<String> scopes = new HashSet<>();
            JSONArray arr = new JSONArray(access.get().getScopesJson());
            for (int i = 0; i < arr.length(); i++) {
                scopes.add(arr.getString(i));
            }
            accessRepository.updateLastUsed(access.get().getId(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return Optional.of(new ValidatedPrincipal(user.get().getUsername(), app.get().getApplicationId(),
                    access.get().getId(), scopes));
        } catch (SQLException e) {
            LOGGER.severe("Failed to validate token: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<ApplicationAccess> listAccesses(String applicationId) {
        try {
            Optional<ApplicationIdentity> app = applicationRepository.findByApplicationId(applicationId);
            if (app.isEmpty()) return List.of();
            return accessRepository.findByApplicationId(app.get().getId());
        } catch (SQLException e) {
            LOGGER.severe("Failed to list accesses: " + e.getMessage());
            return List.of();
        }
    }

    public List<ApplicationAccess> listAllAccesses() {
        try {
            return accessRepository.findAll();
        } catch (SQLException e) {
            LOGGER.severe("Failed to list accesses: " + e.getMessage());
            return List.of();
        }
    }

    public void revokeToken(Long accessId, String actorUsername) {
        try {
            accessRepository.revoke(accessId);
            audit(null, "APPLICATION_TOKEN_REVOKED", String.valueOf(accessId), "SUCCESS",
                    "Revoked access " + accessId + (actorUsername != null ? " by " + actorUsername : ""));
        } catch (SQLException e) {
            LOGGER.severe("Failed to revoke token: " + e.getMessage());
        }
    }

    public void deleteToken(Long accessId) {
        try {
            accessRepository.delete(accessId);
        } catch (SQLException e) {
            LOGGER.severe("Failed to delete token: " + e.getMessage());
        }
    }

    private void audit(Long actorUserId, String action, String resourceKey, String outcome, String details) {
        try {
            AuthorizationService.getInstance(dbManager).logAudit(actorUserId, null, action, "APPLICATION", resourceKey, outcome, details);
        } catch (Exception e) {
            LOGGER.warning("Failed to write audit: " + e.getMessage());
        }
    }
}
