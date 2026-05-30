package eu.kalafatic.evolution.controller.trajectory;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Analyzes architectural stability to determine convergence.
 */
@EvolutionComponent(
    domain = "trajectory",
    role = "stability-analyzer",
    purpose = "Evaluates convergence based on architectural equilibrium",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.MEDIUM
)
public class StabilityAnalyzer {

    public double calculateStability(Trajectory trajectory, TaskContext context) {
        List<Double> history = trajectory.getFitnessHistory();
        if (history.size() < 2) return 0.0;

        double last = history.get(history.size() - 1);
        double prev = history.get(history.size() - 2);

        double delta = Math.abs(last - prev);
        double stability = Math.max(0.0, 1.0 - (delta * 5.0)); // Highly sensitive to change

        context.log("[STABILITY] Trajectory " + trajectory.getTrajectoryId() + " stability: " + stability + " (Delta: " + delta + ")");
        return stability;
    }

    public boolean isConverged(Trajectory trajectory, TaskContext context) {
        double stability = calculateStability(trajectory, context);
        int generation = trajectory.getGeneration();

        // Check if test mode is active to allow accelerated convergence
        boolean isTestMode = context != null && context.getMetadata().containsKey("testMode");

        // Convergence requires both high stability and a minimum evolutionary depth
        boolean converged = stability > 0.9 && (generation >= 3 || isTestMode);

        if (converged) {
            context.log("[STABILITY] Architectural equilibrium reached for trajectory: " + trajectory.getTrajectoryId());
        }

        return converged;
    }
}
