package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Persistent, run-scoped process ownership record (WP5).
 *
 * Persisted at &lt;run&gt;/runtime/process-ownership.json with one entry per
 * owned child kind (supervisor / evo). Every record carries: runId, supervisor
 * instance ID, PID, parent PID, executable, command line, working directory,
 * runtime path, start time, effective ports and exit status.
 */
public class ProcessOwnershipRecord {

    public enum ProcessKind {
        SUPERVISOR,
        EVO_RCP
    }

    public static final String OWNERSHIP_FILE_NAME = "process-ownership.json";

    private final ProcessKind kind;
    private final String runId;
    private final String supervisorInstanceId;
    private final long pid;
    private final long parentPid;
    private final String executable;
    private final String commandLine;
    private final String workingDirectory;
    private final String runtimePath;
    private final long startTimeMillis;
    private final Map<String, Integer> effectivePorts;
    private final Integer exitStatus;

    public ProcessOwnershipRecord(ProcessKind kind, String runId, String supervisorInstanceId,
                                  long pid, long parentPid, String executable, String commandLine,
                                  String workingDirectory, String runtimePath, long startTimeMillis,
                                  Map<String, Integer> effectivePorts, Integer exitStatus) {
        this.kind = kind;
        this.runId = runId;
        this.supervisorInstanceId = supervisorInstanceId;
        this.pid = pid;
        this.parentPid = parentPid;
        this.executable = executable;
        this.commandLine = commandLine;
        this.workingDirectory = workingDirectory;
        this.runtimePath = runtimePath;
        this.startTimeMillis = startTimeMillis;
        this.effectivePorts = effectivePorts != null ? new LinkedHashMap<>(effectivePorts) : new LinkedHashMap<>();
        this.exitStatus = exitStatus;
    }

    public ProcessKind getKind() { return kind; }
    public String getRunId() { return runId; }
    public String getSupervisorInstanceId() { return supervisorInstanceId; }
    public long getPid() { return pid; }
    public long getParentPid() { return parentPid; }
    public String getExecutable() { return executable; }
    public String getCommandLine() { return commandLine; }
    public String getWorkingDirectory() { return workingDirectory; }
    public String getRuntimePath() { return runtimePath; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public Map<String, Integer> getEffectivePorts() { return effectivePorts; }
    public Integer getExitStatus() { return exitStatus; }

    // ------------------------------------------------------------------
    // Persistence (single JSON file per run, one entry per kind)
    // ------------------------------------------------------------------

    private static File ownershipFile(SelfDevContext context) {
        if (context == null || context.getRuntimeDirectory() == null) {
            return null;
        }
        return new File(context.getRuntimeDirectory(), OWNERSHIP_FILE_NAME);
    }

    public static synchronized void write(SelfDevContext context, ProcessOwnershipRecord record) {
        File file = ownershipFile(context);
        if (file == null) {
            return;
        }
        try {
            JSONObject root = load(file);
            root.put(record.getKind().name(), toJson(record));
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.writeString(file.toPath(), root.toString(2), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[ProcessOwnership] Failed to persist ownership record: " + e.getMessage());
        }
    }

    public static synchronized ProcessOwnershipRecord read(SelfDevContext context, ProcessKind kind) {
        File file = ownershipFile(context);
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            JSONObject root = load(file);
            JSONObject entry = root.optJSONObject(kind.name());
            return entry != null ? fromJson(kind, entry) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized void clear(SelfDevContext context, ProcessKind kind, Integer exitStatus) {
        ProcessOwnershipRecord existing = read(context, kind);
        File file = ownershipFile(context);
        if (file == null || existing == null) {
            return;
        }
        ProcessOwnershipRecord updated = new ProcessOwnershipRecord(
                existing.getKind(), existing.getRunId(), existing.getSupervisorInstanceId(),
                existing.getPid(), existing.getParentPid(), existing.getExecutable(),
                existing.getCommandLine(), existing.getWorkingDirectory(), existing.getRuntimePath(),
                existing.getStartTimeMillis(), existing.getEffectivePorts(), exitStatus);
        write(context, updated);
    }

    private static JSONObject load(File file) throws Exception {
        if (file.exists()) {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (content != null && !content.trim().isEmpty()) {
                return new JSONObject(content);
            }
        }
        return new JSONObject();
    }

    private static JSONObject toJson(ProcessOwnershipRecord r) {
        JSONObject o = new JSONObject();
        o.put("kind", r.getKind().name());
        o.put("runId", r.getRunId());
        o.put("supervisorInstanceId", r.getSupervisorInstanceId());
        o.put("pid", r.getPid());
        o.put("parentPid", r.getParentPid());
        o.put("executable", r.getExecutable());
        o.put("commandLine", r.getCommandLine());
        o.put("workingDirectory", r.getWorkingDirectory());
        o.put("runtimePath", r.getRuntimePath());
        o.put("startTimeMillis", r.getStartTimeMillis());
        o.put("startTime", Instant.ofEpochMilli(r.getStartTimeMillis()).toString());
        o.put("effectivePorts", new JSONObject(r.getEffectivePorts()));
        if (r.getExitStatus() != null) {
            o.put("exitStatus", r.getExitStatus());
        }
        return o;
    }

    private static ProcessOwnershipRecord fromJson(ProcessKind kind, JSONObject o) {
        Map<String, Integer> ports = new LinkedHashMap<>();
        JSONObject portsObj = o.optJSONObject("effectivePorts");
        if (portsObj != null) {
            for (Object keyObj : portsObj.keySet()) {
                String key = String.valueOf(keyObj);
                ports.put(key, portsObj.optInt(key));
            }
        }
        Integer exitStatus = o.has("exitStatus") ? o.optInt("exitStatus") : null;
        return new ProcessOwnershipRecord(
                kind,
                o.optString("runId", null),
                o.optString("supervisorInstanceId", null),
                o.optLong("pid", -1),
                o.optLong("parentPid", -1),
                o.optString("executable", null),
                o.optString("commandLine", null),
                o.optString("workingDirectory", null),
                o.optString("runtimePath", null),
                o.optLong("startTimeMillis", -1),
                ports,
                exitStatus);
    }

    // ------------------------------------------------------------------
    // Ownership verification
    // ------------------------------------------------------------------

    /**
     * Verifies that the recorded process identity matches the live process
     * handle. A stale/recycled PID whose command line or start time does not
     * match the record is NOT a valid owner (D-18).
     */
    public static boolean verifyIdentity(ProcessOwnershipRecord record) {
        if (record == null || record.getPid() <= 0) {
            return false;
        }
        Optional<ProcessHandle> handleOpt = ProcessHandle.of(record.getPid());
        if (handleOpt.isEmpty()) {
            return false;
        }
        ProcessHandle handle = handleOpt.get();
        if (!handle.isAlive()) {
            return false;
        }
        ProcessHandle.Info info = handle.info();
        if (record.getExecutable() != null) {
            Optional<String> exe = info.command();
            if (exe.isPresent()) {
                String liveCmd = exe.get();
                if (!(liveCmd.contains(record.getExecutable())
                        || liveCmd.contains("java")
                        || record.getExecutable().contains("java"))) {
                    return false;
                }
            }
        }
        if (record.getCommandLine() != null) {
            Optional<String> cmd = info.commandLine();
            if (cmd.isPresent()) {
                String live = cmd.get();
                // The recorded command line must be a marker subset of the live
                // command line (jar path or supervisor marker).
                String marker = record.getCommandLine().contains("eu.kalafatic.evolution.supervisor.jar")
                        ? "eu.kalafatic.evolution.supervisor.jar" : "java";
                if (!live.contains(marker)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isOwnedByRun(ProcessOwnershipRecord record, SelfDevContext context) {
        return record != null && context != null
                && record.getRunId() != null
                && record.getRunId().equals(context.getRunId());
    }

    public static ProcessOwnershipRecord snapshot(ProcessKind kind, SelfDevContext context, Process process,
                                                  String executable, String commandLine, File workingDirectory,
                                                  File runtimePath, Map<String, Integer> effectivePorts) {
        long pid = -1;
        long parentPid = -1;
        if (process != null) {
            pid = process.pid();
            parentPid = ProcessHandle.current().pid();
        }
        return new ProcessOwnershipRecord(kind, context.getRunId(), context.getRunId() + "-" + kind.name(),
                pid, parentPid, executable, commandLine,
                workingDirectory != null ? workingDirectory.getAbsolutePath() : null,
                runtimePath != null ? runtimePath.getAbsolutePath() : null,
                System.currentTimeMillis(), effectivePorts, null);
    }

    public static void terminateOwnedProcessTree(ProcessOwnershipRecord record) {
        if (record == null || record.getPid() <= 0) {
            return;
        }
        Optional<ProcessHandle> handleOpt = ProcessHandle.of(record.getPid());
        if (handleOpt.isEmpty()) {
            return;
        }
        ProcessHandle handle = handleOpt.get();
        if (!handle.isAlive()) {
            return;
        }
        if (!verifyIdentity(record)) {
            System.err.println("[ProcessOwnership] Refusing to terminate PID " + record.getPid()
                    + ": identity does not match ownership record (stale or recycled PID).");
            return;
        }
        try {
            handle.descendants().forEach(ProcessHandle::destroyForcibly);
        } catch (Throwable ignored) {}
        handle.destroyForcibly();
    }

    public static List<Long> ownedPids(ProcessOwnershipRecord record) {
        if (record == null || record.getPid() <= 0) {
            return java.util.Collections.emptyList();
        }
        java.util.List<Long> pids = new java.util.ArrayList<>();
        pids.add(record.getPid());
        try {
            ProcessHandle.of(record.getPid()).ifPresent(h -> h.descendants().forEach(d -> pids.add(d.pid())));
        } catch (Throwable ignored) {}
        return pids;
    }
}
