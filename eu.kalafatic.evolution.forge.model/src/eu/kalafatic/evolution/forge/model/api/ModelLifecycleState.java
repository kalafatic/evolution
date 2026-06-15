package eu.kalafatic.evolution.forge.model.api;

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
