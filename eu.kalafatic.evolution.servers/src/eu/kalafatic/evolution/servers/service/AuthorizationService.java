package eu.kalafatic.evolution.servers.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.*;
import eu.kalafatic.evolution.servers.repository.*;

public class AuthorizationService {
    private static final Logger LOGGER = Logger.getLogger(AuthorizationService.class.getName());

    private static AuthorizationService instance;

    private final DatabaseManager dbManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAccessRepository resourceAccessRepository;
    private final AuditLogRepository auditLogRepository;

    public static synchronized AuthorizationService getInstance() {
        if (instance == null) {
            instance = new AuthorizationService(new DatabaseManager());
        }
        return instance;
    }

    public static synchronized AuthorizationService getInstance(DatabaseManager dbManager) {
        if (instance == null) {
            instance = new AuthorizationService(dbManager);
        }
        return instance;
    }

    public AuthorizationService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.userRepository = new UserRepository(dbManager);
        this.roleRepository = new RoleRepository(dbManager);
        this.permissionRepository = new PermissionRepository(dbManager);
        this.resourceRepository = new ResourceRepository(dbManager);
        this.resourceAccessRepository = new ResourceAccessRepository(dbManager);
        this.auditLogRepository = new AuditLogRepository(dbManager);
    }

    public DatabaseManager getDbManager() { return dbManager; }
    public UserRepository getUserRepository() { return userRepository; }
    public RoleRepository getRoleRepository() { return roleRepository; }
    public PermissionRepository getPermissionRepository() { return permissionRepository; }
    public ResourceRepository getResourceRepository() { return resourceRepository; }
    public ResourceAccessRepository getResourceAccessRepository() { return resourceAccessRepository; }
    public AuditLogRepository getAuditLogRepository() { return auditLogRepository; }

    /**
     * Central Authorization Check.
     * Evaluates whether principal (username/system) can execute/read action on target resource.
     * Conflict resolution: Explicit DENY overrides ALLOW.
     * System / Admin principal is permitted by default for bootstrap resilience.
     */
    public boolean authorize(String username, String resourceType, String resourceKey, String action) {
        String actor = (username != null && !username.trim().isEmpty()) ? username : "SYSTEM";

        // 1. System / Anonymous / Bootstrap principal check
        if ("SYSTEM".equalsIgnoreCase(actor) || "bootstrap".equalsIgnoreCase(actor)) {
            logAudit(null, actor, "AUTHORIZE", resourceType, resourceKey, "ALLOWED", "System/Bootstrap principal bypass");
            return true;
        }

        try {
            Optional<User> userOpt = userRepository.findByUsername(actor);
            if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
                logAudit(null, actor, "AUTHORIZE", resourceType, resourceKey, "DENIED", "User disabled or not found");
                return false;
            }

            User user = userOpt.get();

            // 2. Admin role bypass
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                logAudit(user.getId(), actor, "AUTHORIZE", resourceType, resourceKey, "ALLOWED", "Admin user granted full access");
                return true;
            }

            // Resolve explicit resource item if registered
            Optional<ResourceItem> resOpt = resourceRepository.findByTypeAndKey(resourceType, resourceKey);

            if (resOpt.isPresent()) {
                ResourceItem resource = resOpt.get();
                if (!resource.isEnabled()) {
                    logAudit(user.getId(), actor, "AUTHORIZE", resourceType, resourceKey, "DENIED", "Resource is disabled");
                    return false;
                }

                // Check explicit resource access policies
                List<ResourceAccessPolicy> policies = resourceAccessRepository.findByResourceId(resource.getId());
                boolean hasExplicitDeny = false;
                boolean hasExplicitAllow = false;

                // Resolve user role IDs
                List<Long> userRoleIds = userRepository.getUserRoleIds(user.getId());

                for (ResourceAccessPolicy policy : policies) {
                    if (policy.getAction().equalsIgnoreCase(action) || policy.getAction().equalsIgnoreCase("*") || policy.getAction().equalsIgnoreCase("MANAGE")) {
                        boolean principalMatches = false;
                        if ("USER".equalsIgnoreCase(policy.getPrincipalType()) && Objects.equals(policy.getPrincipalId(), user.getId())) {
                            principalMatches = true;
                        } else if ("ROLE".equalsIgnoreCase(policy.getPrincipalType()) && userRoleIds.contains(policy.getPrincipalId())) {
                            principalMatches = true;
                        }

                        if (principalMatches) {
                            if ("DENY".equalsIgnoreCase(policy.getEffect())) {
                                hasExplicitDeny = true;
                            } else if ("ALLOW".equalsIgnoreCase(policy.getEffect())) {
                                hasExplicitAllow = true;
                            }
                        }
                    }
                }

                // Strict Conflict Resolution: Explicit DENY overrides ALLOW
                if (hasExplicitDeny) {
                    logAudit(user.getId(), actor, "AUTHORIZE", resourceType, resourceKey, "DENIED", "Explicit DENY policy matched");
                    return false;
                }

                if (hasExplicitAllow) {
                    logAudit(user.getId(), actor, "AUTHORIZE", resourceType, resourceKey, "ALLOWED", "Explicit ALLOW policy matched");
                    return true;
                }
            }

            // 3. Fallback to Role-Based Permissions
            List<Long> roleIds = userRepository.getUserRoleIds(user.getId());
            for (Long rId : roleIds) {
                List<Permission> perms = permissionRepository.findByRoleId(rId);
                for (Permission p : perms) {
                    if ((p.getResourceType().equalsIgnoreCase(resourceType) || p.getResourceType().equals("*"))
                            && (p.getAction().equalsIgnoreCase(action) || p.getAction().equals("*"))) {
                        logAudit(user.getId(), actor, "AUTHORIZE", resourceType, resourceKey, "ALLOWED", "Role permission matched: " + p.getPermissionKey());
                        return true;
                    }
                }
            }

            // Default safe behavior: if user is not ADMIN and no explicit ALLOW/Permission matched, allow non-restricted default operations or log DENIED
            logAudit(user.getId(), actor, "AUTHORIZE", resourceType, resourceKey, "DENIED", "No matching permission or ALLOW policy");
            return false;

        } catch (SQLException e) {
            LOGGER.severe("Authorization evaluation failed: " + e.getMessage());
            logAudit(null, actor, "AUTHORIZE", resourceType, resourceKey, "ERROR", "Exception during authorization: " + e.getMessage());
            return false;
        }
    }

    public void logAudit(Long actorUserId, String actorUsername, String action, String resourceType, String resourceId, String outcome, String details) {
        try {
            AuditLogEntry entry = new AuditLogEntry(null, LocalDateTime.now(), actorUserId, actorUsername, action, resourceType, resourceId, outcome, details);
            auditLogRepository.save(entry);
        } catch (SQLException e) {
            LOGGER.severe("Failed to save audit log: " + e.getMessage());
        }
    }

    /**
     * Registers or updates discovered resources in the administrative inventory.
     */
    public void registerResource(String resourceType, String resourceKey, String displayName, String description, boolean enabled, String metadata) {
        try {
            Optional<ResourceItem> existing = resourceRepository.findByTypeAndKey(resourceType, resourceKey);
            if (existing.isPresent()) {
                ResourceItem item = existing.get();
                item.setDisplayName(displayName);
                item.setDescription(description);
                item.setEnabled(enabled);
                item.setMetadata(metadata);
                resourceRepository.update(item);
            } else {
                ResourceItem item = new ResourceItem(null, resourceType, resourceKey, displayName, description, enabled, metadata);
                resourceRepository.save(item);
            }
        } catch (SQLException e) {
            LOGGER.warning("Failed to register resource: " + e.getMessage());
        }
    }
}
