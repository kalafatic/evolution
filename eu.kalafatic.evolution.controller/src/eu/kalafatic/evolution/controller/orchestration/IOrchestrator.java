package eu.kalafatic.evolution.controller.orchestration;

/**
 * Interface for the core orchestrator that executes a task graph.
 */
public interface IOrchestrator {
    /**
     * Executes the orchestration for the given task request.
     * This is the unified entry point for all interactions.
     *
     * @param request The task request.
     * @param context The shared execution context.
     * @return The unified response.
     * @throws Exception if execution fails.
     */
    OrchestratorResponse handle(TaskRequest request, TaskContext context) throws Exception;

    /**
     * Executes the orchestration for the given user request.
     * @param request The natural language request describing the coding task.
     * @param context The shared execution context.
     * @return The final status/summary of execution.
     * @throws Exception if execution fails and cannot be recovered.
     * @deprecated Use {@link #handle(TaskRequest, TaskContext)} instead.
     */
    @Deprecated
    String execute(String request, TaskContext context) throws Exception;

    /**
     * Executes a single task using the orchestrator's available agents and tools.
     * @param task The task to execute.
     * @param context The shared execution context.
     * @return The result of task execution.
     * @throws Exception if execution fails.
     */
    String executeTask(eu.kalafatic.evolution.model.orchestration.Task task, TaskContext context) throws Exception;
}
