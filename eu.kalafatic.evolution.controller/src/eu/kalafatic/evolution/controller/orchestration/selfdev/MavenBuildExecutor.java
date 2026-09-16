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

        String mavenExec = resolveMavenExecutable(workingDir);
        List<String> command = new ArrayList<>();
        command.add(mavenExec);
        if (goals != null && !goals.isEmpty()) {
            command.addAll(goals);
        }
        if (userArgs != null && !userArgs.isEmpty()) {
            command.addAll(userArgs);
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

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        if (javaHome != null && !javaHome.isEmpty()) {
            env.put("JAVA_HOME", javaHome);
        }

        StringBuilder outputBuffer = new StringBuilder();
        int exitCode = -1;

        try (PrintWriter logWriter = (logFile != null) ? new PrintWriter(new FileWriter(logFile, true)) : null) {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuffer.append(line).append("\n");
                    if (logWriter != null) {
                        logWriter.println(line);
                        logWriter.flush();
                    }
                }
            }

            boolean finished = process.waitFor(timeoutMinutes > 0 ? timeoutMinutes : 30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                long duration = System.currentTimeMillis() - startTime;
                log("[MAVEN][ERROR] Build timed out after " + timeoutMinutes + " minutes.");
                return new TaskResult.Builder(taskId)
                        .status(TaskStatus.FAILED)
                        .message("Maven execution timed out after " + timeoutMinutes + " minutes.")
                        .command(fullCommandStr)
                        .workingDirectory(workingDir)
                        .duration(duration)
                        .logFile(logFile)
                        .diagnostic("errorCategory", MavenErrorCategory.TRANSIENT.name())
                        .diagnostic("outputTail", getTail(outputBuffer.toString(), 2000))
                        .build();
            }

            exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

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
                String fullOutput = outputBuffer.toString();
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
