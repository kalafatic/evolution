package eu.kalafatic.evolution.controller.orchestration.attachments;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Semantic intents for user requests.
 */
public enum TaskIntent {
    DEBUGGING,
    ANALYSIS,
    IMPLEMENTATION,
    REFACTORING,
    ARCHITECTURE,
    TESTING,
    REVIEW,
    OPTIMIZATION,
    EXPLANATION,
    PLANNING
}
