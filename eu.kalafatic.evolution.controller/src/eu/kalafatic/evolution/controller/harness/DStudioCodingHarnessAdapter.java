package eu.kalafatic.evolution.controller.harness;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DStudio coding-harness adapter for EVO (Phase 4, WP-4.10).
 *
 * <p>Delegates coding tasks to the DStudio Harness Platform through the canonical TEIP 1.1
 * protocol. DStudio owns harness execution (OpenCode, Aider, future adapters); EVO keeps
 * Developer Mode, target resolution and Darwin orchestration. The adapter executes inside
 * the resolved development target and streams DStudio events into the Darwin log.</p>
 */
public class DStudioCodingHarnessAdapter implements CodingHarnessAdapter {

    public static final String APPLICATION_ID = "dstudio";
    public static final String APPLICATION_NAME = "DStudio Harness Platform";
    public static final String APPLICATION_TYPE = "DSTUDIO";

    public static final String DEFAULT_ENDPOINT = "http://localhost:48080";
    public static final String CONFIG_ENDPOINT = "endpoint";
    public static final String CONFIG_AUTH_TOKEN = "authToken";
    public static final String CONFIG_HARNESS = "harness";
    public static final String CONFIG_MODEL = "model";
    public static final String CONFIG_TIMEOUT_SECONDS = "timeoutSeconds";

    private volatile String endpoint = DEFAULT_ENDPOINT;
    private volatile String authToken = "";
    private volatile String harness = "";
    private volatile String model = "";
    private volatile long timeoutSeconds = 1800L;

    @Override
    public String getApplicationId() {
        return APPLICATION_ID;
    }

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public String getApplicationType() {
        return APPLICATION_TYPE;
    }

    @Override
    public List<String> getCapabilities() {
        return List.of("CODE_IMPLEMENTATION", "CODE_ANALYSIS");
    }

    @Override
    public boolean isAvailable() {
        return testConnection().success();
    }

    @Override
    public ConnectionTestResult testConnection() {
        try {
            DStudioTeipClient client = new DStudioTeipClient(endpoint, authToken,
                    Duration.ofSeconds(10));
            JSONObject health = client.health();
            String status = health.optString("status", "DOWN");
            if ("UP".equals(status) || "DEGRADED".equals(status)) {
                return ConnectionTestResult.ok("DStudio Harness Platform available at "
                        + endpoint + " (status " + status + ")");
            }
            return ConnectionTestResult.error("DStudio at " + endpoint
                    + " reports status " + status);
        } catch (Exception e) {
            return ConnectionTestResult.error("DStudio Harness Platform unreachable at "
                    + endpoint + ": " + e.getMessage());
        }
    }

    @Override
    public void updateConnectionConfiguration(Map<String, String> configuration) {
        if (configuration == null) {
            return;
        }
        if (configuration.containsKey(CONFIG_ENDPOINT)
                && !configuration.get(CONFIG_ENDPOINT).isBlank()) {
            this.endpoint = configuration.get(CONFIG_ENDPOINT).trim();
        }
        if (configuration.containsKey(CONFIG_AUTH_TOKEN)) {
            this.authToken = configuration.get(CONFIG_AUTH_TOKEN) == null ? ""
                    : configuration.get(CONFIG_AUTH_TOKEN).trim();
        }
        if (configuration.containsKey(CONFIG_HARNESS)) {
            this.harness = configuration.get(CONFIG_HARNESS) == null ? ""
                    : configuration.get(CONFIG_HARNESS).trim();
        }
        if (configuration.containsKey(CONFIG_MODEL)) {
            this.model = configuration.get(CONFIG_MODEL) == null ? ""
                    : configuration.get(CONFIG_MODEL).trim();
        }
        if (configuration.containsKey(CONFIG_TIMEOUT_SECONDS)) {
            try {
                this.timeoutSeconds = Long.parseLong(
                        configuration.get(CONFIG_TIMEOUT_SECONDS));
            } catch (NumberFormatException ignored) {
                this.timeoutSeconds = 1800L;
            }
        }
    }

    @Override
    public Map<String, String> getConnectionConfiguration() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put(CONFIG_ENDPOINT, endpoint);
        config.put(CONFIG_AUTH_TOKEN, authToken.isBlank() ? ""
                : "****** (configured)");
        config.put(CONFIG_HARNESS, harness);
        config.put(CONFIG_MODEL, model);
        config.put(CONFIG_TIMEOUT_SECONDS, String.valueOf(timeoutSeconds));
        return config;
    }

    @Override
    public HarnessExecutionResult executeTask(File targetDir, String instructions,
            String prompt, Consumer<String> logConsumer, BooleanSupplier cancellationSupplier,
            Duration timeout) {
        if (targetDir == null || !targetDir.isDirectory()) {
            return HarnessExecutionResult.error(
                    "DStudio target directory does not exist: " + targetDir);
        }
        Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(timeoutSeconds)
                : Duration.ofSeconds(Math.min(timeout.toSeconds(), timeoutSeconds));
        DStudioTeipClient client = new DStudioTeipClient(endpoint, authToken,
                effectiveTimeout);
        try {
            JSONObject request = new JSONObject();
            request.put("protocol", "TEIP");
            request.put("version", "1.1");
            request.put("requestId", "evo-req-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8));
            request.put("correlationId", "evo-dev-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8));
            request.put("idempotencyKey", "evo-task-" + Integer.toHexString(
                    targetDir.getAbsolutePath().hashCode()));
            JSONObject caller = new JSONObject();
            caller.put("system", "EVO");
            caller.put("projectId", targetDir.getName());
            request.put("caller", caller);
            request.put("capability", "CODE_IMPLEMENTATION");
            request.put("mode", "DEFAULT");
            if (model != null && !model.isBlank()) {
                request.put("model", model);
            }
            request.put("instruction", prompt != null && !prompt.isBlank()
                    ? prompt : instructions);
            JSONArray contextReferences = new JSONArray();
            JSONObject targetReference = new JSONObject();
            targetReference.put("type", "FOLDER");
            targetReference.put("path", targetDir.getName());
            contextReferences.put(targetReference);
            request.put("contextReferences", contextReferences);
            JSONArray grantedScopes = new JSONArray();
            grantedScopes.put("read:workspace");
            grantedScopes.put("write:workspace");
            grantedScopes.put("execute:build");
            request.put("grantedToolScopes", grantedScopes);
            JSONObject parameters = new JSONObject();
            parameters.put("workingDirectory", targetDir.getAbsolutePath());
            if (harness != null && !harness.isBlank()) {
                parameters.put("harness", harness);
            }
            request.put("parameters", parameters);
            JSONObject accepted = client.createExecution(request, null);
            String executionId = accepted.getString("executionId");
            logConsumer.accept("[DStudio] Execution submitted: " + executionId
                    + " (harness platform " + endpoint + ")");
            long deadline = System.currentTimeMillis() + effectiveTimeout.toMillis();
            long cursor = 0;
            while (true) {
                if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
                    client.cancel(executionId);
                    return HarnessExecutionResult.cancelled(
                            "DStudio execution cancelled by EVO");
                }
                if (System.currentTimeMillis() > deadline) {
                    client.cancel(executionId);
                    return HarnessExecutionResult.timedOut(
                            "DStudio execution timed out after "
                                    + effectiveTimeout.toSeconds() + " s");
                }
                for (JSONObject event : client.events(executionId, cursor)) {
                    cursor = Math.max(cursor, event.optLong("sequence", cursor));
                    String type = event.optString("type", "");
                    String message = event.optString("message", "");
                    if ("EXECUTION_LOG".equals(type) || "TOOL_INVOKED".equals(type)
                            || "PROGRESS_UPDATED".equals(type)
                            || "EXECUTION_STARTED".equals(type)
                            || "EXECUTION_COMPLETED".equals(type)
                            || "EXECUTION_FAILED".equals(type)
                            || "EXECUTION_CANCELLED".equals(type)) {
                        logConsumer.accept("[DStudio] " + message);
                    }
                }
                JSONObject status = client.status(executionId);
                String state = status.optString("status", "RUNNING");
                if ("COMPLETED".equals(state)) {
                    JSONObject result = client.result(executionId);
                    if (result == null) {
                        continue;
                    }
                    List<String> modifiedFiles = new ArrayList<>();
                    JSONArray files = result.optJSONArray("modifiedFiles");
                    if (files != null) {
                        for (int i = 0; i < files.length(); i++) {
                            modifiedFiles.add(files.getString(i));
                        }
                    }
                    String summary = result.optString("summary", "");
                    logConsumer.accept("[DStudio] Execution completed: " + summary
                            + " (modified files: " + modifiedFiles.size() + ")");
                    return HarnessExecutionResult.ok(
                            "DStudio execution completed in " + targetDir.getName()
                                    + ": " + summary, modifiedFiles);
                }
                if ("FAILED".equals(state)) {
                    JSONObject result = client.result(executionId);
                    String detail = result == null ? ""
                            : result.optString("summary", "");
                    return HarnessExecutionResult.error(
                            "DStudio execution failed: " + detail);
                }
                if ("CANCELLED".equals(state)) {
                    return HarnessExecutionResult.cancelled(
                            "DStudio execution was cancelled");
                }
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return HarnessExecutionResult.cancelled("DStudio execution interrupted");
        } catch (Exception e) {
            return HarnessExecutionResult.error("DStudio execution failed: "
                    + e.getMessage());
        }
    }
}
