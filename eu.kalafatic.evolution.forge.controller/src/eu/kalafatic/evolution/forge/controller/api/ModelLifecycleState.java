package eu.kalafatic.evolution.forge.controller.api;

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
