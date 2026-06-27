package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord;

/**
 * Interface for tracking evolutionary trends and history.
 */
public interface TrajectoryEngine {
    void recordTrajectory(Trajectory trajectory);
    void saveTrajectoryAnalysis(TrajectoryAnalysisRecord record);
    void flush();
}
