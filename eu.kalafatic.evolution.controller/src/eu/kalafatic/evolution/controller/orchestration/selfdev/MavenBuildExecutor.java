package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import eu.kalafatic.evolution.controller.log.Log;

public class MavenBuildExecutor {

    private final String taskId;
    private static final int MAX_RECOVERY_ATTEMPTS = 1;

    public MavenBuildExecutor(String taskId) {
        this.taskId = taskId;
    }

    public TaskResult executeBuild(File workingDir, List<String> goals, List<String> userArgs, File logFile, long timeoutMinutes) {
        return executeBuildInternal(workingDir, goals, userArgs, logFile, timeoutMinutes, 0);
    }

    private TaskResult executeBuildInternal(File workingDir, List<String> goals, List<String> userArgs, File logFile, long timeoutMinutes, int attempt) {
        long startTime = System.currentTimeMillis();
        if (workingDir == null || !workingDir.exists()) {
            return TaskResult.failure(taskId, "Working directory does not exist: " + (workingDir != null ? workingDir.getAbsolutePath() : "null"), null);
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String mavenExec = resolveMavenExecutable(workingDir);

        List<String> command = new ArrayList<>();
        if (isWindows && (mavenExec.toLowerCase().endsWith(".cmd") || mavenExec.toLowerCase().endsWith(".bat"))) {
            command.add("cmd.exe");
            command.add("/c");
        }
        command.add(mavenExec);

        if (goals != null && !goals.isEmpty()) {
            command.addAll(goals);
        }
        if (userArgs != null && !userArgs.isEmpty()) {
            command.addAll(userArgs);
        }

        // Add non-interactive batch mode and progress flags if missing
        if (!command.contains("-B") && !command.contains("--batch-mode")) {
            command.add("-B");
        }
        if (!command.contains("-ntp") && !command.contains("--no-transfer-progress")) {
            command.add("-ntp");
        }
        if (!command.contains("-Dstyle.color=never")) {
            command.add("-Dstyle.color=never");
        }

        String fullCommandStr = String.join(" ", command);
        String javaHome = System.getProperty("java.home");
        String startDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTime));

        log("[MAVEN] ========================================");
        log("[MAVEN] PROJECT       = " + taskId);
        log("[MAVEN] REPOSITORY    = " + workingDir.getAbsolutePath());
        log("[MAVEN] WORKDIR       = " + workingDir.getAbsolutePath());
        log("[MAVEN] JAVA          = " + (javaHome != null ? javaHome : "System default"));
        log("[MAVEN] MAVEN         = " + mavenExec);
        log("[MAVEN] COMMAND       = " + fullCommandStr);
        log("[MAVEN] START         = " + startDateStr);
        log("[MAVEN] ========================================");

        log("[MAVEN][PROCESS] creating");
        log("[MAVEN][PROCESS] executable=" + mavenExec);
        log("[MAVEN][PROCESS] arguments=" + command);
        log("[MAVEN][PROCESS] workingDirectory=" + workingDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);

        Map<String, String> env = pb.environment();
        if (javaHome != null && !javaHome.isEmpty()) {
            env.put("JAVA_HOME", javaHome);
        }

        StringBuilder outputBuffer = new StringBuilder();
        int exitCode = -1;

        try (PrintWriter logWriter = (logFile != null) ? new PrintWriter(new FileWriter(logFile, true)) : null) {
            log("[MAVEN][PROCESS] starting");
            Process process = pb.start();
            long pid = process.pid();
            log("[MAVEN][PROCESS] started pid=" + pid);

            // Close stdin immediately to prevent process blocking on unclosed input pipe
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {}

            // Concurrent stream handling for stdout and stderr
            Thread stdoutThread = new Thread(() -> {
                log("[MAVEN][PROCESS] stdout-reader started");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (outputBuffer) {
                            outputBuffer.append(line).append("\n");
                        }
                        if (logWriter != null) {
                            synchronized (logWriter) {
                                logWriter.println("[STDOUT] " + line);
                                logWriter.flush();
                            }
                        }
                        log("[MAVEN][STDOUT] " + line);
                    }
                } catch (Exception e) {
                    log("[MAVEN][STDOUT][ERROR] Exception reading stdout: " + e.getMessage());
                } finally {
                    log("[MAVEN][PROCESS] stdout-reader finished");
                }
            }, "Maven-Stdout-Reader-" + taskId);

            Thread stderrThread = new Thread(() -> {
                log("[MAVEN][PROCESS] stderr-reader started");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (outputBuffer) {
                            outputBuffer.append(line).append("\n");
                        }
                        if (logWriter != null) {
                            synchronized (logWriter) {
                                logWriter.println("[STDERR] " + line);
                                logWriter.flush();
                            }
                        }
                        log("[MAVEN][STDERR] " + line);
                    }
                } catch (Exception e) {
                    log("[MAVEN][STDERR][ERROR] Exception reading stderr: " + e.getMessage());
                } finally {
                    log("[MAVEN][PROCESS] stderr-reader finished");
                }
            }, "Maven-Stderr-Reader-" + taskId);

            stdoutThread.setDaemon(true);
            stderrThread.setDaemon(true);
            stdoutThread.start();
            stderrThread.start();

            log("[MAVEN][PROCESS] waiting");
            long timeout = timeoutMinutes > 0 ? timeoutMinutes : 30;
            boolean finished = process.waitFor(timeout, TimeUnit.MINUTES);

            if (!finished) {
                long duration = System.currentTimeMillis() - startTime;
                log("[MAVEN][ERROR] Build timed out after " + timeout + " minutes (PID: " + pid + ").");
                log("[MAVEN][PROCESS] Timeout reached. PID: " + pid + ", WorkingDir: " + workingDir.getAbsolutePath() + ", Cmd: " + fullCommandStr + ", Elapsed: " + duration + " ms");

                try {
                    process.descendants().forEach(ph -> {
                        try {
                            ph.destroyForcibly();
                        } catch (Exception ignored) {}
                    });
                } catch (Exception e) {
                    log("[MAVEN][PROCESS] Error killing process descendants: " + e.getMessage());
                }

                try {
                    process.destroy();
                    Thread.sleep(1000);
                } catch (Exception ignored) {}

                if (process.isAlive()) {
                    process.destroyForcibly();
                }

                try { stdoutThread.join(2000); } catch (Exception ignored) {}
                try { stderrThread.join(2000); } catch (Exception ignored) {}

                log("[MAVEN][PROCESS] completed (timed out)");
                log("[MAVEN][PROCESS] durationMs=" + duration);

                String currentOutput;
                synchronized (outputBuffer) {
                    currentOutput = outputBuffer.toString();
                }

                return new TaskResult.Builder(taskId)
                        .status(TaskStatus.FAILED)
                        .message("Maven execution timed out after " + timeout + " minutes (PID: " + pid + ").")
                        .command(fullCommandStr)
                        .workingDirectory(workingDir)
                        .duration(duration)
                        .logFile(logFile)
                        .diagnostic("errorCategory", MavenErrorCategory.TRANSIENT.name())
                        .diagnostic("pid", String.valueOf(pid))
                        .diagnostic("outputTail", getTail(currentOutput, 2000))
                        .build();
            }

            exitCode = process.exitValue();
            log("[MAVEN][PROCESS] exitCode=" + exitCode);

            try { stdoutThread.join(5000); } catch (Exception ignored) {}
            try { stderrThread.join(5000); } catch (Exception ignored) {}

            log("[MAVEN][PROCESS] completed");
            long duration = System.currentTimeMillis() - startTime;
            log("[MAVEN][PROCESS] durationMs=" + duration);

            String fullOutput;
            synchronized (outputBuffer) {
                fullOutput = outputBuffer.toString();
            }

            if (exitCode == 0) {
                log("[MAVEN] Build succeeded in " + duration + " ms.");
                return new TaskResult.Builder(taskId)
                        .status(TaskStatus.SUCCESS)
                        .message("Maven build succeeded.")
                        .command(fullCommandStr)
                        .workingDirectory(workingDir)
                        .exitCode(0)
                        .duration(duration)
                        .logFile(logFile)
                        .build();
            } else {
                MavenErrorClassifier.ClassificationResult classification = MavenErrorClassifier.classify(fullOutput, workingDir);

                log("[MAVEN][ERROR]");
                log("[MAVEN][ERROR] Exit code: " + exitCode);
                log("[MAVEN][ERROR] Failed phase: " + classification.getFailedPhase());
                log("[MAVEN][ERROR] Failing module: " + classification.getFailingModule());
                log("[MAVEN][ERROR] Failing plugin: " + classification.getFailingPlugin());
                log("[MAVEN][ERROR] Failing goal: " + classification.getFailingGoal());
                log("[MAVEN][ERROR] Repository: " + workingDir.getAbsolutePath());
                log("[MAVEN][ERROR] Working directory: " + workingDir.getAbsolutePath());
                log("[MAVEN][ERROR] Command: " + fullCommandStr);
                log("[MAVEN][ERROR] Error category: " + classification.getCategory());
                log("[MAVEN][ERROR] Root cause: " + classification.getRootCause());
                if (classification.getReportDirectory() != null) {
                    log("[MAVEN][ERROR] Report directory: " + classification.getReportDirectory());
                }
                if (classification.getTestSummary() != null && !classification.getTestSummary().isEmpty()) {
                    log("[MAVEN][ERROR] Test failure details:\n" + classification.getTestSummary());
                }
                log("[MAVEN][ERROR] Relevant Maven output excerpt:\n" + classification.getLastRelevantOutput());

                if (classification.getCategory().isRecoverable() && attempt < MAX_RECOVERY_ATTEMPTS) {
                    log("[MAVEN][RECOVERY]");
                    log("[MAVEN][RECOVERY] Detected recoverable error: " + classification.getCategory());
                    log("[MAVEN][RECOVERY] Action: retry build");
                    log("[MAVEN][RECOVERY] Attempt: " + (attempt + 1) + "/" + MAX_RECOVERY_ATTEMPTS);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                    return executeBuildInternal(workingDir, goals, userArgs, logFile, timeoutMinutes, attempt + 1);
                }

                if (attempt > 0) {
                    log("[MAVEN][FATAL]");
                    log("[MAVEN][FATAL] Build failed after recovery attempts.");
                }

                TaskResult.Builder resultBuilder = new TaskResult.Builder(taskId)
                        .status(TaskStatus.FAILED)
                        .message("Maven build failed with exit code " + exitCode + " [" + classification.getCategory() + "]: " + classification.getRootCause())
                        .command(fullCommandStr)
                        .workingDirectory(workingDir)
                        .exitCode(exitCode)
                        .duration(duration)
                        .logFile(logFile)
                        .diagnostic("errorCategory", classification.getCategory().name())
                        .diagnostic("failingModule", classification.getFailingModule())
                        .diagnostic("failingPlugin", classification.getFailingPlugin())
                        .diagnostic("failingGoal", classification.getFailingGoal())
                        .diagnostic("failedPhase", classification.getFailedPhase())
                        .diagnostic("rootCause", classification.getRootCause())
                        .diagnostic("recoveryAttempts", String.valueOf(attempt))
                        .diagnostic("outputTail", classification.getLastRelevantOutput());

                if (classification.getReportDirectory() != null) {
                    resultBuilder.diagnostic("reportDirectory", classification.getReportDirectory());
                }
                if (classification.getTestSummary() != null && !classification.getTestSummary().isEmpty()) {
                    resultBuilder.diagnostic("testSummary", classification.getTestSummary());
                }

                return resultBuilder.build();
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log("[MAVEN][ERROR] Execution exception: " + e.getMessage());
            return TaskResult.failure(taskId, "Maven execution exception: " + e.getMessage(), e);
        }
    }

    public static String resolveMavenExecutable(File workingDir) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String wrapperName = isWindows ? "mvnw.cmd" : "mvnw";

        if (workingDir != null) {
            File localWrapper = new File(workingDir, wrapperName);
            if (localWrapper.exists()) {
                if (!isWindows) localWrapper.setExecutable(true);
                return localWrapper.getAbsolutePath();
            }

            File parentDir = workingDir.getParentFile();
            if (parentDir != null) {
                File parentWrapper = new File(parentDir, wrapperName);
                if (parentWrapper.exists()) {
                    if (!isWindows) parentWrapper.setExecutable(true);
                    return parentWrapper.getAbsolutePath();
                }
            }
        }

        return isWindows ? "mvn.cmd" : "mvn";
    }

    private String getTail(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(text.length() - maxLength);
    }

    private void log(String message) {
        Log.log(message);
        System.out.println(message);
    }
}
