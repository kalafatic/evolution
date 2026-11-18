package eu.kalafatic.evolution.controller.harness;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Coding-harness adapter SPI for EVO connected applications (Phase 3, A-14/A-15).
 *
 * <p>Darwin Engine remains the orchestrator: a coding harness is an execution capability
 * that Darwin (e.g. Developer Mode) can delegate coding tasks to. Harness-specific process
 * startup, configuration, input/output, streaming, cancellation, timeouts, permissions and
 * result collection stay behind this adapter boundary (EVO AGENTS.md §6).</p>
 *
 * <p>Harnesses execute within the resolved development target (Developer Mode target
 * resolution: ChatSession.targetPath → context project root → error) and must never write
 * outside it.</p>
 */
public interface CodingHarnessAdapter {

    record ConnectionTestResult(boolean success, String message) {
        public static ConnectionTestResult ok(String message) {
            return new ConnectionTestResult(true, message);
        }

        public static ConnectionTestResult error(String message) {
            return new ConnectionTestResult(false, message != null ? message : "Connection test failed");
        }
    }

    record HarnessExecutionResult(
            boolean success,
            String summary,
            String errorMessage,
            boolean timedOut,
            boolean cancelled,
            List<String> modifiedFiles
    ) {
        public static HarnessExecutionResult ok(String summary, List<String> modifiedFiles) {
            return new HarnessExecutionResult(true, summary != null ? summary : "", "", false, false,
                    modifiedFiles != null ? List.copyOf(modifiedFiles) : List.of());
        }

        public static HarnessExecutionResult error(String error) {
            return new HarnessExecutionResult(false, "", error != null ? error : "Coding harness execution failed",
                    false, false, List.of());
        }

        public static HarnessExecutionResult timedOut(String error) {
            return new HarnessExecutionResult(false, "", error != null ? error : "Coding harness execution timed out",
                    true, false, List.of());
        }

        public static HarnessExecutionResult cancelled(String error) {
            return new HarnessExecutionResult(false, "", error != null ? error : "Coding harness execution cancelled",
                    false, true, List.of());
        }
    }

    /**
     * Stable application identity of this harness.
     */
    String getApplicationId();

    /**
     * Human-readable application name.
     */
    String getApplicationName();

    /**
     * Application type (e.g. "OPENCODE", "AIDER", "NATIVE_CODER").
     */
    String getApplicationType();

    /**
     * Capabilities provided by this harness (e.g. "CODE_IMPLEMENTATION").
     */
    List<String> getCapabilities();

    /**
     * Whether the harness is currently usable (binary/endpoint reachable).
     */
    boolean isAvailable();

    /**
     * Live connection test against the configured binary/endpoint.
     */
    ConnectionTestResult testConnection();

    /**
     * Updates the connection configuration of this harness (validates what it accepts).
     */
    default void updateConnectionConfiguration(Map<String, String> configuration) {}

    /**
     * Returns the current connection configuration (secrets masked).
     */
    default Map<String, String> getConnectionConfiguration() {
        return Map.of();
    }

    /**
     * Executes a delegated coding task inside the given development target. Must be
     * cancellation- and timeout-safe and must never write outside the target boundary.
     */
    HarnessExecutionResult executeTask(
            File targetDir,
            String instructions,
            String prompt,
            Consumer<String> logConsumer,
            BooleanSupplier cancellationSupplier,
            Duration timeout
    );
}
