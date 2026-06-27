package eu.kalafatic.evolution.controller.orchestration.cognitive;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * High-level intent behind a user session.
 */
public enum SessionIntent {
    LEARNING,
    BUILDING,
    ANALYZING,
    TROUBLESHOOTING,
    EVOLVING
}
