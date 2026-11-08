package eu.kalafatic.evolution.forge.model.inference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * InferenceExecutionTrace captures the full lifecycle trace of an inference request with structured events.
 */
public class InferenceExecutionTrace implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(InferenceExecutionTrace.class.getName());

    public enum EventType {
        REQUEST_RECEIVED,
        MODEL_RESOLVED,
        ARTIFACT_VERIFIED,
        ENGINE_SELECTED,
        MODEL_LOADED,
        EXECUTION_STARTED,
        EXECUTION_COMPLETED,
        RESPONSE_DELIVERED,
        MODEL_RESOLUTION_FAILED,
        ENGINE_INITIALIZATION_FAILED,
        EXECUTION_FAILED,
        FALLBACK_ACTIVATED,
        RESPONSE_DELIVERY_FAILED
    }

    public static class TraceEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        private final long timestamp;
        private final EventType type;
        private final String message;
        private final String details;

        public TraceEvent(EventType type, String message, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.message = message != null ? message : "";
            this.details = details != null ? details : "";
        }

        public long getTimestamp() { return timestamp; }
        public EventType getType() { return type; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            obj.put("timestamp", timestamp);
            obj.put("type", type.name());
            obj.put("message", message);
            obj.put("details", details);
            return obj;
        }
    }

    private final String inferenceId;
    private final String sessionId;
    private final List<TraceEvent> events = Collections.synchronizedList(new ArrayList<>());
    private final InferenceProvenance provenance;

    public InferenceExecutionTrace(String inferenceId, String sessionId) {
        this.inferenceId = inferenceId != null ? inferenceId : "UNKNOWN";
        this.sessionId = sessionId != null ? sessionId : "UNKNOWN";
        this.provenance = new InferenceProvenance();
        this.provenance.setInferenceId(this.inferenceId);
        this.provenance.setSessionId(this.sessionId);
    }

    public void addEvent(EventType type, String message, String details) {
        TraceEvent event = new TraceEvent(type, message, details);
        events.add(event);
        LOGGER.info(String.format("[INFERENCE][%s][%s] %s %s", inferenceId, type.name(), message, (details != null && !details.isEmpty() ? "(" + details + ")" : "")).trim());
    }

    public String getInferenceId() { return inferenceId; }
    public String getSessionId() { return sessionId; }
    public List<TraceEvent> getEvents() { return Collections.unmodifiableList(events); }
    public InferenceProvenance getProvenance() { return provenance; }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("inferenceId", inferenceId);
        obj.put("sessionId", sessionId);
        obj.put("provenance", provenance.toJsonObject());
        JSONArray arr = new JSONArray();
        synchronized (events) {
            for (TraceEvent e : events) {
                arr.put(e.toJsonObject());
            }
        }
        obj.put("events", arr);
        return obj;
    }
}
