package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceDeltaAnalyzer;

/**
 * Interface for physical change verification (Reality Check).
 */
public interface RealityEngine {
    WorkspaceDeltaAnalyzer.DeltaAnalysis analyze(String baseCommit) throws Exception;
}
