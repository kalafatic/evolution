package eu.kalafatic.evolution.controller.orchestration;

/**
 * Stages of an evolutionary iteration.
 */
public enum EvolutionStage {
    ITERATION_START,
    ANALYSIS,
    ANALYZE_PARENT,
    GENERATE_BRANCH,
    VALIDATE_BRANCH,
    SCORE_BRANCH,
    SELECT_WINNER,
    SAVE_LINEAGE,
    ITERATION_COMPLETE
}
