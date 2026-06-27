package eu.kalafatic.evolution.controller.orchestration.intent;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Represents the actionable status of a user intent interpretation.
 */
public enum InterpretationState {
    /**
     * One dominant interpretation, proceed immediately.
     */
    CLEAR,

    /**
     * Intent clear, multiple implementation strategies possible, spawn Darwin branches.
     */
    EVOLVABLE,

    /**
     * Critical requirement missing, cannot safely optimize or execute.
     */
    NEEDS_CLARIFICATION,

    /**
     * Impossible to continue.
     */
    BLOCKED,

    /**
     * Conflicting requirements detected.
     */
    CONTRADICTORY
}
