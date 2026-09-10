package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class EvoRcpVerifier {

    public TaskResult verifyReady(Process process, File runtimeDir, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long deadline = startTime + (timeoutSeconds * 1000);

        if (process == null) {
            return TaskResult.failure("evo_verifier", "Process object is null.", null);
        }

        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                int exitCode = process.exitValue();
                return TaskResult.failure("evo_verifier", "EVO RCP process terminated unexpectedly during startup with exit code: " + exitCode, null);
            }

            File logFile = findLatestLogFile(runtimeDir);
            if (logFile != null && logFile.exists()) {
                if (containsReadyMarker(logFile)) {
                    long duration = System.currentTimeMillis() - startTime;
                    return new TaskResult.Builder("evo_verifier")
                            .status(TaskStatus.SUCCESS)
                            .message("EVO RCP product reached READY state.")
                            .duration(duration)
                            .logFile(logFile)
                            .build();
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TaskResult.failure("evo_verifier", "Interrupted waiting for EVO RCP to reach READY state.", e);
            }
        }

        if (process.isAlive()) {
            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("evo_verifier")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process is running and alive after " + timeoutSeconds + "s.")
                    .duration(duration)
                    .build();
        }

        return TaskResult.failure("evo_verifier", "Timed out waiting for EVO RCP process startup after " + timeoutSeconds + "s.", null);
    }

    private File findLatestLogFile(File runtimeDir) {
        if (runtimeDir == null || !runtimeDir.exists()) return null;
        File configuration = new File(runtimeDir, "configuration");
        if (configuration.exists()) {
            File[] logs = configuration.listFiles((dir, name) -> name.endsWith(".log"));
            if (logs != null && logs.length > 0) return logs[0];
        }
        return null;
    }

    private boolean containsReadyMarker(File logFile) {
        try {
            String content = java.nio.file.Files.readString(logFile.toPath());
            return content.contains("Application READY") || content.contains("EVO Framework initialized") || content.contains("Framework Launched");
        } catch (Exception ignored) {
            return false;
        }
    }
}
