package eu.kalafatic.evolution.controller.orchestration.develop.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Concrete ProcessExecutor implementation.
 */
public class ProcessExecutorImpl implements ProcessExecutor {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public ProcessResult execute(String command, File workDir, Map<String, String> env, long timeoutMs) throws Exception {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder pb = isWin
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("sh", "-c", command);

        if (workDir != null && workDir.exists()) {
            pb.directory(workDir);
        }

        if (env != null && !env.isEmpty()) {
            pb.environment().putAll(env);
        }

        Process process = pb.start();

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Future<?> stdoutFuture = executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdoutBuilder.append(line).append("\n");
                }
            } catch (Exception ignored) {
            }
        });

        Future<?> stderrFuture = executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrBuilder.append(line).append("\n");
                }
            } catch (Exception ignored) {
            }
        });

        boolean completed = process.waitFor(timeoutMs > 0 ? timeoutMs : 300000, TimeUnit.MILLISECONDS);

        if (!completed) {
            process.destroyForcibly();
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            return new ProcessResult(-1, stdoutBuilder.toString(), stderrBuilder.toString() + "\nProcess timed out after " + timeoutMs + " ms", true);
        }

        try {
            stdoutFuture.get(2, TimeUnit.SECONDS);
            stderrFuture.get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        return new ProcessResult(process.exitValue(), stdoutBuilder.toString(), stderrBuilder.toString(), false);
    }
}
