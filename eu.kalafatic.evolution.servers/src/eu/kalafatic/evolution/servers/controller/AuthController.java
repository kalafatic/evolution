package eu.kalafatic.evolution.servers.controller;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import eu.kalafatic.evolution.servers.service.AuthService;
import eu.kalafatic.evolution.servers.model.User;
import eu.kalafatic.evolution.servers.model.Session;

public class AuthController {
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().toString();

        try {
            if ("/api/auth/login".equals(uri) && "POST".equals(method)) {
                return login(session);
            } else if ("/api/auth/logout".equals(uri) && "POST".equals(method)) {
                return logout(session);
            } else if ("/api/auth/me".equals(uri) && "GET".equals(method)) {
                return me(session);
            }
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_ERROR, e.getMessage());
        }

        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    private Response login(IHTTPSession session) throws IOException {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (NanoHTTPD.ResponseException e) {
            return errorResponse(Response.Status.INTERNAL_ERROR, e.getMessage());
        }
        String postData = files.get("postData");

        Map<String, String> credentials = objectMapper.readValue(postData, Map.class);
        String username = credentials.get("username");
        String password = credentials.get("password");
        String clientIp = session.getRemoteIpAddress();

        try {
            Optional<String> sessionIdOpt = authService.login(username, password, clientIp);
            if (sessionIdOpt.isPresent()) {
                String sessionId = sessionIdOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("sessionId", sessionId);
                Response res = jsonResponse(Response.Status.OK, response);
                // Use SameSite=Lax for better navigation support across links
                // Removing HttpOnly to support embedded browsers that might have trouble with it in some contexts
                res.addHeader("Set-Cookie", "sessionId=" + sessionId + "; Path=/; SameSite=Lax");
                return res;
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid username or password");
                return jsonResponse(Response.Status.UNAUTHORIZED, response);
            }
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_ERROR, e.getMessage());
        }
    }

    private Response logout(IHTTPSession session) {
        String sessionId = getSessionId(session);
        if (sessionId != null) {
            try {
                authService.logout(sessionId);
            } catch (Exception e) {
                // Ignore logout errors
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        Response res = jsonResponse(Response.Status.OK, response);
        res.addHeader("Set-Cookie", "sessionId=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax");
        return res;
    }

    private Response me(IHTTPSession session) {
        // SWT environment bypass
        if (isAuthorized(session)) {
            Map<String, Object> response = new HashMap<>();
            response.put("username", "EclipseUser");
            response.put("role", "ADMIN");
            response.put("sessionId", "swt-bypass-session");
            response.put("loginTimestamp", new java.util.Date().toString());
            response.put("workflowType", "GENERAL");
            return jsonResponse(Response.Status.OK, response);
        }

        String sessionId = getSessionId(session);
        if (sessionId == null) {
            return errorResponse(Response.Status.UNAUTHORIZED, "Unauthorized");
        }

        try {
            Optional<User> userOpt = authService.validateSession(sessionId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Optional<Session> sessOpt = authService.getSession(sessionId);

                Map<String, Object> response = new HashMap<>();
                response.put("username", user.getUsername());
                response.put("role", user.getRole());
                response.put("sessionId", sessionId);
                if (sessOpt.isPresent()) {
                    Session sess = sessOpt.get();
                    response.put("loginTimestamp", sess.getCreatedAt().toString());
                    response.put("workflowType", sess.getWorkflowType());
                }
                return jsonResponse(Response.Status.OK, response);
            } else {
                return errorResponse(Response.Status.UNAUTHORIZED, "Session expired or invalid");
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof java.sql.SQLException) {
                System.err.println("Auth DB busy error: " + e.getMessage());
                return errorResponse(Response.Status.INTERNAL_ERROR, "Database busy. Please retry.");
            }
            return errorResponse(Response.Status.INTERNAL_ERROR, e.getMessage());
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_ERROR, e.getMessage());
        }
    }

    private boolean isAuthorized(IHTTPSession session) {
        // Check global authentication flag via system property or orchestrator
        String authProp = System.getProperty("evolution.api.authenticate");
        if ("false".equalsIgnoreCase(authProp)) {
            return true;
        }

        try {
            eu.kalafatic.evolution.model.orchestration.Orchestrator orch =
                eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (orch != null && orch.getServerSettings() != null) {
                if (!orch.getServerSettings().isAuthenticate()) {
                    return true;
                }
            } else if (authProp == null) {
                // Default to bypass if nothing is configured
                return true;
            }
        } catch (Throwable e) {
            if (authProp == null) return true;
        }

        String runtimeHeader = session.getHeaders().get("x-evo-runtime");
        if (runtimeHeader == null) runtimeHeader = session.getHeaders().get("X-Evo-Runtime");
        String runtimeParam = session.getParms().get("runtime");
        return "SWT".equalsIgnoreCase(runtimeHeader) || "SWT".equalsIgnoreCase(runtimeParam);
    }

    private String getSessionId(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();

        // 1. Authorization Header (Case-insensitive)
        String authHeader = headers.get("authorization");
        if (authHeader == null) authHeader = headers.get("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty() && !"undefined".equals(token) && !"null".equals(token)) {
                return token;
            }
        }

        // 2. Cookie (via NanoHTTPD CookieHandler)
        String sessionId = session.getCookies().read("sessionId");
        if (sessionId != null && !sessionId.isEmpty()) {
            return trimQuotes(sessionId);
        }

        // 3. Manual Cookie Header parsing (fallback)
        String cookieHeader = headers.get("cookie");
        if (cookieHeader == null) cookieHeader = headers.get("Cookie");

        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length >= 2 && "sessionId".equalsIgnoreCase(parts[0])) {
                    return trimQuotes(parts[1]);
                }
            }
        }

        // 4. Query Parameter (fallback)
        String paramId = session.getParms().get("sessionId");
        return (paramId != null && !paramId.isEmpty()) ? paramId : null;
    }

    private String trimQuotes(String val) {
        if (val == null) return null;
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private Response jsonResponse(Response.Status status, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return NanoHTTPD.newFixedLengthResponse(status, "application/json", json);
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_ERROR, "JSON Error");
        }
    }

    private Response errorResponse(Response.Status status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return jsonResponse(status, error);
    }
}
