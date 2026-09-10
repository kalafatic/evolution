package eu.kalafatic.evolution.controller.orchestration.develop.executor;

import java.io.File;
import java.util.Map;

/**
 * Abstraction for external process execution with timeout and cancellation support.
 */
public interface ProcessExecutor {

    /**
     * Executes a system command in the specified working directory with environment and timeout limits.
     *
     * @param command Command line string to execute.
     * @param workDir Working directory for the process.
     * @param env Environment variables (or null for default).
     * @param timeoutMs Maximum runtime in milliseconds.
     * @return Execution result containing exit code, stdout, and stderr.
     * @throws Exception If process execution times out or fails.
     */
    ProcessResult execute(String command, File workDir, Map<String, String> env, long timeoutMs) throws Exception;

    class ProcessResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final boolean timedOut;

        public ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut) {
            this.exitCode = exitCode;
            this.stdout = stdout != null ? stdout : "";
            this.stderr = stderr != null ? stderr : "";
            this.timedOut = timedOut;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public boolean isSuccess() {
            return exitCode == 0 && !timedOut;
        }
    }
}
