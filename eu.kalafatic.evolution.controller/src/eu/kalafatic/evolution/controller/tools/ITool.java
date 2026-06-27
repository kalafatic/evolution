package eu.kalafatic.evolution.controller.tools;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Interface for tools used by agents to interact with the environment.
 */
public interface ITool {
    /**
     * @return the name of the tool.
     */
    String getName();

    /**
     * Executes a command using the tool.
     * @param command The specific command or instruction for the tool.
     * @param workingDir The directory where the command should be executed.
     * @param context The shared task context.
     * @return The result of the execution.
     * @throws Exception if execution fails.
     */
    String execute(String command, File workingDir, TaskContext context) throws Exception;
}
