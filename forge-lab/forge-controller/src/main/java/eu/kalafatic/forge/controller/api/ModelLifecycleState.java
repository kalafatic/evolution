package eu.kalafatic.forge.controller.api;

public enum ModelLifecycleState {
    CREATED,
    CONFIGURED,
    TRAINING,
    STABILIZING,
    FROZEN,
    COMPILING,
    EXPORTED,
    DEPLOYED,
    OBSERVING,
    EVOLVING
}
