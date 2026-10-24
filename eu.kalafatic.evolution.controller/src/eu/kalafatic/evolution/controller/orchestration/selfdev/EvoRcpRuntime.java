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

        File evoRuntimeDir = new File(context.getRuntimeDirectory(), "evo");
        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);

        if (artifact == null && context.getExportDirectory() != null && context.getExportDirectory().exists()) {
            String reqProductId = "evolution";
            if (context.getResourceManager() != null && context.getResourceManager().getProductDefinition() != null) {
                reqProductId = context.getResourceManager().getProductDefinition().getProductId().toLowerCase();
            }
            final String filterProductId = reqProductId;
            File[] zips = context.getExportDirectory().listFiles((dir, name) -> {
                String lname = name.toLowerCase();
                return (lname.endsWith(".zip") || lname.endsWith(".tar.gz")) &&
                       (lname.startsWith(filterProductId + "-") || lname.startsWith(filterProductId + "_") || lname.equals(filterProductId + ".zip"));
            });
            if (zips != null && zips.length > 0) {
                artifact = new BuildArtifact(ArtifactType.EVO_RCP, zips[0], context.getSourceRevision(), null, null);
                context.recordArtifact(artifact);
            }
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
                if (isSecurityBlockExitCode(exitCode)) {
                    long duration = System.currentTimeMillis() - startTime;
                    return buildSecurityBlockResult("start_evo_rcp", executable, "Process terminated immediately with exit code " + exitCode, null, exitCode, duration, logFile);
                }
                return TaskResult.failure("start_evo_rcp", "EVO RCP process exited immediately with exit code " + exitCode, null);
            }

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
        int port = context != null ? context.getEffectiveServerPort() : 48080;
        return verifier.verifyReady(evoProcess, evoRuntimeDir, logDir, port, timeoutSeconds);
    }

    @Override
    public TaskResult stop(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (evoProcess == null || !evoProcess.isAlive()) {
            return new TaskResult.Builder("stop_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process is not running.")
                    .build();
        }

        try {
            evoProcess.destroy();
            boolean exited = evoProcess.waitFor(10, TimeUnit.SECONDS);
            if (!exited) {
                evoProcess.destroyForcibly();
                evoProcess.waitFor(5, TimeUnit.SECONDS);
            }
            evoProcess = null;

            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("stop_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process stopped successfully.")
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
