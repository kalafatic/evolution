package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EvoRcpRuntime implements ProcessLifecycle {

    private volatile Process evoProcess;
    private final EvoRcpVerifier verifier = new EvoRcpVerifier();

    @Override
    public TaskResult start(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("start_evo_rcp", "SelfDevContext is null", null);
        }

        if (isAlive()) {
            return new TaskResult.Builder("start_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process is already running (PID: " + getPid() + ")")
                    .build();
        }

        // WP5/WP6 (D-23): when this JVM has no live handle, adopt a running child
        // ONLY via the current run's ownership record with verified identity,
        // matching effective port AND verified readiness.
        ProcessOwnershipRecord priorOwnership =
                ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);
        if (priorOwnership != null
                && ProcessOwnershipRecord.isOwnedByRun(priorOwnership, context)
                && ProcessOwnershipRecord.verifyIdentity(priorOwnership)
                && priorOwnership.getEffectivePorts().containsKey("server")) {
            Integer recordedPort = priorOwnership.getEffectivePorts().get("server");
            if (recordedPort != null && recordedPort.intValue() == context.getEffectiveServerPort()) {
                RcpReadinessLevel probed = verifier.probeReadiness(
                        new File(context.getRuntimeDirectory(), "evo"),
                        context.getLogDirectory(),
                        context.getEffectiveServerPort(),
                        15);
                if (probed != null && probed.atLeast(RcpReadinessLevel.APPLICATION_INITIALIZED)) {
                    return new TaskResult.Builder("start_evo_rcp")
                            .status(TaskStatus.SUCCESS)
                            .message("EVO RCP process for run " + context.getRunId() + " is already running and ready (PID "
                                    + priorOwnership.getPid() + ", readiness " + probed + ").")
                            .diagnostic("readinessLevel", probed.name())
                            .build();
                }
                return TaskResult.failure("start_evo_rcp",
                        "Owned EVO process for run " + context.getRunId() + " exists (PID " + priorOwnership.getPid()
                        + ") but did not reach required readiness (highest verified: "
                        + (probed != null ? probed.name() : "NONE") + "). Run STOP_EVO first.", null);
            }
        }

        File evoRuntimeDir = new File(context.getRuntimeDirectory(), "evo");
        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);

        // WP4 (D-10): START_EVO must consume the exact recorded artifact from the
        // per-run manifest store. Arbitrary ZIP scans are forbidden; a stale or
        // checksum-mismatched record is a hard failure.
        ArtifactManifestStore manifestStore = context.getArtifactManifestStore();
        try {
            BuildArtifact manifestArtifact = manifestStore.resolve(ArtifactType.EVO_RCP);
            if (manifestArtifact != null) {
                artifact = manifestArtifact;
            } else if (artifact != null && artifact.getPath() != null && artifact.getPath().exists()) {
                // In-memory artifact without a manifest entry: verify it came from
                // the current run's export directory before use.
                File exportDir = context.getExportDirectory();
                File artifactPath = artifact.getPath().getAbsoluteFile();
                boolean inExport = exportDir != null && artifactPath.getAbsolutePath().startsWith(exportDir.getAbsolutePath());
                boolean inRuntime = artifactPath.getAbsolutePath().startsWith(context.getRuntimeDirectory().getAbsolutePath());
                if (!inExport && !inRuntime) {
                    System.err.println("[start_evo_rcp] Rejecting artifact outside run export/runtime dirs: " + artifactPath);
                    artifact = null;
                }
            }
        } catch (java.io.IOException manifestError) {
            return TaskResult.failure("start_evo_rcp", "Artifact manifest validation failed: " + manifestError.getMessage(), manifestError);
        }

        File executable = null;
        if (artifact != null && artifact.getPath() != null && artifact.getPath().exists()) {
            // Guarantee artifact lineage: deploy exact exported artifact produced by EXPORT_EVO
            EvoRcpDeployer deployer = new EvoRcpDeployer();
            TaskResult deployResult = deployer.deploy(context, artifact);
            if (!deployResult.isSuccess()) {
                return TaskResult.failure("start_evo_rcp", "Failed to deploy exported EVO RCP product artifact: " + deployResult.getMessage(), deployResult.getError());
            }
            executable = findExecutable(evoRuntimeDir);
        } else {
            // Fallback to pre-existing runtime executable if no artifact was recorded
            executable = findExecutable(evoRuntimeDir);
        }

        if (executable == null && context.getExportDirectory() != null && context.getExportDirectory().exists()) {
            executable = findExecutable(context.getExportDirectory());
        }

        if (executable == null) {
            return TaskResult.failure("start_evo_rcp", "Could not locate EVO RCP executable in " + evoRuntimeDir.getAbsolutePath(), null);
        }

        EvoRcpDeployer deployer = new EvoRcpDeployer();
        TaskResult layoutVal = deployer.validateDeployment(context, executable.getParentFile());
        if (!layoutVal.isSuccess()) {
            return TaskResult.failure("start_evo_rcp", "Invalid or incomplete exported product layout at " + executable.getParentFile().getAbsolutePath() + ": " + layoutVal.getMessage(), layoutVal.getError());
        }

        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            executable.setExecutable(true);
        }

        int effectiveServerPort = context.getEffectiveServerPort();

        // WP6/WP7: an occupied child port must prevent an unsafe direct launch.
        // The check is performed immediately before launch and reports a conflict.
        if (!isPortAvailable(effectiveServerPort)) {
            return TaskResult.failure("start_evo_rcp",
                    "Port conflict: effective server port " + effectiveServerPort
                    + " is already in use. Refusing to launch a duplicate EVO child (run " + context.getRunId() + ").", null);
        }

        // Prepare OS network / firewall access before process launch
        eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessResult netResult =
                eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessManager.getInstance()
                        .prepareNetworkAccess(executable, effectiveServerPort, context);
        if (!netResult.isSuccess()) {
            if (netResult.isRequiresElevation()) {
                return TaskResult.failure("start_evo_rcp",
                        "EVO RCP network access preparation failed (requires administrator elevation to create firewall rule): " + netResult.getMessage(),
                        netResult.getError());
            } else {
                return TaskResult.failure("start_evo_rcp",
                        "EVO RCP network access preparation failed: " + netResult.getMessage(),
                        netResult.getError());
            }
        }

        List<String> command = new ArrayList<>();
        command.add(executable.getAbsolutePath());
        command.add("--port=" + effectiveServerPort);
        command.add("--mode=SELF_DEV");
        File runtimeWs = new File(context.getRuntimeDirectory(), "workspace");
        if (!runtimeWs.exists()) {
            runtimeWs.mkdirs();
        }
        command.add("-data");
        command.add(runtimeWs.getAbsolutePath());
        if (context.isDebugMode()) {
            command.add("-debug");
            command.add("-consoleLog");
        }

        File logDir = context.getLogDirectory();
        if (!logDir.exists()) logDir.mkdirs();

        File startCmdFile = new File(logDir, "start-command.txt");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(startCmdFile, false))) {
            pw.println("executable=" + executable.getAbsolutePath());
            pw.println("arguments=" + command);
            pw.println("workingDirectory=" + executable.getParentFile().getAbsolutePath());
            pw.println("javaHome=" + System.getProperty("java.home"));
        } catch (Exception ignored) {}

        String cmdStr = String.join(" ", command);
        System.out.println("[RCP][START][COMMAND]\nexecutable=" + executable.getAbsolutePath() +
                "\narguments=" + command +
                "\nworkingDirectory=" + executable.getParentFile().getAbsolutePath() +
                "\njavaHome=" + System.getProperty("java.home"));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(executable.getParentFile());
            // Pass the run-scoped state directory explicitly so the child writes readiness
            // markers into the run directory instead of guessing a legacy CWD-relative location.
            File runDir = context.getRuntimeDirectory() != null ? context.getRuntimeDirectory().getParentFile() : null;
            if (runDir != null) {
                pb.environment().put("EVO_SELFDEV_STATE_DIR", runDir.getAbsolutePath());
            }

            File logFile = new File(logDir, "evo_runtime.log");
            File stdoutFile = new File(logDir, "start.stdout.log");
            File stderrFile = new File(logDir, "start.stderr.log");

            evoProcess = pb.start();
            long pid = getPid();

            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(startCmdFile, true))) {
                pw.println("processId=" + pid);
            } catch (Exception ignored) {}

            Thread stdoutThread = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(evoProcess.getInputStream()));
                     java.io.PrintWriter stdoutWriter = new java.io.PrintWriter(new java.io.FileWriter(stdoutFile, true));
                     java.io.PrintWriter runtimeWriter = new java.io.PrintWriter(new java.io.FileWriter(logFile, true))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutWriter.println(line);
                        stdoutWriter.flush();
                        runtimeWriter.println("[STDOUT] " + line);
                        runtimeWriter.flush();
                        System.out.println("[RCP][STDOUT] " + line);
                    }
                } catch (Exception ignored) {}
            }, "RCP-Stdout-Reader");

            Thread stderrThread = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(evoProcess.getErrorStream()));
                     java.io.PrintWriter stderrWriter = new java.io.PrintWriter(new java.io.FileWriter(stderrFile, true));
                     java.io.PrintWriter runtimeWriter = new java.io.PrintWriter(new java.io.FileWriter(logFile, true))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrWriter.println(line);
                        stderrWriter.flush();
                        runtimeWriter.println("[STDERR] " + line);
                        runtimeWriter.flush();
                        System.err.println("[RCP][STDERR] " + line);
                    }
                } catch (Exception ignored) {}
            }, "RCP-Stderr-Reader");

            stdoutThread.setDaemon(true);
            stderrThread.setDaemon(true);
            stdoutThread.start();
            stderrThread.start();

            Thread.sleep(1500);
            if (!evoProcess.isAlive()) {
                int exitCode = evoProcess.exitValue();
                // WP5: persist the failed ownership outcome.
                ProcessOwnershipRecord failed =
                        ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);
                if (failed != null) {
                    ProcessOwnershipRecord.clear(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP, exitCode);
                }
                if (isSecurityBlockExitCode(exitCode)) {
                    long duration = System.currentTimeMillis() - startTime;
                    return buildSecurityBlockResult("start_evo_rcp", executable, "Process terminated immediately with exit code " + exitCode, null, exitCode, duration, logFile);
                }
                return TaskResult.failure("start_evo_rcp", "EVO RCP process exited immediately with exit code " + exitCode, null);
            }

            // WP5: persist the run-scoped EVO ownership record.
            java.util.Map<String, Integer> ports = new java.util.LinkedHashMap<>();
            ports.put("server", effectiveServerPort);
            ProcessOwnershipRecord ownership = ProcessOwnershipRecord.snapshot(
                    ProcessOwnershipRecord.ProcessKind.EVO_RCP, context, evoProcess,
                    executable.getAbsolutePath(), cmdStr, executable.getParentFile(),
                    new File(context.getRuntimeDirectory(), "evo"), ports);
            ProcessOwnershipRecord.write(context, ownership);

            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("start_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process launched (PID: " + pid + ")")
                    .duration(duration)
                    .command(cmdStr)
                    .workingDirectory(executable.getParentFile())
                    .logFile(logFile)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            if (isSecurityBlockException(e)) {
                return buildSecurityBlockResult("start_evo_rcp", executable, "Process creation failed", e, -1, duration, null);
            }
            return TaskResult.failure("start_evo_rcp", "Failed to launch EVO RCP process: " + e.getMessage(), e);
        }
    }

    public static boolean isPortAvailable(int port) {
        try (java.net.ServerSocket ss = new java.net.ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new java.net.InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSecurityBlockException(Throwable t) {
        if (t == null) return false;
        String msg = (t.getMessage() != null ? t.getMessage() : "").toLowerCase();
        Throwable cause = t.getCause();
        String causeMsg = (cause != null && cause.getMessage() != null ? cause.getMessage() : "").toLowerCase();
        String combined = msg + " " + causeMsg;

        return combined.contains("contains a virus") ||
               combined.contains("potentially unwanted software") ||
               combined.contains("0x800700e5") ||
               combined.contains("800700e5") ||
               combined.contains("225") ||
               combined.contains("operation did not complete successfully") ||
               combined.contains("blocked by security policy") ||
               combined.contains("antivirus") ||
               combined.contains("defender") ||
               (combined.contains("access is denied") && System.getProperty("os.name").toLowerCase().contains("win"));
    }

    private boolean isSecurityBlockExitCode(int exitCode) {
        return exitCode == 225 || exitCode == 0x800700e5 || exitCode == 5 || exitCode == -1073741819 || exitCode == 0xC0000005;
    }

    private TaskResult buildSecurityBlockResult(String taskId, File executable, String detail, Throwable cause, int exitCode, long duration, File logFile) {
        String exePath = executable != null ? executable.getAbsolutePath() : "unknown";
        long size = executable != null && executable.exists() ? executable.length() : -1;
        String hash = computeFileHash(executable);

        String message = "SECURITY BLOCK: Launch of executable '" + exePath + "' was blocked or terminated by Windows Defender or security software (" + detail + "). " +
                "Do not disable security software or bypass system controls. " +
                "To inspect this security event, check Windows Security -> Protection History ('start windowsdefender://threathistory') or administrative security event logs. " +
                "Permit execution only through normal authorized workflows after verification.";

        System.err.println("[SECURITY][BLOCK] " + message);

        TaskResult.Builder builder = new TaskResult.Builder(taskId)
                .status(TaskStatus.FAILED)
                .message(message)
                .duration(duration)
                .error(cause)
                .diagnostic("securityBlocked", "true")
                .diagnostic("executablePath", exePath)
                .diagnostic("fileSizeBytes", String.valueOf(size))
                .diagnostic("fileSha256", hash)
                .diagnostic("exitCode", String.valueOf(exitCode))
                .diagnostic("protectionHistoryUri", "windowsdefender://threathistory");

        if (logFile != null) {
            builder.logFile(logFile);
        }

        return builder.build();
    }

    private String computeFileHash(File file) {
        if (file == null || !file.exists()) return "N/A";
        try (java.io.InputStream is = new java.io.FileInputStream(file)) {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public boolean isAlive() {
        return evoProcess != null && evoProcess.isAlive();
    }

    @Override
    public TaskResult waitUntilReady(SelfDevContext context, long timeoutSeconds) {
        if (!isAlive()) {
            return TaskResult.failure("verify_evo_rcp", "EVO RCP process is not running", null);
        }
        File evoRuntimeDir = new File(context.getRuntimeDirectory(), "evo");
        File logDir = context != null ? context.getLogDirectory() : null;
        int port = context != null ? context.getEffectiveServerPort() : eu.kalafatic.evolution.controller.resource.PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort();
        return verifier.verifyReady(evoProcess, evoRuntimeDir, logDir, port, timeoutSeconds);
    }

    @Override
    public TaskResult stop(SelfDevContext context) {
        long startTime = System.currentTimeMillis();

        // WP5: ownership-record-first stop. When this JVM has no live handle for
        // the child, the current run's ownership record (identity-verified) is
        // the only acceptable termination target.
        ProcessOwnershipRecord ownership =
                context != null ? ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP) : null;

        if ((evoProcess == null || !evoProcess.isAlive()) && ownership == null) {
            return new TaskResult.Builder("stop_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process is not running.")
                    .build();
        }

        try {
            int exitStatus = 0;
            if (evoProcess != null && evoProcess.isAlive()) {
                evoProcess.destroy();
                boolean exited = evoProcess.waitFor(10, TimeUnit.SECONDS);
                if (!exited) {
                    evoProcess.destroyForcibly();
                    evoProcess.waitFor(5, TimeUnit.SECONDS);
                }
                evoProcess = null;
            } else if (ownership != null) {
                if (!ProcessOwnershipRecord.verifyIdentity(ownership)) {
                    System.err.println("[STOP_EVO] Refusing to terminate PID " + ownership.getPid()
                            + ": identity does not match the ownership record (stale or recycled PID).");
                    exitStatus = -1;
                } else {
                    ProcessOwnershipRecord.terminateOwnedProcessTree(ownership);
                    // Bounded wait for the owned process to exit.
                    try {
                        ProcessHandle.of(ownership.getPid()).ifPresent(h -> {
                            try {
                                h.onExit().get(5, TimeUnit.SECONDS);
                            } catch (Exception ignored) {}
                        });
                        if (ProcessHandle.of(ownership.getPid()).map(ProcessHandle::isAlive).orElse(false)) {
                            exitStatus = -1;
                        }
                    } catch (Exception e) {
                        exitStatus = -1;
                    }
                }
            }
            if (context != null && ownership != null) {
                ProcessOwnershipRecord.clear(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP, exitStatus);
            }

            long duration = System.currentTimeMillis() - startTime;
            if (exitStatus != 0) {
                return TaskResult.failure("stop_evo_rcp", "EVO RCP stop refused: ownership identity verification failed.", null);
            }
            return new TaskResult.Builder("stop_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process stopped successfully (identity-verified).")
                    .duration(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure("stop_evo_rcp", "Failed to stop EVO RCP process: " + e.getMessage(), e);
        }
    }

    @Override
    public long getPid() {
        if (evoProcess != null && evoProcess.isAlive()) {
            try {
                return evoProcess.pid();
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    private File findExecutable(File root) {
        if (root == null || !root.exists()) return null;
        File[] execs = root.listFiles((dir, name) -> name.equals("evo.exe") || name.equals("evo") || name.equals("eclipse.exe") || name.equals("eclipse"));
        if (execs != null && execs.length > 0) return execs[0];

        File[] subdirs = root.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File sub : subdirs) {
                File found = findExecutable(sub);
                if (found != null) return found;
            }
        }
        return null;
    }
}
