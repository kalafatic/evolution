package eu.kalafatic.evolution.controller.trajectory;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;

/**
 * Policy interface for determining trajectory stability and convergence.
 */
public interface IStabilityPolicy {
    double calculateStability(Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure);
    boolean isConverged(Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure);
    boolean shouldProgress(eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase current, Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure);
}
