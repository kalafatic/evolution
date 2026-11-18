package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * WP8 - run-scoped iteration ledger.
 *
 * Persisted at &lt;run&gt;/iteration-ledger.json. Every stage records:
 * iterationId, stage, status, timestamp, taskId and diagnostics.
 * The Darwin IterationManager remains the sole transition authority for
 * in-process Darwin iterations; this ledger records the externally observable
 * lifecycle evidence for the self-dev loop.
 */
public class SelfDevIterationLedger {

    public static final String LEDGER_FILE_NAME = "iteration-ledger.json";

    public enum Stage {
        OBJECTIVE_RECEIVED,
        ANALYSIS,
        RUN_PREPARED,
        SOURCE_MODIFICATION,
        STATIC_VALIDATION,
        BUILD,
        TEST,
        EXPORT,
        GIT_VALIDATION,
        DEPLOYMENT,
        OLD_CHILD_STOPPED,
        NEW_CHILD_STARTED,
        OSGI_VERIFIED,
        APPLICATION_VERIFIED,
        HEALTH_CHECK,
        ACCEPTED,
        REJECTED,
        COMMIT_PREPARED,
        PUSH_PENDING,
        TERMINATED
    }

    public static class Entry {
        private final String iterationId;
        private final Stage stage;
        private final String status;
        private final long timestamp;
        private final String taskId;
        private final String diagnostics;

        public Entry(String iterationId, Stage stage, String status, long timestamp, String taskId, String diagnostics) {
            this.iterationId = iterationId;
            this.stage = stage;
            this.status = status;
            this.timestamp = timestamp;
            this.taskId = taskId;
            this.diagnostics = diagnostics;
        }

        public String getIterationId() { return iterationId; }
        public Stage getStage() { return stage; }
        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
        public String getTaskId() { return taskId; }
        public String getDiagnostics() { return diagnostics; }
    }

    private final File ledgerFile;
    private final String runId;

    public SelfDevIterationLedger(SelfDevContext context) {
        this.runId = context != null ? context.getRunId() : "unknown-run";
        this.ledgerFile = ledgerFile(context);
    }

    public static File ledgerFile(SelfDevContext context) {
        if (context == null || context.getBuildDirectory() == null) {
            return null;
        }
        File runDir = context.getBuildDirectory().getParentFile();
        return runDir != null ? new File(runDir, LEDGER_FILE_NAME) : null;
    }

    public File getLedgerFile() {
        return ledgerFile;
    }

    public String getRunId() {
        return runId;
    }

    public synchronized void record(String iterationId, Stage stage, String status, String taskId, String diagnostics) {
        if (ledgerFile == null) {
            return;
        }
        try {
            JSONObject root = readRoot();
            JSONArray entries = root.optJSONArray("entries");
            if (entries == null) {
                entries = new JSONArray();
                root.put("entries", entries);
            }
            JSONObject entry = new JSONObject();
            entry.put("iterationId", iterationId);
            entry.put("stage", stage.name());
            entry.put("status", status);
            entry.put("timestamp", System.currentTimeMillis());
            entry.put("taskId", taskId);
            entry.put("diagnostics", diagnostics);
            entries.put(entry);
            File parent = ledgerFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.writeString(ledgerFile.toPath(), root.toString(2), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[IterationLedger] Failed to record " + stage + ": " + e.getMessage());
        }
    }

    public synchronized List<Entry> readEntries() {
        List<Entry> result = new ArrayList<>();
        if (ledgerFile == null || !ledgerFile.exists()) {
            return result;
        }
        try {
            JSONObject root = readRoot();
            JSONArray entries = root.optJSONArray("entries");
            if (entries != null) {
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject e = entries.getJSONObject(i);
                    result.add(new Entry(
                            e.optString("iterationId", null),
                            Stage.valueOf(e.optString("stage", Stage.ANALYSIS.name())),
                            e.optString("status", "UNKNOWN"),
                            e.optLong("timestamp", -1),
                            e.optString("taskId", null),
                            e.optString("diagnostics", null)));
                }
            }
        } catch (Exception e) {
            System.err.println("[IterationLedger] Failed to read ledger: " + e.getMessage());
        }
        return result;
    }

    private JSONObject readRoot() throws Exception {
        if (ledgerFile.exists()) {
            String content = Files.readString(ledgerFile.toPath(), StandardCharsets.UTF_8);
            if (content != null && !content.trim().isEmpty()) {
                return new JSONObject(content);
            }
        }
        JSONObject root = new JSONObject();
        root.put("runId", runId);
        return root;
    }
}
