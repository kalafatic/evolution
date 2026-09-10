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
        if (!evoRuntimeDir.exists()) {
            evoRuntimeDir = context.getExportDirectory();
        }

        File executable = findExecutable(evoRuntimeDir);
        if (executable == null) {
            return TaskResult.failure("start_evo_rcp", "Could not locate EVO RCP executable in " + evoRuntimeDir.getAbsolutePath(), null);
        }

        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            executable.setExecutable(true);
        }

        List<String> command = new ArrayList<>();
        command.add(executable.getAbsolutePath());
        if (context.isDebugMode()) {
            command.add("-debug");
            command.add("-consoleLog");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(executable.getParentFile());

            File logFile = new File(context.getLogDirectory(), "evo_runtime.log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

            evoProcess = pb.start();

            Thread.sleep(1500);
            if (!evoProcess.isAlive()) {
                int exitCode = evoProcess.exitValue();
                return TaskResult.failure("start_evo_rcp", "EVO RCP process exited immediately with exit code " + exitCode, null);
            }

            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("start_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process launched (PID: " + getPid() + ")")
                    .duration(duration)
                    .command(String.join(" ", command))
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
        return verifier.verifyReady(evoProcess, evoRuntimeDir, timeoutSeconds);
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
