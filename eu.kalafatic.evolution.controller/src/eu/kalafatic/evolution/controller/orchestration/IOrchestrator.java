package eu.kalafatic.evolution.controller.orchestration;

/**
 * Interface for the core orchestrator that executes a task graph.
 */
public interface IOrchestrator {
    /**
     * Executes the orchestration for the given user request.
     * @param request The natural language request describing the coding task.
     * @param context The shared execution context.
     * @return The final status/summary of execution.
     * @throws Exception if execution fails and cannot be recovered.
     */
    String execute(String request, TaskContext context) throws Exception;
}
