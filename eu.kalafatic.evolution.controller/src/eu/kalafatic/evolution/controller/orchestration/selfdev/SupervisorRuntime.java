package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SupervisorRuntime implements ProcessLifecycle {

    private static volatile Process supervisorProcess;
    private final SupervisorClient client = new SupervisorClient();

    @Override
    public TaskResult start(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("start_supervisor", "SelfDevContext is null", null);
        }

        if (isAlive() && client.ping()) {
            return new TaskResult.Builder("start_supervisor")
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor is already running and responding to ping.")
                    .build();
        }

        if (supervisorProcess != null) {
            supervisorProcess.destroyForcibly();
            try {
                supervisorProcess.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            supervisorProcess = null;
        }

        File jarFile = findSupervisorJar(context);
        if (jarFile == null || !jarFile.exists()) {
            return TaskResult.failure("start_supervisor", "Supervisor JAR file not found.", null);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if (context.isDebugMode()) {
            cmd.add("-Devo.mode=debug");
            cmd.add("-Ddebug=true");
        }
        cmd.add("-jar");
        cmd.add(jarFile.getAbsolutePath());
        cmd.add(context.getProjectRoot().getAbsolutePath());
        if (context.isDebugMode()) {
            cmd.add("--debug");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(jarFile.getParentFile());

            File logFile = new File(context.getLogDirectory(), "supervisor_runtime.log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

            supervisorProcess = pb.start();

            TaskResult readyRes = waitUntilReady(context, 15);
            long duration = System.currentTimeMillis() - startTime;

            if (readyRes.isSuccess()) {
                return new TaskResult.Builder("start_supervisor")
                        .status(TaskStatus.SUCCESS)
                        .message("Supervisor started successfully and responding on http://127.0.0.1:8089")
                        .duration(duration)
                        .command(String.join(" ", cmd))
                        .workingDirectory(jarFile.getParentFile())
                        .logFile(logFile)
                        .build();
            } else {
                return TaskResult.failure("start_supervisor", "Supervisor started but failed ping check: " + readyRes.getMessage(), null);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure("start_supervisor", "Failed to start Supervisor process: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAlive() {
        return supervisorProcess != null && supervisorProcess.isAlive();
    }

    @Override
    public TaskResult waitUntilReady(SelfDevContext context, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long deadline = startTime + (timeoutSeconds * 1000);

        while (System.currentTimeMillis() < deadline) {
            if (client.ping()) {
                long duration = System.currentTimeMillis() - startTime;
                return new TaskResult.Builder("supervisor_ready")
                        .status(TaskStatus.SUCCESS)
                        .message("Supervisor HTTP endpoint is responsive.")
                        .duration(duration)
                        .build();
            }

            if (supervisorProcess != null && !supervisorProcess.isAlive()) {
                return TaskResult.failure("supervisor_ready", "Supervisor process terminated unexpectedly.", null);
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
        if (supervisorProcess == null && !client.ping()) {
            return new TaskResult.Builder("stop_supervisor")
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor is not running.")
                    .build();
        }

        try {
            if (client.ping()) {
                client.sendCommand("shutdown", null);
                Thread.sleep(1000);
            }

            if (supervisorProcess != null && supervisorProcess.isAlive()) {
                supervisorProcess.destroy();
                boolean exited = supervisorProcess.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    supervisorProcess.destroyForcibly();
                    supervisorProcess.waitFor(2, TimeUnit.SECONDS);
                }
            }
            supervisorProcess = null;

            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("stop_supervisor")
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor process stopped.")
                    .duration(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure("stop_supervisor", "Failed to stop Supervisor process: " + e.getMessage(), e);
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

    public SupervisorClient getClient() {
        return client;
    }

    private File findSupervisorJar(SelfDevContext context) {
        File runtimeSupervisorDir = new File(context.getRuntimeDirectory(), "supervisor");
        File jarInRuntime = new File(runtimeSupervisorDir, "eu.kalafatic.evolution.supervisor.jar");
        if (jarInRuntime.exists()) return jarInRuntime;

        BuildArtifact artifact = context.getArtifact(ArtifactType.SUPERVISOR);
        if (artifact != null && artifact.getPath().exists()) return artifact.getPath();

        File targetJar = new File(context.getProjectRoot(), "eu.kalafatic.evolution.supervisor/target/eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar");
        if (targetJar.exists()) return targetJar;

        File exportJar = new File(context.getExportDirectory(), "eu.kalafatic.evolution.supervisor.jar");
        if (exportJar.exists()) return exportJar;

        return null;
    }
}
