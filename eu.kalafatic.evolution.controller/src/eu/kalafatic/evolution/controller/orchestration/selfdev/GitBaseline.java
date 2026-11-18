package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

/**
 * WP9 - Git iteration baseline.
 *
 * Captured read-only at the start of every self-dev run: branch, HEAD revision
 * and the full working-tree status. Iteration changes are computed strictly
 * against this baseline; pre-existing user changes (present in the baseline)
 * are never attributed to an autonomous iteration.
 */
public class GitBaseline {

    public static final String BASELINE_FILE_NAME = "git-baseline.json";

    private final String runId;
    private final String branch;
    private final String head;
    private final List<String> statusLines;
    private final long capturedAt;

    public GitBaseline(String runId, String branch, String head, List<String> statusLines, long capturedAt) {
        this.runId = runId;
        this.branch = branch;
        this.head = head;
        this.statusLines = statusLines != null ? new ArrayList<>(statusLines) : new ArrayList<>();
        this.capturedAt = capturedAt;
    }

    public String getRunId() { return runId; }
    public String getBranch() { return branch; }
    public String getHead() { return head; }
    public List<String> getStatusLines() { return java.util.Collections.unmodifiableList(statusLines); }
    public long getCapturedAt() { return capturedAt; }

    /**
     * Read-only baseline capture. Never mutates Git state.
     */
    public static GitBaseline capture(SelfDevContext context, GitManager manager) {
        String branch = null;
        String head = null;
        List<String> statusLines = new ArrayList<>();
        try {
            branch = manager.getCurrentBranch();
        } catch (Exception ignored) {}
        try {
            head = manager.getHeadCommit();
        } catch (Exception ignored) {}
        try {
            String status = manager.getStatusPorcelain();
            if (status != null && !status.trim().isEmpty()) {
                for (String line : status.split("\n")) {
                    if (!line.trim().isEmpty()) {
                        statusLines.add(line);
                    }
                }
            }
        } catch (Exception ignored) {}
        return new GitBaseline(context != null ? context.getRunId() : null, branch, head, statusLines, System.currentTimeMillis());
    }

    /**
     * Computes the iteration-owned change set: working-tree entries that were
     * NOT already present in the baseline. Pre-existing user changes are
     * excluded by construction (D-41/D-47).
     */
    public Set<String> iterationChangedPaths(List<String> currentStatusLines) {
        Set<String> baselinePaths = new LinkedHashSet<>();
        for (String line : statusLines) {
            String path = extractPath(line);
            if (path != null) {
                baselinePaths.add(path);
            }
        }
        Set<String> changed = new LinkedHashSet<>();
        if (currentStatusLines != null) {
            for (String line : currentStatusLines) {
                String path = extractPath(line);
                if (path != null && !baselinePaths.contains(path)) {
                    changed.add(path);
                }
            }
        }
        return changed;
    }

    private static String extractPath(String statusLine) {
        if (statusLine == null || statusLine.length() < 3) {
            return null;
        }
        String path = statusLine.substring(3).trim();
        if (path.contains(" -> ")) {
            path = path.substring(path.indexOf(" -> ") + 4).trim();
        }
        return path.isEmpty() ? null : path;
    }

    public static File baselineFile(SelfDevContext context) {
        if (context == null || context.getBuildDirectory() == null) {
            return null;
        }
        File runDir = context.getBuildDirectory().getParentFile();
        return runDir != null ? new File(runDir, BASELINE_FILE_NAME) : null;
    }

    public static void persist(SelfDevContext context, GitBaseline baseline) {
        File file = baselineFile(context);
        if (file == null) {
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("runId", baseline.getRunId());
            o.put("branch", baseline.getBranch());
            o.put("head", baseline.getHead());
            o.put("capturedAt", baseline.getCapturedAt());
            o.put("statusLines", new org.json.JSONArray(baseline.getStatusLines()));
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.writeString(file.toPath(), o.toString(2), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[GitBaseline] Failed to persist baseline: " + e.getMessage());
        }
    }

    public static GitBaseline load(SelfDevContext context) {
        File file = baselineFile(context);
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            org.json.JSONArray arr = o.optJSONArray("statusLines");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    lines.add(arr.optString(i));
                }
            }
            return new GitBaseline(o.optString("runId", null), o.optString("branch", null),
                    o.optString("head", null), lines, o.optLong("capturedAt", -1));
        } catch (Exception e) {
            return null;
        }
    }
}
