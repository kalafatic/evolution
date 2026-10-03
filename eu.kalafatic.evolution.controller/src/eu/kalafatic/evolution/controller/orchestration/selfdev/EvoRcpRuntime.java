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
        File executable = findExecutable(evoRuntimeDir);

        if (executable == null) {
            BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);
            if (artifact == null && context.getExportDirectory() != null && context.getExportDirectory().exists()) {
                File[] zips = context.getExportDirectory().listFiles((dir, name) -> name.endsWith(".zip") || name.startsWith("evolution"));
                if (zips != null && zips.length > 0) {
                    artifact = new BuildArtifact(ArtifactType.EVO_RCP, zips[0], context.getSourceRevision(), null, null);
                }
            }
            if (artifact != null) {
                EvoRcpDeployer deployer = new EvoRcpDeployer();
                TaskResult deployResult = deployer.deploy(context, artifact);
                if (deployResult.isSuccess()) {
                    executable = findExecutable(evoRuntimeDir);
                }
            }
        }

        if (executable == null && context.getExportDirectory() != null && context.getExportDirectory().exists()) {
            executable = findExecutable(context.getExportDirectory());
        }

        if (executable == null) {
            return TaskResult.failure("start_evo_rcp", "Could not locate EVO RCP executable in " + evoRuntimeDir.getAbsolutePath(), null);
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
            return TaskResult.failure("start_evo_rcp", "Failed to launch EVO RCP process: " + e.getMessage(), e);
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
