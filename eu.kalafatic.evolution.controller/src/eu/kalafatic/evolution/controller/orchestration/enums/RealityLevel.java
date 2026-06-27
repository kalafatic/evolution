package eu.kalafatic.evolution.controller.orchestration.enums;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Pragma A: Light vs Heavy Reality Gates hierarchy.
 */
public enum RealityLevel {
    /** Static Analysis: NAMING, FORMATTING, SYNTAX. < 100ms. Every branch. */
    LIGHT,

    /** Syntax Check: CLASS_STRUCTURE, METHOD_SIGNATURE. < 500ms. Every branch. */
    MEDIUM,

    /** Full Build: ARCHITECTURE, DEPENDENCIES, DATA_MODEL. > 1s. Winner only. */
    HEAVY,

    /** Integration Tests: PERFORMANCE, SCALABILITY. > 5s. Final synthesis only. */
    EXTREME
}
