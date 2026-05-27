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
     * Executes an orchestration task and returns a unified response.
     * @param request The task request.
     * @return The orchestrator response.
     */
    OrchestratorResponse handle(TaskRequest request);

    /**
     * Gets the current result of a task.
     * @param id The task ID.
     * @return The task result.
     */
    TaskResult getTaskResult(String id);

    /**
     * Shuts down and removes a session.
     * @param sessionId The session ID.
     */
    void shutdownSession(String sessionId);
}
