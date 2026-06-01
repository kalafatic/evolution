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

        double pressureResolution = calculatePressureResolution(trajectory, context);
        double mutationEffectiveness = calculateMutationEffectiveness(trajectory, context);
        double deltaDecay = calculateDeltaDecay(trajectory, context);
        double confidenceStability = calculateConfidenceStability(trajectory, context);

        // System Equilibrium: Weighted combination of factors
        double stability = (pressureResolution * 0.3) +
                          (mutationEffectiveness * 0.2) +
                          (deltaDecay * 0.3) +
                          (confidenceStability * 0.2);

        context.log(String.format("[STABILITY] Trajectory %s | Stability: %.2f (PR: %.2f, ME: %.2f, DD: %.2f, CS: %.2f)",
            trajectory.getTrajectoryId(), stability, pressureResolution, mutationEffectiveness, deltaDecay, confidenceStability));

        return stability;
    }

    private double calculatePressureResolution(Trajectory trajectory, TaskContext context) {
        List<eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector> history = trajectory.getPressureHistory();
        if (history == null || history.size() < 2) return 0.5;

        double first = history.get(0).getTotalPressure();
        double last = history.get(history.size() - 1).getTotalPressure();

        if (first <= 0.0) return 1.0;

        // Higher resolution (lower pressure) means higher stability
        double resolution = 1.0 - (last / first);
        return Math.max(0.0, Math.min(1.0, 0.5 + resolution));
    }

    private double calculateMutationEffectiveness(Trajectory trajectory, TaskContext context) {
        List<Double> fitness = trajectory.getFitnessHistory();
        if (fitness == null || fitness.size() < 2) return 0.5;

        double gain = fitness.get(fitness.size() - 1) - fitness.get(0);
        int generations = trajectory.getGeneration();

        if (generations <= 0) return 0.5;

        // Effectiveness: Gain per generation
        double effectiveness = gain / generations;
        return Math.max(0.0, Math.min(1.0, 0.5 + effectiveness));
    }

    private double calculateDeltaDecay(Trajectory trajectory, TaskContext context) {
        List<Double> history = trajectory.getFitnessHistory();
        if (history == null || history.size() < 2) return 0.0;

        double last = history.get(history.size() - 1);
        double prev = history.get(history.size() - 2);

        double delta = Math.abs(last - prev);
        // Delta decay: Smaller delta means higher stability
        return Math.max(0.0, 1.0 - (delta * 4.0));
    }

    private double calculateConfidenceStability(Trajectory trajectory, TaskContext context) {
        List<Double> history = trajectory.getConfidenceHistory();
        if (history == null || history.size() < 2) return 0.5;

        double last = history.get(history.size() - 1);
        double prev = history.get(history.size() - 2);

        double delta = Math.abs(last - prev);
        return Math.max(0.0, 1.0 - (delta * 2.0));
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

        double stability = calculateStability(trajectory, context, pressure);

        // Check if test mode is active to allow accelerated convergence
        boolean isTestMode = context != null && context.getMetadata().containsKey("testMode");

        // Convergence requires high stability. Minimum depth is now handled by shouldProgress.
        boolean converged = stability > 0.85; // Lower threshold for multi-factor stability

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
