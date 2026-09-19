package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.kalafatic.evolution.controller.resource.EvoService;

public class SupervisorRuntime implements ProcessLifecycle {

    private static volatile Process supervisorProcess;
    private final SupervisorClient client = new SupervisorClient();

    @Override
    public TaskResult start(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("start_supervisor", "SelfDevContext is null", null);
        }

        SupervisorClient activeClient = new SupervisorClient(context);
        int supervisorPort = context.getEffectiveSupervisorPort();
        int controlPort = context.getEffectiveSupervisorControlPort();

        if (isAlive() && activeClient.ping()) {
            return new TaskResult.Builder("start_supervisor")
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor is already running and responding to ping on " + activeClient.getBaseUrl() + ".")
                    .build();
        }

        if (supervisorProcess != null) {
            killProcessTree(supervisorProcess);
            supervisorProcess = null;
        }

        File jarFile = findSupervisorJar(context);
        if (jarFile == null || !jarFile.exists() || jarFile.length() == 0) {
            return TaskResult.failure("start_supervisor", "Supervisor JAR artifact pre-condition check failed: file missing or empty at " + (jarFile != null ? jarFile.getAbsolutePath() : "null"), null);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if (context.isDebugMode()) {
            cmd.add("-Devo.mode=debug");
            cmd.add("-Ddebug=true");
        }
        cmd.add("-Dport=" + supervisorPort);
        cmd.add("-Dcontrol.port=" + controlPort);
        cmd.add("-jar");
        cmd.add(jarFile.getAbsolutePath());
        cmd.add(context.getProjectRoot().getAbsolutePath());
        cmd.add("--port=" + supervisorPort);
        cmd.add("--control-port=" + controlPort);
        if (context.isDebugMode()) {
            cmd.add("--debug");
        }

        File logFile = new File(context.getLogDirectory(), "supervisor_runtime.log");

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(jarFile.getParentFile());

            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

            supervisorProcess = pb.start();

            TaskResult readyRes = waitUntilReady(context, 15);
            long duration = System.currentTimeMillis() - startTime;

            if (readyRes.isSuccess()) {
                String serviceUrl = activeClient.getBaseUrl();
                return new TaskResult.Builder("start_supervisor")
                        .status(TaskStatus.SUCCESS)
                        .message("Supervisor started successfully and responding on " + serviceUrl)
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
        return supervisorProcess != null && supervisorProcess.isAlive();
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
        SupervisorClient activeClient = new SupervisorClient(context);
        if (supervisorProcess == null && !activeClient.ping()) {
            return new TaskResult.Builder("stop_supervisor")
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor is not running.")
                    .build();
        }

        try {
            if (activeClient.ping()) {
                activeClient.sendCommand("shutdown", null);
                Thread.sleep(1000);
            }

            if (supervisorProcess != null) {
                killProcessTree(supervisorProcess);
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
