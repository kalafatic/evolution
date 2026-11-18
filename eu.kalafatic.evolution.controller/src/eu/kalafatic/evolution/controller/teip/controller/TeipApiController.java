package eu.kalafatic.evolution.controller.teip.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.teip.dto.TeipAcceptedResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipCapabilityDescriptor;
import eu.kalafatic.evolution.controller.teip.dto.TeipError;
import eu.kalafatic.evolution.controller.teip.dto.TeipEventsResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionRequest;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionResult;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionStatusResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipHealthResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipJsonConverter;
import eu.kalafatic.evolution.controller.teip.dto.TeipVersionMismatchException;
import eu.kalafatic.evolution.controller.teip.service.TeipExecutionManager;
import eu.kalafatic.evolution.controller.teip.store.TeipEventStore;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.ResponseException;

/**
 * Controller handling all TEIP 1.1 HTTP requests under /teip/v1.
 */
public class TeipApiController {

    private final TeipExecutionManager executionManager = TeipExecutionManager.getInstance();

    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            // TEIP 1.1 §6: Authorization: Bearer <token> is REQUIRED and ENFORCED.
            // Dev-mode opt-out (loopback-only without a token) MUST be explicit configuration.
            String configuredToken = System.getProperty("evo.teip.auth.token");
            if (configuredToken != null && !configuredToken.trim().isEmpty()) {
                String authHeader = session.getHeaders().get("authorization");
                if (authHeader == null || !authHeader.equalsIgnoreCase("Bearer " + configuredToken.trim())) {
                    TeipError unauth = new TeipError("AUTHENTICATION_FAILED", "Invalid or missing Bearer authorization token", null, null, false);
                    return jsonResponse(Response.Status.UNAUTHORIZED, TeipJsonConverter.toJson(unauth));
                }
            } else if (!isExplicitDevModeAllowed(session)) {
                TeipError unauth = new TeipError("AUTHENTICATION_FAILED",
                        "No TEIP bearer token configured (evo.teip.auth.token). Dev-mode opt-out requires evo.teip.devMode=true and a loopback connection.",
                        null, null, false);
                return jsonResponse(Response.Status.UNAUTHORIZED, TeipJsonConverter.toJson(unauth));
            }

            if (Method.GET.equals(method) && "/teip/v1/health".equals(uri)) {
                return handleHealth();
            } else if (Method.GET.equals(method) && "/teip/v1/capabilities".equals(uri)) {
                return handleCapabilities();
            } else if (Method.POST.equals(method) && "/teip/v1/executions".equals(uri)) {
                return handleCreateExecution(session);
            } else if (uri.startsWith("/teip/v1/executions/")) {
                String sub = uri.substring("/teip/v1/executions/".length());
                if (sub.endsWith("/events")) {
                    if (!Method.GET.equals(method)) {
                        TeipError methErr = new TeipError("METHOD_NOT_ALLOWED", "Method " + method + " not allowed on /events", null, null, false);
                        return jsonResponse(Response.Status.METHOD_NOT_ALLOWED, TeipJsonConverter.toJson(methErr));
                    }
                    String execId = sub.substring(0, sub.length() - "/events".length());
                    return handleGetEvents(execId, session);
                } else if (sub.endsWith("/result")) {
                    if (!Method.GET.equals(method)) {
                        TeipError methErr = new TeipError("METHOD_NOT_ALLOWED", "Method " + method + " not allowed on /result", null, null, false);
                        return jsonResponse(Response.Status.METHOD_NOT_ALLOWED, TeipJsonConverter.toJson(methErr));
                    }
                    String execId = sub.substring(0, sub.length() - "/result".length());
                    return handleGetResult(execId);
                } else if (sub.endsWith("/cancel")) {
                    if (!Method.POST.equals(method)) {
                        TeipError methErr = new TeipError("METHOD_NOT_ALLOWED", "Method " + method + " not allowed on /cancel", null, null, false);
                        return jsonResponse(Response.Status.METHOD_NOT_ALLOWED, TeipJsonConverter.toJson(methErr));
                    }
                    String execId = sub.substring(0, sub.length() - "/cancel".length());
                    return handleCancelExecution(execId, session);
                } else if (Method.GET.equals(method)) {
                    return handleGetStatus(sub);
                }
            }

            TeipError notFound = new TeipError("NOT_FOUND", "Endpoint not found: " + uri, null, null, false);
            return jsonResponse(Response.Status.NOT_FOUND, TeipJsonConverter.toJson(notFound));

        } catch (TeipVersionMismatchException e) {
            TeipError err = new TeipError("VERSION_MISMATCH", e.getMessage(), null, null, false);
            return jsonResponse(Response.Status.BAD_REQUEST, TeipJsonConverter.toJson(err));
        } catch (IllegalArgumentException e) {
            TeipError err = new TeipError("VALIDATION_FAILED", e.getMessage(), null, null, false);
            return jsonResponse(Response.Status.BAD_REQUEST, TeipJsonConverter.toJson(err));
        } catch (SecurityException e) {
            // TEIP 1.1 §5: canonical authorization code is AUTHORIZATION_DENIED
            TeipError err = new TeipError("AUTHORIZATION_DENIED", e.getMessage(), null, null, false);
            return jsonResponse(Response.Status.FORBIDDEN, TeipJsonConverter.toJson(err));
        } catch (Throwable t) {
            // TEIP 1.1 §5: canonical internal error code is INTERNAL_ERROR (non-retryable)
            TeipError err = new TeipError("INTERNAL_ERROR", t.getMessage(), null, null, false);
            return jsonResponse(Response.Status.INTERNAL_ERROR, TeipJsonConverter.toJson(err));
        }
    }

    /**
     * Explicit development opt-out (TEIP 1.1 §6): only when evo.teip.devMode=true AND the
     * caller is on loopback may a request be admitted without a bearer token.
     */
    private boolean isExplicitDevModeAllowed(IHTTPSession session) {
        String devMode = System.getProperty("evo.teip.devMode");
        if (devMode == null || !"true".equalsIgnoreCase(devMode.trim())) {
            return false;
        }
        try {
            String remoteIp = session.getRemoteIpAddress();
            if (remoteIp == null) return false;
            java.net.InetAddress addr = java.net.InetAddress.getByName(remoteIp);
            return addr.isLoopbackAddress();
        } catch (Throwable t) {
            return false;
        }
    }

    private Response handleHealth() {
        TeipHealthResponse health = executionManager.checkHealth();
        Response.Status status = health.isReady() ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;
        return jsonResponse(status, TeipJsonConverter.toJson(health));
    }

    private Response handleCapabilities() {
        TeipCapabilityDescriptor cap = executionManager.getCapabilities();
        return jsonResponse(Response.Status.OK, TeipJsonConverter.toJson(cap));
    }

    private Response handleCreateExecution(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        if (postData == null || postData.trim().isEmpty()) {
            postData = session.getQueryParameterString();
        }

        TeipExecutionRequest req = TeipJsonConverter.parseExecutionRequest(postData);

        // TEIP 1.1 §4.3: Idempotency-Key header is accepted; body key takes precedence
        if (req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank()) {
            String headerKey = session.getHeaders().get("idempotency-key");
            if (headerKey != null && !headerKey.isBlank()) {
                req.setIdempotencyKey(headerKey.trim());
            }
        }

        TeipExecutionManager.CreateOutcome outcome = executionManager.createExecution(req);
        // TEIP 1.1 §4.3: 202 Accepted (new), 200 OK (idempotent duplicate)
        Response.Status status = outcome.duplicate() ? Response.Status.OK : Response.Status.ACCEPTED;
        return jsonResponse(status, TeipJsonConverter.toJson(outcome.response()));
    }

    private Response handleGetStatus(String executionId) {
        TeipExecutionStatusResponse status = executionManager.getStatus(executionId);
        if (status == null) {
            TeipError notFound = new TeipError("NOT_FOUND", "Execution not found: " + executionId, null, null, false);
            return jsonResponse(Response.Status.NOT_FOUND, TeipJsonConverter.toJson(notFound));
        }
        return jsonResponse(Response.Status.OK, TeipJsonConverter.toJson(status));
    }

    private Response handleGetResult(String executionId) {
        TeipExecutionResult res = executionManager.getResult(executionId);
        if (res == null) {
            TeipExecutionStatusResponse status = executionManager.getStatus(executionId);
            if (status == null) {
                TeipError notFound = new TeipError("NOT_FOUND", "Execution not found: " + executionId, null, null, false);
                return jsonResponse(Response.Status.NOT_FOUND, TeipJsonConverter.toJson(notFound));
            }
            // If running or queued, result is not yet available.
            // TEIP 1.1 §4.6/§5: CONFLICT is non-retryable and carries details.currentStatus.
            TeipError err = new TeipError("CONFLICT", "Execution is not yet completed. Current status: " + status.getStatus(), null, null, false);
            err.getDetails().put("currentStatus", status.getStatus().name());
            return jsonResponse(Response.Status.CONFLICT, TeipJsonConverter.toJson(err));
        }
        return jsonResponse(Response.Status.OK, TeipJsonConverter.toJson(res));
    }

    private Response handleCancelExecution(String executionId, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        String reason = "Cancelled by operator";
        if (postData != null && !postData.trim().isEmpty()) {
            try {
                JSONObject obj = new JSONObject(postData);
                if (obj.has("reason")) {
                    reason = obj.getString("reason");
                }
            } catch (Exception ignored) {}
        }

        boolean cancelled = executionManager.cancelExecution(executionId, reason);
        if (!cancelled) {
            TeipError notFound = new TeipError("NOT_FOUND", "Execution not found: " + executionId, null, null, false);
            return jsonResponse(Response.Status.NOT_FOUND, TeipJsonConverter.toJson(notFound));
        }

        JSONObject resp = new JSONObject();
        resp.put("protocol", "TEIP");
        resp.put("version", "1.1");
        resp.put("executionId", executionId);
        resp.put("status", "CANCELLED");
        resp.put("message", "Execution cancellation accepted.");
        return jsonResponse(Response.Status.OK, resp.toString(2));
    }

    private Response handleGetEvents(String executionId, IHTTPSession session) {
        String afterSeqStr = session.getParms().get("afterSequence");
        String limitStr = session.getParms().get("limit");

        long afterSeq = 0;
        int limit = 50;

        if (afterSeqStr != null) {
            try { afterSeq = Long.parseLong(afterSeqStr); } catch (Exception ignored) {}
        }
        if (limitStr != null) {
            try { limit = Integer.parseInt(limitStr); } catch (Exception ignored) {}
        }

        TeipEventsResponse eventsResp = TeipEventStore.getInstance().getEvents(executionId, afterSeq, limit);
        return jsonResponse(Response.Status.OK, TeipJsonConverter.toJson(eventsResp));
    }

    private Response jsonResponse(Response.Status status, String json) {
        Response resp = NanoHTTPD.newFixedLengthResponse(status, "application/json", json);
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return resp;
    }
}
