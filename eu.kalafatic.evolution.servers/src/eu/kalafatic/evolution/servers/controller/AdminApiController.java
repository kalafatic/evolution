package eu.kalafatic.evolution.servers.controller;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;

import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.*;
import eu.kalafatic.evolution.servers.repository.*;
import eu.kalafatic.evolution.servers.security.BCryptUtils;
import eu.kalafatic.evolution.servers.service.AuthorizationService;

public class AdminApiController {

    private final AuthorizationService authService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAccessRepository resourceAccessRepository;
    private final AuditLogRepository auditLogRepository;
    private final SessionRepository sessionRepository;

    public AdminApiController(DatabaseManager dbManager) {
        this.authService = AuthorizationService.getInstance(dbManager);
        this.userRepository = authService.getUserRepository();
        this.roleRepository = authService.getRoleRepository();
        this.permissionRepository = authService.getPermissionRepository();
        this.resourceRepository = authService.getResourceRepository();
        this.resourceAccessRepository = authService.getResourceAccessRepository();
        this.auditLogRepository = authService.getAuditLogRepository();
        this.sessionRepository = new SessionRepository(dbManager);
    }

    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            // Parse POST/PUT JSON body
            Map<String, String> files = new HashMap<>();
            if (Method.POST.equals(method) || Method.PUT.equals(method)) {
                session.parseBody(files);
            }
            String postData = files.get("postData");
            JSONObject bodyJson = (postData != null && !postData.trim().isEmpty()) ? new JSONObject(postData) : new JSONObject();

            // Extract principal header if provided, fallback to admin/system
            String actorUser = session.getHeaders().getOrDefault("x-evo-user", "admin");

            // Authorization check
            if (!authService.authorize(actorUser, "CONFIGURATION", "admin_api", "MANAGE")) {
                // If not admin, verify if user has users.manage or permissions.manage
                if (uri.startsWith("/api/admin/users") && !authService.authorize(actorUser, "USER", "users", "MANAGE")) {
                    return jsonResponse(Response.Status.FORBIDDEN, new JSONObject().put("error", "Access Denied"));
                }
            }

            // Route matching
            if (uri.equals("/api/admin/overview")) {
                return getOverview();
            } else if (uri.equals("/api/admin/users") && Method.GET.equals(method)) {
                return getUsers();
            } else if (uri.equals("/api/admin/users") && Method.POST.equals(method)) {
                return createUser(bodyJson, actorUser);
            } else if (uri.startsWith("/api/admin/users/") && Method.PUT.equals(method)) {
                Long userId = Long.parseLong(uri.substring("/api/admin/users/".length()));
                return updateUser(userId, bodyJson, actorUser);
            } else if (uri.startsWith("/api/admin/users/") && Method.DELETE.equals(method)) {
                Long userId = Long.parseLong(uri.substring("/api/admin/users/".length()));
                return deleteUser(userId, actorUser);
            } else if (uri.equals("/api/admin/sessions") && Method.GET.equals(method)) {
                return getSessions();
            } else if (uri.startsWith("/api/admin/sessions/terminate/") && Method.POST.equals(method)) {
                String targetSid = uri.substring("/api/admin/sessions/terminate/".length());
                return terminateSession(targetSid, actorUser);
            } else if (uri.equals("/api/admin/roles") && Method.GET.equals(method)) {
                return getRoles();
            } else if (uri.equals("/api/admin/permissions") && Method.GET.equals(method)) {
                return getPermissions();
            } else if (uri.equals("/api/admin/resources") && Method.GET.equals(method)) {
                return getResources();
            } else if (uri.equals("/api/admin/policies") && Method.GET.equals(method)) {
                return getPolicies();
            } else if (uri.equals("/api/admin/policies") && Method.POST.equals(method)) {
                return createPolicy(bodyJson, actorUser);
            } else if (uri.startsWith("/api/admin/policies/") && Method.DELETE.equals(method)) {
                Long policyId = Long.parseLong(uri.substring("/api/admin/policies/".length()));
                return deletePolicy(policyId, actorUser);
            } else if (uri.equals("/api/admin/audit-logs") && Method.GET.equals(method)) {
                return getAuditLogs();
            }

            return jsonResponse(Response.Status.NOT_FOUND, new JSONObject().put("error", "Admin Endpoint Not Found"));

        } catch (Exception e) {
            e.printStackTrace();
            return jsonResponse(Response.Status.INTERNAL_ERROR, new JSONObject().put("error", e.getMessage()));
        }
    }

    private Response getOverview() throws SQLException {
        List<User> users = userRepository.findAll();
        List<Session> sessions = sessionRepository.findAll();
        List<Role> roles = roleRepository.findAll();
        List<ResourceItem> resources = resourceRepository.findAll();

        long activeSessions = sessions.stream().filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus())).count();
        long enabledUsers = users.stream().filter(User::isEnabled).count();

        JSONObject overview = new JSONObject();
        overview.put("totalUsers", users.size());
        overview.put("enabledUsers", enabledUsers);
        overview.put("disabledUsers", users.size() - enabledUsers);
        overview.put("totalSessions", sessions.size());
        overview.put("activeSessions", activeSessions);
        overview.put("totalRoles", roles.size());
        overview.put("totalResources", resources.size());
        return jsonResponse(Response.Status.OK, overview);
    }

    private Response getUsers() throws SQLException {
        List<User> users = userRepository.findAll();
        JSONArray array = new JSONArray();
        for (User u : users) {
            JSONObject obj = new JSONObject();
            obj.put("id", u.getId());
            obj.put("username", u.getUsername());
            obj.put("displayName", u.getDisplayName() != null ? u.getDisplayName() : u.getUsername());
            obj.put("role", u.getRole());
            obj.put("enabled", u.isEnabled());
            obj.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            obj.put("updatedAt", u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "");
            obj.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : "");
            obj.put("description", u.getDescription() != null ? u.getDescription() : "");
            array.put(obj);
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("users", array));
    }

    private Response createUser(JSONObject json, String actor) throws SQLException {
        String username = json.optString("username");
        String password = json.optString("password", "password123");
        String displayName = json.optString("displayName", username);
        String role = json.optString("role", "USER");
        boolean enabled = json.optBoolean("enabled", true);
        String description = json.optString("description", "");

        if (username.isEmpty()) {
            return jsonResponse(Response.Status.BAD_REQUEST, new JSONObject().put("error", "Username required"));
        }

        User user = new User(null, username, displayName, BCryptUtils.hashPassword(password), role, enabled, null, null, null, description);
        userRepository.save(user);

        authService.logAudit(null, actor, "USER_CREATED", "USER", username, "SUCCESS", "Created user " + username + " with role " + role);
        return jsonResponse(Response.Status.OK, new JSONObject().put("message", "User created").put("id", user.getId()));
    }

    private Response updateUser(Long userId, JSONObject json, String actor) throws SQLException {
        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            return jsonResponse(Response.Status.NOT_FOUND, new JSONObject().put("error", "User not found"));
        }
        User user = uOpt.get();
        if (json.has("displayName")) user.setDisplayName(json.getString("displayName"));
        if (json.has("role")) user.setRole(json.getString("role"));
        if (json.has("enabled")) user.setEnabled(json.getBoolean("enabled"));
        if (json.has("description")) user.setDescription(json.getString("description"));
        if (json.has("password") && !json.getString("password").trim().isEmpty()) {
            user.setPasswordHash(BCryptUtils.hashPassword(json.getString("password")));
        }

        userRepository.update(user);
        authService.logAudit(null, actor, "USER_UPDATED", "USER", user.getUsername(), "SUCCESS", "Updated user " + user.getUsername());
        return jsonResponse(Response.Status.OK, new JSONObject().put("message", "User updated"));
    }

    private Response deleteUser(Long userId, String actor) throws SQLException {
        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isPresent()) {
            User user = uOpt.get();
            if ("admin".equalsIgnoreCase(user.getUsername())) {
                return jsonResponse(Response.Status.BAD_REQUEST, new JSONObject().put("error", "Cannot delete default admin user"));
            }
            userRepository.delete(userId);
            authService.logAudit(null, actor, "USER_DELETED", "USER", user.getUsername(), "SUCCESS", "Deleted user " + user.getUsername());
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("message", "User deleted"));
    }

    private Response getSessions() throws SQLException {
        List<Session> list = sessionRepository.findAll();
        JSONArray array = new JSONArray();
        for (Session s : list) {
            JSONObject obj = new JSONObject();
            obj.put("id", s.getId());
            obj.put("sessionId", s.getSessionId());
            obj.put("userId", s.getUserId());
            obj.put("sessionType", s.getSessionType() != null ? s.getSessionType() : "SYSTEM");
            obj.put("status", s.getStatus() != null ? s.getStatus() : "ACTIVE");
            obj.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            obj.put("lastAccess", s.getLastAccess() != null ? s.getLastAccess().toString() : "");
            obj.put("endedAt", s.getEndedAt() != null ? s.getEndedAt().toString() : "");
            obj.put("metadata", s.getMetadata() != null ? s.getMetadata() : "");
            array.put(obj);
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("sessions", array));
    }

    private Response terminateSession(String sid, String actor) throws SQLException {
        sessionRepository.updateStatus(sid, "TERMINATED");
        authService.logAudit(null, actor, "SESSION_TERMINATED", "SESSION", sid, "SUCCESS", "Terminated session " + sid);
        return jsonResponse(Response.Status.OK, new JSONObject().put("message", "Session terminated"));
    }

    private Response getRoles() throws SQLException {
        List<Role> roles = roleRepository.findAll();
        JSONArray array = new JSONArray();
        for (Role r : roles) {
            JSONObject obj = new JSONObject();
            obj.put("id", r.getId());
            obj.put("name", r.getName());
            obj.put("description", r.getDescription());
            obj.put("systemRole", r.isSystemRole());
            obj.put("enabled", r.isEnabled());
            array.put(obj);
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("roles", array));
    }

    private Response getPermissions() throws SQLException {
        List<Permission> perms = permissionRepository.findAll();
        JSONArray array = new JSONArray();
        for (Permission p : perms) {
            JSONObject obj = new JSONObject();
            obj.put("id", p.getId());
            obj.put("permissionKey", p.getPermissionKey());
            obj.put("name", p.getName());
            obj.put("description", p.getDescription());
            obj.put("resourceType", p.getResourceType());
            obj.put("action", p.getAction());
            array.put(obj);
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("permissions", array));
    }

    private Response getResources() throws SQLException {
        List<ResourceItem> list = resourceRepository.findAll();
        JSONArray array = new JSONArray();
        for (ResourceItem r : list) {
            JSONObject obj = new JSONObject();
            obj.put("id", r.getId());
            obj.put("resourceType", r.getResourceType());
            obj.put("resourceKey", r.getResourceKey());
            obj.put("displayName", r.getDisplayName());
            obj.put("description", r.getDescription());
            obj.put("enabled", r.isEnabled());
            obj.put("metadata", r.getMetadata() != null ? r.getMetadata() : "");
            array.put(obj);
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("resources", array));
    }

    private Response getPolicies() throws SQLException {
        List<ResourceAccessPolicy> list = resourceAccessRepository.findAll();
        JSONArray array = new JSONArray();
        for (ResourceAccessPolicy p : list) {
            JSONObject obj = new JSONObject();
            obj.put("id", p.getId());
            obj.put("principalType", p.getPrincipalType());
            obj.put("principalId", p.getPrincipalId());
            obj.put("resourceId", p.getResourceId());
            obj.put("action", p.getAction());
            obj.put("effect", p.getEffect());
            obj.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            array.put(obj);
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("policies", array));
    }

    private Response createPolicy(JSONObject json, String actor) throws SQLException {
        String principalType = json.getString("principalType");
        Long principalId = json.getLong("principalId");
        Long resourceId = json.getLong("resourceId");
        String action = json.optString("action", "EXECUTE");
        String effect = json.optString("effect", "ALLOW");

        ResourceAccessPolicy policy = new ResourceAccessPolicy(null, principalType, principalId, resourceId, action, effect, null, null);
        resourceAccessRepository.save(policy);

        authService.logAudit(null, actor, "POLICY_CREATED", "POLICY", String.valueOf(policy.getId()), "SUCCESS", "Created access policy effect " + effect);
        return jsonResponse(Response.Status.OK, new JSONObject().put("message", "Policy created").put("id", policy.getId()));
    }

    private Response deletePolicy(Long policyId, String actor) throws SQLException {
        resourceAccessRepository.delete(policyId);
        authService.logAudit(null, actor, "POLICY_DELETED", "POLICY", String.valueOf(policyId), "SUCCESS", "Deleted access policy");
        return jsonResponse(Response.Status.OK, new JSONObject().put("message", "Policy deleted"));
    }

    private Response getAuditLogs() throws SQLException {
        List<AuditLogEntry> logs = auditLogRepository.findAll(200);
        JSONArray array = new JSONArray();
        for (AuditLogEntry l : logs) {
            JSONObject obj = new JSONObject();
            obj.put("id", l.getId());
            obj.put("timestamp", l.getTimestamp() != null ? l.getTimestamp().toString() : "");
            obj.put("actorUsername", l.getActorUsername() != null ? l.getActorUsername() : "SYSTEM");
            obj.put("action", l.getAction());
            obj.put("resourceType", l.getResourceType() != null ? l.getResourceType() : "");
            obj.put("resourceId", l.getResourceId() != null ? l.getResourceId() : "");
            obj.put("outcome", l.getOutcome());
            obj.put("details", l.getDetails() != null ? l.getDetails() : "");
            array.put(obj);
        }
        return jsonResponse(Response.Status.OK, new JSONObject().put("auditLogs", array));
    }

    private Response jsonResponse(Response.Status status, JSONObject json) {
        return NanoHTTPD.newFixedLengthResponse(status, "application/json", json.toString());
    }
}
