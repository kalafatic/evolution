package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Universal contract for the Headless Cognitive Loop.
 */
public interface ICognitiveLoop {

    /**
     * Executes the cognitive loop to solve the given goal within the session context.
     *
     * @param session The isolated session container.
     * @param taskContext The task execution context.
     * @param goal The domain-independent goal.
     * @return The structured execution result.
     */
    CognitiveResult solve(SessionContainer session, TaskContext taskContext, CognitiveGoal goal);
}
