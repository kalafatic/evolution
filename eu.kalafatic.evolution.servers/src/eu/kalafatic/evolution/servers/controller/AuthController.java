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
                res.addHeader("Set-Cookie", "sessionId=" + sessionId + "; Path=/; HttpOnly; SameSite=Strict");
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
        res.addHeader("Set-Cookie", "sessionId=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
        return res;
    }

    private Response me(IHTTPSession session) {
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
                return errorResponse(Response.Status.UNAUTHORIZED, "Session expired");
            }
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_ERROR, e.getMessage());
        }
    }

    private String getSessionId(IHTTPSession session) {
        // Try header first
        String authHeader = session.getHeaders().get("authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // Try cookies
        String sessionId = session.getCookies().read("sessionId");
        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionId;
        }
        // Then query param (fallback)
        return session.getParms().get("sessionId");
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
