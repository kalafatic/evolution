package eu.kalafatic.evolution.controller.harness;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DCode coding-harness adapter for EVO (Phase 6, WP-6.5).
 *
 * <p>Delegates coding tasks to the native DCode harness through DCode's stable
 * {@code /api/v1} REST surface (FHC 1.0 wire mapping). DCode owns the agent execution loop,
 * tools, workspace operations and harness-specific execution state; EVO keeps Developer
 * Mode, target resolution and Darwin orchestration. DCode never commits in delegated mode —
 * sessions with changes end in {@code WAITING_FOR_APPROVAL} and the resulting diff is
 * reviewed by the calling application.</p>
 */
public class DCodeCodingHarnessAdapter implements CodingHarnessAdapter {

    public static final String APPLICATION_ID = "dcode";
    public static final String APPLICATION_NAME = "DCode — Darwin Code";
    public static final String APPLICATION_TYPE = "DCODE";

    public static final String DEFAULT_ENDPOINT = "http://localhost:48080";
    public static final String CONFIG_ENDPOINT = "endpoint";
    public static final String CONFIG_AUTH_TOKEN = "authToken";
    public static final String CONFIG_PROVIDER_ID = "providerId";
    public static final String CONFIG_MODEL = "model";
    public static final String CONFIG_TIMEOUT_SECONDS = "timeoutSeconds";

    private volatile String endpoint = DEFAULT_ENDPOINT;
    private volatile String authToken = "";
    private volatile String providerId = "";
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
            DCodeClient client = new DCodeClient(endpoint, authToken,
                    Duration.ofSeconds(10));
            JSONObject health = client.health();
            String status = health.optString("status", "DOWN");
            if ("UP".equals(status) || "DEGRADED".equals(status)) {
                return ConnectionTestResult.ok("DCode harness available at " + endpoint
                        + " (status " + status + ")");
            }
            return ConnectionTestResult.error("DCode at " + endpoint
                    + " reports status " + status);
        } catch (Exception e) {
            return ConnectionTestResult.error("DCode harness unreachable at "
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
        if (configuration.containsKey(CONFIG_PROVIDER_ID)) {
            this.providerId = configuration.get(CONFIG_PROVIDER_ID) == null ? ""
                    : configuration.get(CONFIG_PROVIDER_ID).trim();
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
        config.put(CONFIG_PROVIDER_ID, providerId);
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
                    "DCode target directory does not exist: " + targetDir);
        }
        Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(timeoutSeconds)
                : Duration.ofSeconds(Math.min(timeout.toSeconds(), timeoutSeconds));
        DCodeClient client = new DCodeClient(endpoint, authToken, effectiveTimeout);
        String sessionId = null;
        try {
            String workspaceId = client.createWorkspace(
                    "evo-developer-" + Integer.toHexString(
                            targetDir.getAbsolutePath().hashCode()),
                    targetDir.getAbsolutePath()).getString("id");
            client.addRepository(workspaceId, targetDir.getName(),
                    targetDir.getAbsolutePath());
            JSONObject session = client.createSession("EVO Developer Mode task",
                    workspaceId, providerId, model);
            sessionId = session.getString("id");
            client.startSession(sessionId, prompt != null && !prompt.isBlank()
                    ? prompt : instructions);
            logConsumer.accept("[DCode] Execution submitted: session " + sessionId
                    + " (harness " + endpoint + ")");

            long deadline = System.currentTimeMillis() + effectiveTimeout.toMillis();
            int loggedMessages = 0;
            while (true) {
                if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
                    client.cancel(sessionId);
                    return HarnessExecutionResult.cancelled(
                            "DCode execution cancelled by EVO");
                }
                if (System.currentTimeMillis() > deadline) {
                    client.cancel(sessionId);
                    return HarnessExecutionResult.timedOut(
                            "DCode execution timed out after "
                                    + effectiveTimeout.toSeconds() + " s");
                }
                List<JSONObject> messages = client.messages(sessionId);
                for (int i = loggedMessages; i < messages.size(); i++) {
                    JSONObject message = messages.get(i);
                    String role = message.optString("role", "");
                    String content = message.optString("content", "");
                    if (!content.isBlank()) {
                        logConsumer.accept("[DCode:" + role + "] " + content);
                    }
                }
                loggedMessages = messages.size();

                String state = client.session(sessionId).optString("state", "RUNNING");
                switch (state) {
                    case "COMPLETED", "WAITING_FOR_APPROVAL" -> {
                        List<String> modifiedFiles = collectModifiedFiles(client,
                                sessionId);
                        String summary = lastAssistantContent(client, sessionId);
                        if ("WAITING_FOR_APPROVAL".equals(state)) {
                            summary = summary.isBlank()
                                    ? "Execution completed with changes pending review"
                                    : summary + " (changes pending review)";
                        }
                        logConsumer.accept("[DCode] Execution completed: " + summary
                                + " (modified files: " + modifiedFiles.size() + ")");
                        return HarnessExecutionResult.ok("DCode execution completed in "
                                + targetDir.getName() + ": " + summary, modifiedFiles);
                    }
                    case "FAILED" -> {
                        String summary = lastAssistantContent(client, sessionId);
                        return HarnessExecutionResult.error(
                                "DCode execution failed: " + summary);
                    }
                    case "CANCELLED" -> {
                        return HarnessExecutionResult.cancelled(
                                "DCode execution was cancelled");
                    }
                    default -> Thread.sleep(2000);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return HarnessExecutionResult.cancelled("DCode execution interrupted");
        } catch (Exception e) {
            return HarnessExecutionResult.error("DCode execution failed: "
                    + e.getMessage());
        }
    }

    private static List<String> collectModifiedFiles(DCodeClient client, String sessionId)
            throws Exception {
        List<String> files = new ArrayList<>();
        JSONArray repositories = client.changes(sessionId).optJSONArray("repositories");
        if (repositories != null) {
            for (int i = 0; i < repositories.length(); i++) {
                JSONArray changed = repositories.getJSONObject(i)
                        .optJSONArray("changedFiles");
                if (changed != null) {
                    for (int c = 0; c < changed.length(); c++) {
                        files.add(changed.getString(c));
                    }
                }
            }
        }
        return files;
    }

    private static String lastAssistantContent(DCodeClient client, String sessionId)
            throws Exception {
        List<JSONObject> messages = client.messages(sessionId);
        String content = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            JSONObject message = messages.get(i);
            if ("assistant".equals(message.optString("role", ""))) {
                content = message.optString("content", "");
                if (!content.isBlank()) {
                    return content;
                }
            }
        }
        return content;
    }
}
