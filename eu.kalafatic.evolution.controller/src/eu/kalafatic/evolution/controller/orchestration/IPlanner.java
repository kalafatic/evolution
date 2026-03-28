package eu.kalafatic.evolution.controller.orchestration;

import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Interface for the planning agent responsible for decomposing requests into tasks.
 */
public interface IPlanner {
    /**
     * Decomposes a natural language request into a list of structured tasks.
     * @param request The user's coding request.
     * @param context The shared task context.
     * @return A list of tasks to execute.
     * @throws Exception if decomposition fails.
     */
    List<Task> plan(String request, TaskContext context) throws Exception;
}
