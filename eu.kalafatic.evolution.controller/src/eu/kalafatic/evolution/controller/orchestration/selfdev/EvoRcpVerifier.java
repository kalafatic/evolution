package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class EvoRcpVerifier {

    public TaskResult verifyReady(Process process, File runtimeDir, long timeoutSeconds) {
        return verifyReady(process, runtimeDir, null, -1, timeoutSeconds);
    }

    public TaskResult verifyReady(Process process, File runtimeDir, File logDir, long timeoutSeconds) {
        return verifyReady(process, runtimeDir, logDir, -1, timeoutSeconds);
    }

    public TaskResult verifyReady(Process process, File runtimeDir, File logDir, int port, long timeoutSeconds) {
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

            if (port > 0 && isHttpServerHealthy(port)) {
                long duration = System.currentTimeMillis() - startTime;
                return new TaskResult.Builder("evo_verifier")
                        .status(TaskStatus.SUCCESS)
                        .message("EVO RCP product reached READY state via HTTP server health probe on port " + port)
                        .duration(duration)
                        .build();
            }

            File logFile = findLatestLogFile(runtimeDir, logDir);
            if (logFile != null && logFile.exists()) {
                if (containsReadyMarker(logFile)) {
                    long duration = System.currentTimeMillis() - startTime;
                    return new TaskResult.Builder("evo_verifier")
                            .status(TaskStatus.SUCCESS)
                            .message("EVO RCP product reached READY state via log marker.")
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

        return TaskResult.failure("evo_verifier", "Timed out waiting for EVO RCP process startup and health verification after " + timeoutSeconds + "s.", null);
    }

    private boolean isHttpServerHealthy(int port) {
        if (port <= 0) return false;
        try {
            java.net.URL url = new java.net.URL("http://127.0.0.1:" + port + "/server/status");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(800);
            conn.setReadTimeout(800);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private File findLatestLogFile(File runtimeDir, File logDir) {
        if (logDir != null && logDir.exists()) {
            File runtimeLog = new File(logDir, "evo_runtime.log");
            if (runtimeLog.exists() && runtimeLog.length() > 0) return runtimeLog;
        }
        if (runtimeDir == null || !runtimeDir.exists()) return null;
        File configuration = new File(runtimeDir, "configuration");
        if (configuration.exists()) {
            File[] logs = configuration.listFiles((dir, name) -> name.endsWith(".log"));
            if (logs != null && logs.length > 0) return logs[0];
        }
        File runtimeLog = new File(runtimeDir, "evo_runtime.log");
        if (runtimeLog.exists() && runtimeLog.length() > 0) return runtimeLog;
        return null;
    }

    private boolean containsReadyMarker(File logFile) {
        try {
            String content = java.nio.file.Files.readString(logFile.toPath());
            return content.contains("Application READY") ||
                   content.contains("EVO Framework initialized") ||
                   content.contains("Evolution background server started on port");
        } catch (Exception ignored) {
            return false;
        }
    }
}
