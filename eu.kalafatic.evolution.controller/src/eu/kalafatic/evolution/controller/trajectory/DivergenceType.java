package eu.kalafatic.evolution.controller.trajectory;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Categorization of divergences between architectural hypothesis (EMF/Trajectories)
 * and physical implementation (Git/Code).
 */
public enum DivergenceType {
    /** Physical structure drifted from the intended architecture. */
    STRUCTURAL_DRIFT,

    /** Implementation behavior does not match the semantic goal. */
    BEHAVIORAL_MISMATCH,

    /** Implementation introduced unexpected complexity. */
    COMPLEXITY_EXPLOSION,

    /** Git state changed without corresponding semantic signal (e.g. noise). */
    SILENT_CHANGE,

    /** Core trajectory hypothesis proved unviable after physical application. */
    HYPOTHESIS_COLLAPSE,

    /** No divergence detected. */
    NONE
}
