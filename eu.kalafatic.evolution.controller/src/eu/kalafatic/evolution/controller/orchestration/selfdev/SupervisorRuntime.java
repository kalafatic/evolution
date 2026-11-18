package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.kalafatic.evolution.controller.resource.EvoService;

public class SupervisorRuntime implements ProcessLifecycle {

    private static volatile Process supervisorProcess;
    private static volatile Process uiRcpSupervisorProcess;
    private final SupervisorClient client = new SupervisorClient();

    // WP5: processes launched by this JVM, terminated on JVM shutdown so the
    // parent never leaves unmanaged children behind (best-effort; identity-verified).
    private static final java.util.Set<ProcessOwnershipRecord> LAUNCHED_OWNERS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static volatile boolean shutdownHookInstalled = false;

    private static synchronized void installShutdownHook() {
        if (shutdownHookInstalled) {
            return;
        }
        shutdownHookInstalled = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (ProcessOwnershipRecord record : LAUNCHED_OWNERS) {
                try {
                    ProcessOwnershipRecord.terminateOwnedProcessTree(record);
                } catch (Throwable ignored) {}
            }
        }, "selfdev-owned-processes-cleanup"));
    }

    @Override
    public TaskResult start(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("start_supervisor", "SelfDevContext is null", null);
        }

        SupervisorClient activeClient = new SupervisorClient(context);
        int supervisorPort = context.getEffectiveSupervisorPort();
        int controlPort = context.getEffectiveSupervisorControlPort();

        // WP5 (D-01/D-17): the fast path is accepted ONLY when the current run's
        // ownership record exists, the PID identity matches, and the endpoint
        // responds. A foreign/stale supervisor is never adopted.
        ProcessOwnershipRecord existingOwnership =
                ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR);
        if (existingOwnership != null
                && ProcessOwnershipRecord.isOwnedByRun(existingOwnership, context)
                && ProcessOwnershipRecord.verifyIdentity(existingOwnership)
                && activeClient.ping()) {
            Integer recordedPort = existingOwnership.getEffectivePorts().get("supervisor");
            if (recordedPort != null && recordedPort.intValue() == supervisorPort) {
                return new TaskResult.Builder("start_supervisor")
                        .status(TaskStatus.SUCCESS)
                        .message("Supervisor for run " + context.getRunId() + " is already running and responding (PID " + existingOwnership.getPid() + ").")
                        .build();
            }
            System.err.println("[START_SUPERVISOR] Ownership record for run " + context.getRunId()
                    + " exists but effective ports differ (recorded=" + recordedPort + ", expected=" + supervisorPort + "). Not adopting.");
        }

        if (supervisorProcess != null) {
            killProcessTree(supervisorProcess);
            supervisorProcess = null;
        }
        if (uiRcpSupervisorProcess != null) {
            killProcessTree(uiRcpSupervisorProcess);
            uiRcpSupervisorProcess = null;
        }

        File jarFile = findSupervisorJar(context);
        if (jarFile == null || !jarFile.exists() || jarFile.length() == 0) {
            return TaskResult.failure("start_supervisor", "Supervisor JAR artifact pre-condition check failed: file missing or empty at " + (jarFile != null ? jarFile.getAbsolutePath() : "null"), null);
        }

        // Prepare OS network / firewall readiness before process launch
        eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessResult netResult =
                eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessManager.getInstance()
                        .prepareNetworkAccess(jarFile, supervisorPort, context);
        if (!netResult.isSuccess()) {
            log("[START_EVO_SUPERVISOR][WARNING] Network access preparation returned non-success: " + netResult.getMessage());
        }

        // Process 1: Simple Java App Headless Supervisor
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if (context.isDebugMode()) {
            cmd.add("-Devo.mode=debug");
            cmd.add("-Ddebug=true");
        }
        cmd.add("-Dport=" + supervisorPort);
        cmd.add("-Dcontrol.port=" + controlPort);
        // WP7: explicit child EVO HTTP port contract - the supervisor must use
        // this port for the EVO child instead of deriving offsets itself (D-26).
        cmd.add("-Devo.http.port=" + context.getEffectiveServerPort());
        cmd.add("-jar");
        cmd.add(jarFile.getAbsolutePath());
        cmd.add(context.getProjectRoot().getAbsolutePath());
        cmd.add("--port=" + supervisorPort);
        cmd.add("--control-port=" + controlPort);
        cmd.add("--headless");
        if (context.isDebugMode()) {
            cmd.add("--debug");
        }

        File logFile = new File(context.getLogDirectory(), "supervisor_runtime.log");

        log("[START_EVO_SUPERVISOR][LAUNCH_HEADLESS]");
        log("artifact=" + jarFile.getAbsolutePath());
        log("runtimeRoot=" + context.getRuntimeDirectory().getAbsolutePath());
        log("executable=java");
        log("workingDirectory=" + jarFile.getParentFile().getAbsolutePath());
        log("effectiveSupervisorPort=" + supervisorPort);
        log("command=" + String.join(" ", cmd));

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(jarFile.getParentFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

            supervisorProcess = pb.start();
            long pid = supervisorProcess.pid();
            context.setSupervisorPid(pid);
            context.setSupervisorExecutable(jarFile.getAbsolutePath());
            context.setSupervisorWorkingDirectory(jarFile.getParentFile());

            // WP5: persist the run-scoped ownership record (PID, parent PID,
            // command line, workdir, runtime path, start time, effective ports).
            java.util.Map<String, Integer> ports = new java.util.LinkedHashMap<>();
            ports.put("supervisor", supervisorPort);
            ports.put("control", controlPort);
            ProcessOwnershipRecord ownership = ProcessOwnershipRecord.snapshot(
                    ProcessOwnershipRecord.ProcessKind.SUPERVISOR, context, supervisorProcess,
                    "java", String.join(" ", cmd), jarFile.getParentFile(),
                    new File(context.getRuntimeDirectory(), "supervisor"), ports);
            ProcessOwnershipRecord.write(context, ownership);
            LAUNCHED_OWNERS.add(ownership);
            installShutdownHook();

            log("[START_EVO_SUPERVISOR][PROCESS_STARTED_HEADLESS]");
            log("pid=" + pid);

            // In Self-Dev Debug Mode, start BOTH Simple Java App Supervisor and UI RCP Supervisor
            if (context.isDebugMode()) {
                int uiSupervisorPort = supervisorPort + 1;
                int uiControlPort = controlPort + 1;

                List<String> cmdUi = new ArrayList<>();
                cmdUi.add("java");
                cmdUi.add("-Devo.mode=debug");
                cmdUi.add("-Ddebug=true");
                cmdUi.add("-Devo.supervisor.mode=ui");
                cmdUi.add("-Dport=" + uiSupervisorPort);
                cmdUi.add("-Dcontrol.port=" + uiControlPort);
                cmdUi.add("-jar");
                cmdUi.add(jarFile.getAbsolutePath());
                cmdUi.add(context.getProjectRoot().getAbsolutePath());
                cmdUi.add("--port=" + uiSupervisorPort);
                cmdUi.add("--control-port=" + uiControlPort);
                cmdUi.add("--debug");
                cmdUi.add("--ui");

                File uiLogFile = new File(context.getLogDirectory(), "supervisor_ui_runtime.log");

                log("[START_EVO_SUPERVISOR][LAUNCH_UI_RCP]");
                log("effectiveUiPort=" + uiSupervisorPort);
                log("uiCommand=" + String.join(" ", cmdUi));

                ProcessBuilder pbUi = new ProcessBuilder(cmdUi);
                pbUi.directory(jarFile.getParentFile());
                pbUi.redirectErrorStream(true);
                pbUi.redirectOutput(ProcessBuilder.Redirect.appendTo(uiLogFile));

                uiRcpSupervisorProcess = pbUi.start();
                long uiPid = uiRcpSupervisorProcess.pid();

                log("[START_EVO_SUPERVISOR][PROCESS_STARTED_UI_RCP]");
                log("uiPid=" + uiPid);
            }

            TaskResult readyRes = waitUntilReady(context, 15);
            long duration = System.currentTimeMillis() - startTime;

            if (readyRes.isSuccess()) {
                String serviceUrl = activeClient.getBaseUrl();
                String msg = "Supervisor started successfully and responding on " + serviceUrl;
                if (context.isDebugMode()) {
                    msg += " (Started both Headless PID " + pid + " and UI RCP PID " + (uiRcpSupervisorProcess != null ? uiRcpSupervisorProcess.pid() : "N/A") + ")";
                }
                return new TaskResult.Builder("start_supervisor")
                        .status(TaskStatus.SUCCESS)
                        .message(msg)
                        .duration(duration)
                        .command(String.join(" ", cmd))
                        .workingDirectory(jarFile.getParentFile())
                        .logFile(logFile)
                        .build();
            } else {
                int exitCode = -1;
                try {
                    if (supervisorProcess != null && !supervisorProcess.isAlive()) {
                        exitCode = supervisorProcess.exitValue();
                    }
                } catch (Throwable ignored) {}

                String logSnippet = getRecentLogSnippet(logFile);
                killProcessTree(supervisorProcess);
                supervisorProcess = null;
                if (uiRcpSupervisorProcess != null) {
                    killProcessTree(uiRcpSupervisorProcess);
                    uiRcpSupervisorProcess = null;
                }
                // WP5: persist the failed ownership outcome so no stale record
                // remains for the next run.
                ProcessOwnershipRecord failedOwnership =
                        ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR);
                if (failedOwnership != null) {
                    LAUNCHED_OWNERS.remove(failedOwnership);
                    ProcessOwnershipRecord.clear(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR, exitCode);
                }
                context.setSupervisorPid(-1);

                String failMsg = "Supervisor process started but failed ping check: " + readyRes.getMessage();
                if (exitCode != -1) {
                    failMsg += " (Process exited with code " + exitCode + ")";
                }

                return new TaskResult.Builder("start_supervisor")
                        .status(TaskStatus.FAILED)
                        .message(failMsg)
                        .duration(duration)
                        .command(String.join(" ", cmd))
                        .workingDirectory(jarFile.getParentFile())
                        .logFile(logFile)
                        .exitCode(exitCode)
                        .diagnostic("logSnippet", logSnippet)
                        .build();
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String logSnippet = getRecentLogSnippet(logFile);
            killProcessTree(supervisorProcess);
            supervisorProcess = null;
            if (uiRcpSupervisorProcess != null) {
                killProcessTree(uiRcpSupervisorProcess);
                uiRcpSupervisorProcess = null;
            }
            ProcessOwnershipRecord failedOwnership =
                    ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR);
            if (failedOwnership != null) {
                LAUNCHED_OWNERS.remove(failedOwnership);
                ProcessOwnershipRecord.clear(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR, -1);
            }
            context.setSupervisorPid(-1);
            return new TaskResult.Builder("start_supervisor")
                    .status(TaskStatus.FAILED)
                    .message("Failed to start Supervisor process: " + e.getMessage())
                    .duration(duration)
                    .command(String.join(" ", cmd))
                    .workingDirectory(jarFile.getParentFile())
                    .logFile(logFile)
                    .diagnostic("logSnippet", logSnippet)
                    .error(e)
                    .build();
        }
    }

    @Override
    public boolean isAlive() {
        return (supervisorProcess != null && supervisorProcess.isAlive())
            || (uiRcpSupervisorProcess != null && uiRcpSupervisorProcess.isAlive());
    }

    @Override
    public TaskResult waitUntilReady(SelfDevContext context, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long deadline = startTime + (timeoutSeconds * 1000);
        SupervisorClient activeClient = new SupervisorClient(context);

        while (System.currentTimeMillis() < deadline) {
            if (activeClient.ping()) {
                long duration = System.currentTimeMillis() - startTime;
                return new TaskResult.Builder("supervisor_ready")
                        .status(TaskStatus.SUCCESS)
                        .message("Supervisor HTTP endpoint is responsive on " + activeClient.getBaseUrl() + ".")
                        .duration(duration)
                        .build();
            }

            if (supervisorProcess != null && !supervisorProcess.isAlive()) {
                int exitVal = -1;
                try { exitVal = supervisorProcess.exitValue(); } catch (Throwable ignored) {}
                return new TaskResult.Builder("supervisor_ready")
                        .status(TaskStatus.FAILED)
                        .message("Supervisor process terminated unexpectedly with exit code " + exitVal)
                        .exitCode(exitVal)
                        .build();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TaskResult.failure("supervisor_ready", "Interrupted waiting for Supervisor readiness.", e);
            }
        }

        return TaskResult.failure("supervisor_ready", "Timed out waiting for Supervisor HTTP ping after " + timeoutSeconds + "s.", null);
    }

    @Override
    public TaskResult stop(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        SupervisorClient activeClient = context != null ? new SupervisorClient(context) : null;
        long targetPid = context != null ? context.getSupervisorPid() : -1;

        // WP5 (D-16/D-18): STOP operates only on the current run's ownership
        // record with verified PID identity. No handle and no record => nothing
        // owned by this run to stop (honest no-op).
        ProcessOwnershipRecord ownership =
                context != null ? ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR) : null;

        if (supervisorProcess == null && uiRcpSupervisorProcess == null && targetPid <= 0 && ownership == null) {
            return new TaskResult.Builder("stop_supervisor")
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor is not running or not owned by current run.")
                    .build();
        }

        try {
            Integer exitStatus = null;
            boolean refused = false;

            // 1. Graceful shutdown via HTTP when the owned endpoint is responsive.
            if (activeClient != null && activeClient.ping()) {
                try {
                    activeClient.sendCommand("shutdown", null);
                    Thread.sleep(800);
                } catch (Exception ignored) {}
            }

            // 2. Verify identity before force termination; never kill an
            // unrelated process that reused the recorded PID (D-18).
            boolean stopped = false;
            if (ownership != null && ProcessOwnershipRecord.verifyIdentity(ownership)) {
                java.util.Optional<ProcessHandle> ph = ProcessHandle.of(ownership.getPid());
                if (ph.isPresent() && ph.get().isAlive()) {
                    try {
                        ph.get().descendants().forEach(ProcessHandle::destroyForcibly);
                    } catch (Throwable ignored) {}
                    ph.get().destroyForcibly();
                    try {
                        ph.get().onExit().get(5, java.util.concurrent.TimeUnit.SECONDS);
                        stopped = !ph.get().isAlive();
                    } catch (Throwable ignored) {
                        stopped = !ph.get().isAlive();
                    }
                    exitStatus = stopped ? 0 : -1;
                } else {
                    stopped = true; // already gone
                }
            } else if (ownership != null && ownership.getPid() > 0 && isPidAlive(ownership.getPid())) {
                refused = true;
                System.err.println("[STOP_SUPERVISOR] Refusing to terminate live PID " + ownership.getPid()
                        + ": identity does not match the ownership record (stale or recycled PID).");
            }

            // 3. Fall back to same-JVM handles (they were launched by this JVM).
            if (supervisorProcess != null) {
                if (supervisorProcess.isAlive()) {
                    killProcessTree(supervisorProcess);
                }
                stopped = true;
                supervisorProcess = null;
            }
            if (uiRcpSupervisorProcess != null) {
                if (uiRcpSupervisorProcess.isAlive()) {
                    killProcessTree(uiRcpSupervisorProcess);
                }
                uiRcpSupervisorProcess = null;
            }

            // 4. Persist shutdown result and clear run-scoped state.
            if (ownership != null && exitStatus == null) {
                exitStatus = 0;
            }
            if (context != null && ownership != null) {
                ProcessOwnershipRecord.clear(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR, exitStatus);
                LAUNCHED_OWNERS.remove(ownership);
            }
            if (context != null) {
                context.setSupervisorPid(-1);
            }

            long duration = System.currentTimeMillis() - startTime;
            if (refused) {
                return TaskResult.failure("stop_supervisor",
                        "Supervisor stop refused: ownership identity verification failed for live PID.", null);
            }
            if (stopped || exitStatus != null) {
                return new TaskResult.Builder("stop_supervisor")
                        .status(TaskStatus.SUCCESS)
                        .message("Supervisor process stopped (graceful-first, identity-verified).")
                        .duration(duration)
                        .exitCode(exitStatus != null ? exitStatus : 0)
                        .build();
            }
            return TaskResult.failure("stop_supervisor",
                    "Supervisor stop incomplete: owned process could not be verified/terminated.", null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure("stop_supervisor", "Failed to stop Supervisor process: " + e.getMessage(), e);
        }
    }

    private static boolean isPidAlive(long pid) {
        if (pid <= 0) {
            return false;
        }
        try {
            return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        } catch (Throwable t) {
            return false;
        }
    }

    private void log(String msg) {
        eu.kalafatic.evolution.controller.log.Log.log(msg);
        System.out.println(msg);
    }

    private static void killProcessTree(Process p) {
        if (p == null) return;
        try {
            p.descendants().forEach(ph -> {
                try { ph.destroyForcibly(); } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}
        try {
            p.destroyForcibly();
            p.waitFor(3, TimeUnit.SECONDS);
        } catch (Throwable ignored) {}
    }

    private String getRecentLogSnippet(File logFile) {
        if (logFile == null || !logFile.exists() || logFile.length() == 0) {
            return "Log file empty or not created.";
        }
        try {
            List<String> lines = java.nio.file.Files.readAllLines(logFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            int total = lines.size();
            int start = Math.max(0, total - 50);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < total; i++) {
                sb.append(lines.get(i)).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Failed to read log file: " + e.getMessage();
        }
    }

    @Override
    public long getPid() {
        if (supervisorProcess != null && supervisorProcess.isAlive()) {
            try {
                return supervisorProcess.pid();
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    public static Process getSupervisorProcess() {
        return supervisorProcess;
    }

    public static Process getUiRcpSupervisorProcess() {
        return uiRcpSupervisorProcess;
    }

    public SupervisorClient getClient() {
        return client;
    }

    private File findSupervisorJar(SelfDevContext context) {
        File runtimeSupervisorDir = new File(context.getRuntimeDirectory(), "supervisor");
        File jarInRuntime = new File(runtimeSupervisorDir, "eu.kalafatic.evolution.supervisor.jar");
        if (jarInRuntime.exists()) return jarInRuntime;

        BuildArtifact artifact = context.getArtifact(ArtifactType.SUPERVISOR);
        if (artifact != null && artifact.getPath() != null && artifact.getPath().exists()) return artifact.getPath();

        // Run-scoped export artifact only. Reading build output from the canonical
        // repository target/ directory is forbidden (run isolation, defect D-38).
        File exportJar = new File(context.getExportDirectory(), "eu.kalafatic.evolution.supervisor.jar");
        if (exportJar.exists()) return exportJar;

        return null;
    }
}
