package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
