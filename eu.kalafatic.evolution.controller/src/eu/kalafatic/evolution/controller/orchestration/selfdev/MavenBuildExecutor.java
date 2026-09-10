package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MavenBuildExecutor {

    private final String taskId;

    public MavenBuildExecutor(String taskId) {
        this.taskId = taskId;
    }

    public TaskResult executeBuild(File workingDir, List<String> goals, List<String> userArgs, File logFile, long timeoutMinutes) {
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
        System.out.println("[MavenBuildExecutor] Executing: " + fullCommandStr + " in " + workingDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        String javaHome = System.getProperty("java.home");
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
                return new TaskResult.Builder(taskId)
                        .status(TaskStatus.FAILED)
                        .message("Maven execution timed out after " + timeoutMinutes + " minutes.")
                        .command(fullCommandStr)
                        .workingDirectory(workingDir)
                        .duration(duration)
                        .logFile(logFile)
                        .diagnostic("outputTail", getTail(outputBuffer.toString(), 2000))
                        .build();
            }

            exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0) {
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
                String failureSummary = extractFailureSummary(outputBuffer.toString());
                return new TaskResult.Builder(taskId)
                        .status(TaskStatus.FAILED)
                        .message("Maven build failed with exit code " + exitCode + ": " + failureSummary)
                        .command(fullCommandStr)
                        .workingDirectory(workingDir)
                        .exitCode(exitCode)
                        .duration(duration)
                        .logFile(logFile)
                        .diagnostic("outputTail", getTail(outputBuffer.toString(), 2000))
                        .build();
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
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

    private String extractFailureSummary(String fullOutput) {
        if (fullOutput == null || fullOutput.isEmpty()) return "No output";
        String[] lines = fullOutput.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.contains("[ERROR]") || line.contains("BUILD FAILURE")) {
                sb.append(line).append(" ");
            }
        }
        if (sb.length() > 0) {
            return sb.toString().trim();
        }
        return getTail(fullOutput, 300);
    }

    private String getTail(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(text.length() - maxLength);
    }
}
