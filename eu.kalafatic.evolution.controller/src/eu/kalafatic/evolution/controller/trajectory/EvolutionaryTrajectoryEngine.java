package eu.kalafatic.evolution.controller.trajectory;

import java.util.List;
import eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine;
import eu.kalafatic.evolution.controller.kernel.TrajectoryMutationEngine;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Coordinator for recursive trajectory evolution.
 */
@EvolutionComponent(
    domain = "trajectory",
    role = "evolution-coordinator",
    purpose = "Drives recursive mutation and pressure-based adaptation",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.CRITICAL
)
public class EvolutionaryTrajectoryEngine {

    private final EvolutionaryPressureEngine pressureEngine = new EvolutionaryPressureEngine();
    private final TrajectoryMutationEngine mutationEngine = new TrajectoryMutationEngine();
    private final StabilityAnalyzer stabilityAnalyzer = new StabilityAnalyzer();

    public boolean evolve(Trajectory trajectory, TaskContext context) {
        context.log("[EVOLUTION] Driving recursive trajectory iteration: " + trajectory.getTrajectoryId());

        // 1. Analyze Pressure
        EvolutionaryPressureVector pressure = pressureEngine.analyze(trajectory, context);

        // 2. Check Stability
        if (stabilityAnalyzer.isConverged(trajectory, context)) {
            context.log("[EVOLUTION] Trajectory " + trajectory.getTrajectoryId() + " has reached stability. Convergence confirmed.");
            return true;
        }

        // 3. Propose Mutations
        List<BranchVariant.Action> mutations = mutationEngine.proposeMutations(trajectory, pressure, context);
        context.log("[EVOLUTION] Proposed " + mutations.size() + " mutations for generation " + (trajectory.getGeneration() + 1));

        // Update trajectory generation
        trajectory.setGeneration(trajectory.getGeneration() + 1);

        return false;
    }

    public StabilityAnalyzer getStabilityAnalyzer() {
        return stabilityAnalyzer;
    }
}
