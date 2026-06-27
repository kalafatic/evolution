package eu.kalafatic.evolution.controller.orchestration.intent;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Engine for discovering and analyzing unresolved semantic dimensions.
 */
public interface DimensionInferenceEngine {

    /**
     * Analyzes the given goal/context to discover unresolved semantic dimensions.
     */
    EvolutionAssessment analyze(String goal, TaskContext context) throws Exception;
}
