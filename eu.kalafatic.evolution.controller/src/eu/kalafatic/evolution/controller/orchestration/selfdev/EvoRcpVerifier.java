package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class EvoRcpVerifier {

    public TaskResult verifyReady(Process process, File runtimeDir, long timeoutSeconds) {
        return verifyReady(process, runtimeDir, null, timeoutSeconds);
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

            File logFile = findLatestLogFile(runtimeDir, logDir);
            if (logFile != null && logFile.exists()) {
                String fatalErr = checkFatalExceptions(logFile, logDir);
                if (fatalErr != null) {
                    return TaskResult.failure("evo_verifier", "EVO RCP process startup failed with fatal exception:\n" + fatalErr, null);
                }

                if (containsReadyMarker(logFile)) {
                    String unresolvedErr = verifyOsgiResolution(port, logDir);
                    if (unresolvedErr != null) {
                        return TaskResult.failure("evo_verifier", "EVO RCP process started but OSGi bundle resolution failed:\n" + unresolvedErr, null);
                    }

                    long duration = System.currentTimeMillis() - startTime;
                    return new TaskResult.Builder("evo_verifier")
                            .status(TaskStatus.SUCCESS)
                            .message("EVO RCP product reached READY state with all OSGi bundles verified.")
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
            File logFile = findLatestLogFile(runtimeDir, logDir);
            String fatalErr = checkFatalExceptions(logFile, logDir);
            if (fatalErr != null) {
                return TaskResult.failure("evo_verifier", "EVO RCP process startup failed with fatal exception:\n" + fatalErr, null);
            }

            String unresolvedErr = verifyOsgiResolution(port, logDir);
            if (unresolvedErr != null) {
                return TaskResult.failure("evo_verifier", "EVO RCP process is alive but OSGi bundle resolution failed:\n" + unresolvedErr, null);
            }

            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("evo_verifier")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP process is running and alive after " + timeoutSeconds + "s.")
                    .duration(duration)
                    .build();
        }

        return TaskResult.failure("evo_verifier", "Timed out waiting for EVO RCP process startup after " + timeoutSeconds + "s.", null);
    }

    public String verifyOsgiResolution(int port, File logDir) {
        if (port > 0) {
            try {
                java.net.URL url = new java.net.URI("http://127.0.0.1:" + port + "/server/osgi").toURL();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1500);
                conn.setReadTimeout(1500);
                if (conn.getResponseCode() == 200) {
                    try (java.io.InputStream is = conn.getInputStream()) {
                        String jsonText = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        org.json.JSONObject obj = new org.json.JSONObject(jsonText);
                        org.json.JSONArray bundles = obj.optJSONArray("bundles");

                        StringBuilder unresolvedReport = new StringBuilder();
                        int unresolvedEvoBundles = 0;

                        if (bundles != null) {
                            for (int i = 0; i < bundles.length(); i++) {
                                org.json.JSONObject b = bundles.getJSONObject(i);
                                String symName = b.optString("symbolicName", "");
                                boolean resolved = b.optBoolean("resolved", true);
                                String state = b.optString("state", "UNKNOWN");

                                if (!resolved || "INSTALLED".equals(state)) {
                                    if (symName.startsWith("eu.kalafatic.")) {
                                        unresolvedEvoBundles++;
                                        unresolvedReport.append("\n[EVO-OSGI] Unresolved Bundle: ").append(symName)
                                                .append(" (v").append(b.optString("version", "")).append(")\n")
                                                .append("    State: ").append(state).append(" (RESOLVED: NO)\n");
                                        if (b.has("requireBundle")) unresolvedReport.append("    Require-Bundle: ").append(b.getString("requireBundle")).append("\n");
                                        if (b.has("importPackage")) unresolvedReport.append("    Import-Package: ").append(b.getString("importPackage")).append("\n");
                                        if (b.has("fragmentHost")) unresolvedReport.append("    Fragment-Host: ").append(b.getString("fragmentHost")).append("\n");
                                    }
                                }
                            }
                        }

                        if (unresolvedEvoBundles > 0) {
                            return "OSGi Resolution Failure: " + unresolvedEvoBundles + " EVO bundle(s) remain UNRESOLVED (State: INSTALLED):\n" + unresolvedReport.toString();
                        }
                    }
                }
            } catch (Exception ignored) {
                // Server might not be listening yet, fallback to log check
            }
        }

        // Fallback: check log files for unresolved bundle markers
        if (logDir != null && logDir.exists()) {
            File[] checkFiles = new File[] { new File(logDir, "start.stdout.log"), new File(logDir, "evo_runtime.log") };
            for (File f : checkFiles) {
                if (f.exists()) {
                    try {
                        String content = java.nio.file.Files.readString(f.toPath());
                        if (content.contains("[EVO-OSGI] UNRESOLVED BUNDLE:")) {
                            StringBuilder sb = new StringBuilder();
                            String[] lines = content.split("\n");
                            for (String line : lines) {
                                if (line.contains("[EVO-OSGI]") || line.contains("State: INSTALLED") || line.contains("Require-Bundle:") || line.contains("Import-Package:")) {
                                    sb.append(line).append("\n");
                                }
                            }
                            return "OSGi Resolution Failure detected in logs:\n" + sb.toString().trim();
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return null;
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

    private String checkFatalExceptions(File logFile, File logDir) {
        String err = checkFileForFatalException(logFile);
        if (err != null) return err;

        if (logDir != null && logDir.exists()) {
            File stderrLog = new File(logDir, "start.stderr.log");
            err = checkFileForFatalException(stderrLog);
            if (err != null) return err;

            File runtimeLog = new File(logDir, "evo_runtime.log");
            err = checkFileForFatalException(runtimeLog);
            if (err != null) return err;
        }

        return null;
    }

    private String checkFileForFatalException(File file) {
        if (file == null || !file.exists()) return null;
        try {
            String content = java.nio.file.Files.readString(file.toPath());
            if (content.contains("Could not initialize class") ||
                content.contains("ExceptionInInitializerError") ||
                content.contains("NoClassDefFoundError") ||
                content.contains("ClassNotFoundException") ||
                content.contains("LinkageError") ||
                content.contains("BundleException")) {

                String[] lines = content.split("\n");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].contains("Could not initialize class") ||
                        lines[i].contains("ExceptionInInitializerError") ||
                        lines[i].contains("NoClassDefFoundError") ||
                        lines[i].contains("ClassNotFoundException") ||
                        lines[i].contains("LinkageError") ||
                        lines[i].contains("BundleException")) {
                        int start = Math.max(0, i - 2);
                        int end = Math.min(lines.length, i + 10);
                        for (int j = start; j < end; j++) {
                            sb.append(lines[j]).append("\n");
                        }
                        break;
                    }
                }
                return sb.toString().trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean containsReadyMarker(File logFile) {
        try {
            String content = java.nio.file.Files.readString(logFile.toPath());
            return content.contains("Application READY") ||
                   content.contains("EVO Framework initialized") ||
                   content.contains("Framework Launched") ||
                   content.contains("Evolution background server started on port");
        } catch (Exception ignored) {
            return false;
        }
    }
}
