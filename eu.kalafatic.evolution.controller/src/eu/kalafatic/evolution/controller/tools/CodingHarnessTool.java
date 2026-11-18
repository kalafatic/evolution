package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

import eu.kalafatic.evolution.controller.harness.CodingHarnessAdapter;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Coding-harness execution tool (Phase 3, WP-3.9). Delegates a coding task to the currently
 * selected connected coding harness (e.g. OpenCode) and executes it inside the resolved
 * development target (the working directory supplied by the Darwin/Developer Mode context —
 * Developer Mode resolves the target via ChatSession.targetPath → context project root →
 * error).
 *
 * <p>Darwin Engine remains the orchestrator: this tool is an execution capability that the
 * Darwin agent invokes; harness selection and lifecycle stay in the connected-application
 * registry behind the coding-harness adapter boundary (EVO AGENTS.md §6).</p>
 */
public class CodingHarnessTool implements ITool {

    public static final String NAME = "coding_harness_execute";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Coding harness requires a task prompt (command)");
        }
        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        Optional<CodingHarnessAdapter> selected = registry.findSelectedAdapter();
        if (selected.isEmpty()) {
            throw new IllegalStateException(
                    "No connected coding harness selected/available. Register and select an application first.");
        }
        CodingHarnessAdapter adapter = selected.get();

        if (workingDir == null || !workingDir.isDirectory()) {
            throw new IllegalStateException("No valid development target directory for coding harness execution");
        }

        if (context != null) {
            context.log("[HARNESS] Delegating task to " + adapter.getApplicationName()
                    + " (" + adapter.getApplicationId() + ") in target: " + workingDir.getAbsolutePath());
        }

        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                workingDir,
                command,
                command,
                context != null ? context::log : s -> {},
                () -> false,
                Duration.ofMinutes(30)
        );

        if (result.timedOut()) {
            throw new IllegalStateException("Coding harness timed out: " + result.errorMessage());
        }
        if (result.cancelled()) {
            throw new IllegalStateException("Coding harness execution cancelled: " + result.errorMessage());
        }
        if (!result.success()) {
            throw new IllegalStateException("Coding harness failed: " + result.errorMessage());
        }
        return result.summary()
                + (result.modifiedFiles().isEmpty() ? "" : "\nModified files: " + String.join(", ", result.modifiedFiles()));
    }
}
