package eu.kalafatic.evolution.controller.trajectory;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Analyzes architectural stability to determine convergence and progression.
 */
@EvolutionComponent(
    domain = "trajectory",
    role = "stability-analyzer",
    purpose = "Evaluates convergence and phase progression based on architectural equilibrium",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.MEDIUM
)
public class StabilityAnalyzer {

    public double calculateStability(Trajectory trajectory, TaskContext context) {
        return calculateStability(trajectory, context, null);
    }

    public double calculateStability(Trajectory trajectory, TaskContext context, eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure) {
        if (trajectory == null) return 0.0;
        List<Double> history = trajectory.getFitnessHistory();
        if (history == null || history.size() < 2) return 0.0;

        double last = history.get(history.size() - 1);
        double prev = history.get(history.size() - 2);

        double delta = Math.abs(last - prev);
        double stability = Math.max(0.0, 1.0 - (delta * 5.0)); // Highly sensitive to change

        context.log("[STABILITY] Trajectory " + trajectory.getTrajectoryId() + " stability: " + stability + " (Delta: " + delta + ")");
        return stability;
    }

    public boolean isConverged(Trajectory trajectory, TaskContext context) {
        return isConverged(trajectory, context, null);
    }

    public boolean isConverged(Trajectory trajectory, TaskContext context, eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure) {
        if (trajectory == null) return false;

        // Intent expansion never triggers convergence logic
        if (context != null && context.getOrchestrationState() != null) {
            String phase = context.getOrchestrationState().getCurrentPhase();
            if (EvolutionPhase.INTENT_EXPANSION.name().equals(phase) || "INTENT_EXPANSION".equals(phase)) {
                return false;
            }
        }

        double stability = calculateStability(trajectory, context);

        // Check if test mode is active to allow accelerated convergence
        boolean isTestMode = context != null && context.getMetadata().containsKey("testMode");

        // Convergence requires high stability. Minimum depth is now handled by shouldProgress.
        boolean converged = stability > 0.9;

        if (converged) {
            context.log("[STABILITY] Architectural equilibrium reached for trajectory: " + trajectory.getTrajectoryId());
        }

        return converged;
    }

    /**
     * Determines if the evolution should progress to the next phase or stay in the current one.
     */
    public boolean shouldProgress(EvolutionPhase current, Trajectory trajectory, TaskContext context) {
        return shouldProgress(current, trajectory, context, null);
    }

    /**
     * Determines if the evolution should progress to the next phase or stay in the current one with pressure awareness.
     */
    public boolean shouldProgress(EvolutionPhase current, Trajectory trajectory, TaskContext context, eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector pressure) {
        // Intent expansion always progresses once intent is clear (handled in IterationManager)
        if (current == EvolutionPhase.INTENT_EXPANSION) {
            context.log("[STABILITY] Intent expansion phase complete. Progressing.");
            return true;
        }

        if (trajectory == null) return true;

        int generation = trajectory.getGeneration();
        boolean converged = isConverged(trajectory, context);

        // Mandatory evolutionary depth for early stages
        if (generation < 3 && !converged) {
            if (current == EvolutionPhase.ARCHITECTURE_VARIANTS || current == EvolutionPhase.SELECTION_REFINEMENT) {
                context.log("[STABILITY] Mandatory evolutionary depth not reached (Gen: " + generation + "). Recursing in " + current);
                return false;
            }
        }

        if (converged) {
            context.log("[STABILITY] Stability confirmed. Ready to progress from " + current);
            return true;
        }

        // If not converged, we might still want to progress after some effort
        if (generation >= 5) {
            context.log("[STABILITY] Maximum generation limit reached for phase. Forcing progression from " + current);
            return true;
        }

        // Default to recursion in evolutionary phases if not converged and depth not reached
        return current == EvolutionPhase.IMPLEMENTATION_PLAN || current == EvolutionPhase.FINAL_SYNTHESIS;
    }
}
