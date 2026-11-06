package eu.kalafatic.evolution.controller.teip.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Bidirectional JSON serializer and deserializer for TEIP 1.0 DTOs.
 */
public class TeipJsonConverter {

    public static String toJson(TeipHealthResponse health) {
        JSONObject obj = new JSONObject();
        obj.put("protocol", health.getProtocol());
        obj.put("version", health.getVersion());
        obj.put("status", health.getStatus());
        obj.put("application", health.getApplication());
        obj.put("appVersion", health.getAppVersion());
        obj.put("timestamp", health.getTimestamp());
        obj.put("ready", health.isReady());
        obj.put("details", new JSONObject(health.getDetails()));
        return obj.toString(2);
    }

    public static String toJson(TeipCapabilityDescriptor cap) {
        JSONObject obj = new JSONObject();
        obj.put("protocol", cap.getProtocol());
        obj.put("version", cap.getVersion());
        obj.put("timestamp", cap.getTimestamp());
        obj.put("supportedModes", new JSONArray(cap.getSupportedModes()));
        obj.put("agents", new JSONArray(cap.getAgents()));
        obj.put("models", new JSONArray(cap.getModels()));
        obj.put("tools", new JSONArray(cap.getTools()));
        obj.put("features", new JSONObject(cap.getFeatures()));
        return obj.toString(2);
    }

    public static String toJson(TeipAcceptedResponse resp) {
        JSONObject obj = new JSONObject();
        obj.put("protocol", resp.getProtocol());
        obj.put("version", resp.getVersion());
        obj.put("executionId", resp.getExecutionId());
        obj.put("status", resp.getStatus() != null ? resp.getStatus().name() : "QUEUED");
        obj.put("correlationId", resp.getCorrelationId());
        obj.put("createdAt", resp.getCreatedAt());
        obj.put("links", new JSONObject(resp.getLinks()));
        return obj.toString(2);
    }

    public static String toJson(TeipExecutionStatusResponse resp) {
        JSONObject obj = new JSONObject();
        obj.put("protocol", resp.getProtocol());
        obj.put("version", resp.getVersion());
        obj.put("executionId", resp.getExecutionId());
        obj.put("status", resp.getStatus() != null ? resp.getStatus().name() : "QUEUED");
        obj.put("correlationId", resp.getCorrelationId());
        obj.put("progress", resp.getProgress());
        obj.put("statusMessage", resp.getStatusMessage());
        obj.put("createdAt", resp.getCreatedAt());
        obj.put("startedAt", resp.getStartedAt() != null ? resp.getStartedAt() : JSONObject.NULL);
        obj.put("completedAt", resp.getCompletedAt() != null ? resp.getCompletedAt() : JSONObject.NULL);
        obj.put("details", new JSONObject(resp.getDetails()));
        return obj.toString(2);
    }

    public static String toJson(TeipExecutionResult result) {
        JSONObject obj = new JSONObject();
        obj.put("protocol", result.getProtocol());
        obj.put("version", result.getVersion());
        obj.put("executionId", result.getExecutionId());
        obj.put("correlationId", result.getCorrelationId());
        obj.put("status", result.getStatus() != null ? result.getStatus().name() : "COMPLETED");
        obj.put("summary", result.getSummary());
        obj.put("completedAt", result.getCompletedAt());
        obj.put("artifacts", new JSONArray(result.getArtifacts()));
        obj.put("metrics", new JSONObject(result.getMetrics()));
        return obj.toString(2);
    }

    public static String toJson(TeipEvent event) {
        JSONObject obj = new JSONObject();
        obj.put("eventId", event.getEventId());
        obj.put("executionId", event.getExecutionId());
        obj.put("sequence", event.getSequence());
        obj.put("type", event.getType());
        obj.put("timestamp", event.getTimestamp());
        obj.put("correlationId", event.getCorrelationId());
        obj.put("data", new JSONObject(event.getData()));
        return obj.toString();
    }

    public static String toJson(TeipEventsResponse resp) {
        JSONObject obj = new JSONObject();
        obj.put("protocol", resp.getProtocol());
        obj.put("version", resp.getVersion());
        obj.put("executionId", resp.getExecutionId());
        JSONArray eventsArr = new JSONArray();
        for (TeipEvent evt : resp.getEvents()) {
            eventsArr.put(new JSONObject(toJson(evt)));
        }
        obj.put("events", eventsArr);
        obj.put("hasMore", resp.isHasMore());
        obj.put("nextSequence", resp.getNextSequence());
        return obj.toString(2);
    }

    public static String toJson(TeipError err) {
        JSONObject obj = new JSONObject();
        obj.put("protocol", err.getProtocol());
        obj.put("version", err.getVersion());
        obj.put("code", err.getCode());
        obj.put("message", err.getMessage());
        obj.put("requestId", err.getRequestId());
        obj.put("correlationId", err.getCorrelationId());
        obj.put("retryable", err.isRetryable());
        obj.put("timestamp", err.getTimestamp());
        obj.put("details", new JSONObject(err.getDetails()));
        return obj.toString(2);
    }

    public static Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null) return map;
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = json.opt(key);
            if (value instanceof JSONObject) {
                map.put(key, jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonToList((JSONArray) value));
            } else if (value == JSONObject.NULL) {
                map.put(key, null);
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    public static List<Object> jsonToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                list.add(jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(jsonToList((JSONArray) value));
            } else if (value == JSONObject.NULL) {
                list.add(null);
            } else {
                list.add(value);
            }
        }
        return list;
    }

    public static TeipExecutionRequest parseExecutionRequest(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Execution request payload cannot be empty");
        }
        if (jsonStr.length() > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("Execution request exceeds maximum allowed size (5MB)");
        }
        JSONObject obj = new JSONObject(jsonStr);

        TeipExecutionRequest req = new TeipExecutionRequest();
        req.setProtocol(obj.optString("protocol", "TEIP"));
        req.setVersion(obj.optString("version", "1.0"));
        req.setRequestId(obj.optString("requestId", null));
        req.setCorrelationId(obj.optString("correlationId", null));
        req.setIdempotencyKey(obj.optString("idempotencyKey", null));
        req.setTimestamp(obj.optString("timestamp", java.time.Instant.now().toString()));
        req.setCapability(obj.optString("capability", null));
        req.setMode(obj.optString("mode", null));
        req.setInstruction(obj.optString("instruction", null));

        JSONObject callerObj = obj.optJSONObject("caller");
        if (callerObj != null) {
            req.setCaller(jsonToMap(callerObj));
        }

        JSONObject paramsObj = obj.optJSONObject("parameters");
        if (paramsObj != null) {
            req.setParameters(jsonToMap(paramsObj));
        }

        JSONArray scopesArr = obj.optJSONArray("grantedToolScopes");
        if (scopesArr != null) {
            List<String> scopes = new ArrayList<>();
            for (int i = 0; i < scopesArr.length(); i++) {
                scopes.add(scopesArr.optString(i));
            }
            req.setGrantedToolScopes(scopes);
        }

        JSONArray ctxArr = obj.optJSONArray("contextReferences");
        if (ctxArr != null) {
            List<Map<String, Object>> ctxList = new ArrayList<>();
            for (int i = 0; i < ctxArr.length(); i++) {
                JSONObject item = ctxArr.optJSONObject(i);
                if (item != null) {
                    ctxList.add(jsonToMap(item));
                }
            }
            req.setContextReferences(ctxList);
        }

        return req;
    }
}
