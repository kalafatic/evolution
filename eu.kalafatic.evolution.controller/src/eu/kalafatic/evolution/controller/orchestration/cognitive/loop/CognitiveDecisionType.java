package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

/**
 * Semantic decisions available to the Cognitive Loop.
 */
public enum CognitiveDecisionType {
    CONTINUE,
    RETRY,
    ADAPT,
    USE_CAPABILITY,
    SPAWN_WORKER,
    START_DARWIN,
    WAIT,
    ASK_FOR_AUTHORITY,
    ABORT,
    SUCCESS,
    FAILURE
}
