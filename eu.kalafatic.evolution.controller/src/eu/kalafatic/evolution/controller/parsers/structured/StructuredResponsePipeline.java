package eu.kalafatic.evolution.controller.parsers.structured;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Orchestrates the robust structured-response reliability pipeline.
 */
public class StructuredResponsePipeline {

    private final JsonRepairEngine repairEngine = new JsonRepairEngine();
    private final SchemaValidator schemaValidator = new SchemaValidator();
    private final SemanticValidator semanticValidator = new SemanticValidator();
    private final RecoveryStrategy recoveryStrategy = new RecoveryStrategy();

    public JSONObject process(String rawResponse, Map<String, Class<?>> schema, TaskContext context) {
        return process(rawResponse, schema, context, 0);
    }

    private JSONObject process(String rawResponse, Map<String, Class<?>> schema, TaskContext context, int attempt) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return recoveryStrategy.recover(rawResponse, List.of("Empty response"), context, schema, attempt);
        }

        // 1. RAW CAPTURE & REPAIR
        String repaired = repairEngine.repair(rawResponse);

        if (!repaired.equals(rawResponse.trim())) {
            context.consoleLog("[PIPELINE] JSON repaired successfully.");
            context.getOrchestrationState().getMetadata().put("structured_repair_performed", true);
            // Increment repair statistics if metrics are available in the future
        }

        JSONObject json = null;
        try {
            json = new JSONObject(repaired);
        } catch (JSONException e) {
            context.consoleLog("[PIPELINE] JSON Repair failed to produce valid JSON: " + e.getMessage());
            context.getOrchestrationState().getMetadata().put("structured_repair_failed", true);
            return recoveryStrategy.recover(repaired, List.of("Malformed JSON: " + e.getMessage()), context, schema, attempt);
        }

        // 2. SCHEMA VALIDATION
        SchemaValidator.ValidationResult schemaResult = schemaValidator.validate(json, schema);
        if (!schemaResult.isValid()) {
            context.consoleLog("[PIPELINE] Schema validation failed: " + String.join(", ", schemaResult.getErrors()));
            context.getOrchestrationState().getMetadata().put("structured_validation_failed", true);
            return recoveryStrategy.recover(repaired, schemaResult.getErrors(), context, schema, attempt);
        }

        // 3. SEMANTIC VALIDATION
        SemanticValidator.SemanticResult semanticResult = semanticValidator.validate(json);
        if (!semanticResult.isValid()) {
            context.consoleLog("[PIPELINE] Semantic validation failed: " + String.join(", ", semanticResult.getErrors()));
            context.getOrchestrationState().getMetadata().put("structured_semantic_failed", true);
            return recoveryStrategy.recover(repaired, semanticResult.getErrors(), context, schema, attempt);
        }

        context.consoleLog("[PIPELINE] Structured response successfully validated.");
        context.getOrchestrationState().getMetadata().put("structured_validation_passed", true);
        return json;
    }

    public JSONObject retry(String repaired, List<String> errors, TaskContext context, Map<String, Class<?>> schema, int attempt) {
        if (attempt >= 2) {
             context.consoleLog("[PIPELINE] Maximum retry attempts reached.");
             return null;
        }

        context.consoleLog("[PIPELINE] Retrying with repair prompt (attempt " + (attempt + 1) + ")...");

        String repairPrompt = "Your previous JSON response had the following errors:\n" +
                             String.join("\n", errors) +
                             "\n\nPlease fix the JSON and return ONLY the valid JSON object.";

        try {
            String newResponse = context.getAiService().sendRequest(context.getOrchestrator(), repairPrompt, context);
            return process(newResponse, schema, context, attempt + 1);
        } catch (Exception e) {
            context.consoleLog("[PIPELINE] Retry failed: " + e.getMessage());
            return null;
        }
    }
}
