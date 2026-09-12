package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

/**
 * State machine states for Cognitive Loop lifecycle.
 */
public enum CognitiveState {
    CREATED,
    OBSERVING,
    UNDERSTANDING,
    PLANNING,
    ACTING,
    EVALUATING,
    ADAPTING,
    DARWINING,
    WAITING,
    SUCCESS,
    FAILED,
    ABORTED
}
