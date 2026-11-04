package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

/**
 * State machine states for Cognitive Loop lifecycle.
 */
public enum CognitiveState {
    CREATED,
    GOAL,
    UNDERSTANDING,
    OBSERVING,
    PLANNING,
    SELECT_CAPABILITY,
    ACTING,
    EVALUATING,
    ADAPTING,
    DARWINING,
    WAITING,
    SUCCESS,
    FAILED,
    ABORTED,
    CANCELLED,
    TIMEOUT
}
