package eu.kalafatic.evolution.controller.orchestration.develop.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * Hardened process executor providing isolated sandbox execution for untrusted project builds and tests.
 * Enforces:
 * - Environment variable sanitization (stripping host tokens, SSH keys, server credentials).
 * - Working directory containment (blocks executions escaping designated workspace).
 * - Execution timeouts.
 * - Recursive process tree termination (ProcessHandle.descendants()).
 * - Capped output streaming to prevent memory exhaustion.
 */
public class SandboxProcessExecutor implements ProcessExecutor {

    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

    private static final Set<String> SENSITIVE_ENV_PREFIXES = new HashSet<>(Arrays.asList(
            "EVO_", "TRYST_", "AWS_", "GITHUB_", "GITLAB_", "TOKEN", "SECRET", "PASSWORD", "KEY", "AUTH"
    ));

    private static final Set<String> ALLOWED_ENV_VARS = new HashSet<>(Arrays.asList(
            "PATH", "JAVA_HOME", "M2_HOME", "MAVEN_HOME", "GRADLE_HOME", "NODE_HOME",
            "HOME", "USER", "TMPDIR", "TEMP", "TMP", "LANG", "LC_ALL", "TERM"
    ));

    private static final int MAX_STREAM_CHARS = 1024 * 1024 * 2; // 2MB stream cap

    @Override
    public ProcessResult execute(String command, File workDir, Map<String, String> env, long timeoutMs) throws Exception {
        return executeSandboxed(command, workDir, null, env, timeoutMs);
    }

    /**
     * Executes command strictly contained within the allowedRoot directory.
     */
    public ProcessResult executeSandboxed(String command, File workDir, File allowedRoot, Map<String, String> env, long timeoutMs) throws Exception {
        if (command == null || command.trim().isEmpty()) {
            return new ProcessResult(-1, "", "Command cannot be empty", false);
        }

        // Validate workDir containment using java.nio.file.Path to prevent prefix escapes
        if (workDir != null && allowedRoot != null) {
            java.nio.file.Path canonicalWork = workDir.toPath().toRealPath();
            java.nio.file.Path canonicalRoot = allowedRoot.toPath().toRealPath();
            if (!canonicalWork.startsWith(canonicalRoot)) {
                Log.log("[SANDBOX-VIOLATION] Directory traversal blocked: workDir " + canonicalWork + " outside root " + canonicalRoot);
                return new ProcessResult(-1, "", "Sandbox violation: Working directory is outside allowed project root.", false);
            }
        }

        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> commandList = new ArrayList<>();
        if (isWin) {
            commandList.add("cmd.exe");
            commandList.add("/c");
            commandList.add(command);
        } else {
            commandList.add("sh");
            commandList.add("-c");
            commandList.add(command);
        }

        ProcessBuilder pb = new ProcessBuilder(commandList);

        if (workDir != null && workDir.exists()) {
            pb.directory(workDir);
        }

        // Sanitize environment
        Map<String, String> processEnv = pb.environment();
        Map<String, String> sanitizedEnv = sanitizeEnvironment(processEnv);
        processEnv.clear();
        processEnv.putAll(sanitizedEnv);

        // Apply caller overrides
        if (env != null && !env.isEmpty()) {
            for (Map.Entry<String, String> entry : env.entrySet()) {
                if (isEnvironmentVariableSafe(entry.getKey())) {
                    processEnv.put(entry.getKey(), entry.getValue());
                }
            }
        }

        Log.log("[SANDBOX] Launching sandboxed process in " + (workDir != null ? workDir.getAbsolutePath() : "."));
        Process process = pb.start();

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Future<?> stdoutFuture = ioExecutor.submit(() -> captureStream(process.getInputStream(), stdoutBuilder));
        Future<?> stderrFuture = ioExecutor.submit(() -> captureStream(process.getErrorStream(), stderrBuilder));

        long effectiveTimeout = timeoutMs > 0 ? timeoutMs : 300000;
        boolean completed = process.waitFor(effectiveTimeout, TimeUnit.MILLISECONDS);

        if (!completed) {
            Log.log("[SANDBOX] Process timed out after " + effectiveTimeout + " ms. Terminating process tree.");
            terminateProcessTree(process);
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            return new ProcessResult(-1, stdoutBuilder.toString(), stderrBuilder.toString() + "\nProcess timed out and was killed by sandbox after " + effectiveTimeout + " ms", true);
        }

        try {
            stdoutFuture.get(2, TimeUnit.SECONDS);
            stderrFuture.get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        return new ProcessResult(process.exitValue(), stdoutBuilder.toString(), stderrBuilder.toString(), false);
    }

    private void captureStream(java.io.InputStream stream, StringBuilder target) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (target) {
                    if (target.length() < MAX_STREAM_CHARS) {
                        target.append(line).append("\n");
                    } else if (target.length() == MAX_STREAM_CHARS) {
                        target.append("\n[OUTPUT TRUNCATED BY SANDBOX: Exceeded 2MB buffer]\n");
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public Map<String, String> sanitizeEnvironment(Map<String, String> originalEnv) {
        Map<String, String> clean = new HashMap<>();
        for (Map.Entry<String, String> entry : originalEnv.entrySet()) {
            String key = entry.getKey();
            if (isEnvironmentVariableSafe(key)) {
                clean.put(key, entry.getValue());
            }
        }
        return clean;
    }

    public boolean isEnvironmentVariableSafe(String key) {
        if (key == null) return false;
        String upper = key.toUpperCase();
        for (String sensitive : SENSITIVE_ENV_PREFIXES) {
            if (upper.contains(sensitive)) return false;
        }
        return ALLOWED_ENV_VARS.contains(upper) || upper.startsWith("LC_") || upper.startsWith("JAVA_");
    }

    public void terminateProcessTree(Process process) {
        if (process == null) return;
        try {
            ProcessHandle handle = process.toHandle();
            handle.descendants().forEach(child -> {
                try {
                    child.destroyForcibly();
                } catch (Exception ignored) {
                }
            });
            handle.destroyForcibly();
        } catch (Exception ex) {
            process.destroyForcibly();
        }
    }
}
