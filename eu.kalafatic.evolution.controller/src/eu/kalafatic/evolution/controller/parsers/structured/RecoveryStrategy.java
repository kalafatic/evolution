package eu.kalafatic.evolution.controller.parsers.structured;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Strategy for recovering from structured response failures.
 */
public class RecoveryStrategy {

    public enum RecoveryMode {
        RETRY,
        PARTIAL_FALLBACK,
        SAFE_DETERMINISTIC_FALLBACK,
        FAIL
    }

    public JSONObject recover(String rawResponse, List<String> errors, TaskContext context, Map<String, Class<?>> schema, int attempt) {
        context.consoleLog("[RECOVERY] Attempting recovery for structured response errors: " + String.join(", ", errors));

        // 1. Try Retry if we have an AiService and haven't exhausted attempts
        if (attempt < 2 && context.getAiService() != null) {
             StructuredResponsePipeline pipeline = new StructuredResponsePipeline();
             JSONObject retried = pipeline.retry(rawResponse, errors, context, schema, attempt);
             if (retried != null) return retried;
        }

        // 2. Safe deterministic fallback if recovery is needed
        JSONObject fallback = new JSONObject();
        fallback.put("state", "CLEAR");
        fallback.put("dominantIntent", "Fallback after parsing failure");
        fallback.put("confidence", new JSONObject().put("overallConfidence", 0.1).put("rationale", "Parsing failed: " + String.join("; ", errors)));

        return fallback;
    }
}
