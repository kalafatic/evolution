package eu.kalafatic.evolution.controller.orchestration;

/**
 * Service for executing orchestration tasks.
 */
public interface OrchestratorService {
    /**
     * Executes an orchestration task.
     * @param request The task request.
     * @return The initial task result.
     */
    TaskResult execute(TaskRequest request);

    /**
     * Gets the current result of a task.
     * @param id The task ID.
     * @return The task result.
     */
    TaskResult getTaskResult(String id);
}
