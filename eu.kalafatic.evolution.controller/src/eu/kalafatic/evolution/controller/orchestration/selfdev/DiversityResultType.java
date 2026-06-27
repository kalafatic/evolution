package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Result of a diversity analysis cycle.
 */
public enum DiversityResultType {
    /** Trajectory is conceptually unique and adheres to blueprint. */
    ACCEPTED,

    /** Trajectory has minor issues (e.g. soft dimension mismatch) but is preserved. */
    ACCEPTED_WITH_WARNINGS,

    /** Trajectory is a redundant duplicate or violates strict blueprint constraints. */
    REJECTED_FATAL
}
