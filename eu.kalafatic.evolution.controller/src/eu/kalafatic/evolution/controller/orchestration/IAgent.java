package eu.kalafatic.evolution.controller.orchestration;

import java.util.List;

/**
 * Interface for agents in the orchestration system.
 */
public interface IAgent {
    /**
     * @return the unique ID of the agent.
     */
    String getId();

    /**
     * @return the type/role of the agent (e.g., Architect, JavaDev).
     */
    String getType();

    /**
     * Processes a task and returns the result.
     * @param taskDescription The description of the task.
     * @param context The shared context containing history and project info.
     * @param lastFeedback Optional feedback from a previous failed attempt.
     * @return The agent's response/output.
     * @throws Exception if processing fails.
     */
    String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception;

    /**
     * @return the list of tools this agent can access.
     */
    List<ITool> getTools();
}
