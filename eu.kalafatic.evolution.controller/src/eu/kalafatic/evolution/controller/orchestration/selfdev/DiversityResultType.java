package eu.kalafatic.evolution.controller.orchestration.selfdev;

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
