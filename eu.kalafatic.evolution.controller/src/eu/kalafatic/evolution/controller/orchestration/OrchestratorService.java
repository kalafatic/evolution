package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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

    /**
     * Pauses or resumes a session.
     * @param sessionId The session ID.
     * @param paused True to pause, false to resume.
     */
    void setPaused(String sessionId, boolean paused);

    /**
     * Submits a request to a session.
     * @param sessionId The session ID.
     * @param request The task request.
     */
    void submit(String sessionId, TaskRequest request);

    /**
     * Updates the configuration for a session.
     * @param sessionId The session ID.
     * @param settings The settings to update.
     */
    void updateConfiguration(String sessionId, java.util.Map<String, Object> settings);

    /**
     * Provides input for a waiting task.
     * @param taskId The task ID (usually sessionId).
     * @param input The user input.
     */
    void provideInput(String taskId, String input);

    /**
     * Provides approval for a waiting task.
     * @param taskId The task ID (usually sessionId).
     * @param approved True if approved.
     */
    void provideApproval(String taskId, boolean approved);

    /**
     * Resumes a specific step in a session.
     * @param sessionId The session ID.
     * @param stepId The step ID.
     * @param status The target status for resumption.
     */
    void resumeStep(String sessionId, String stepId, eu.kalafatic.evolution.controller.workflow.WorkflowStatus status);
}
