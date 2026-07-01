package eu.kalafatic.evolution.controller.trajectory;

import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Analyzes architectural stability to determine convergence and progression.
 * Refactored to be policy-driven and agnostic of hardcoded rules.
 */
@EvolutionComponent(
    domain = "trajectory",
    role = "stability-analyzer",
    purpose = "Evaluates convergence and phase progression based on architectural equilibrium policies",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.MEDIUM
)
public class StabilityAnalyzer {

    public double calculateStability(Trajectory trajectory, TaskContext context) {
        return calculateStability(trajectory, context, null);
    }

    public double calculateStability(Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure) {
        return resolvePolicy(context).calculateStability(trajectory, context, pressure);
    }

    public boolean isConverged(Trajectory trajectory, TaskContext context) {
        return isConverged(trajectory, context, null);
    }

    public boolean isConverged(Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure) {
        if (trajectory == null) return false;

        // Force Solution override (Universal Cognitive Signal)
        if (context != null && context.getOrchestrationState().getMetadata().containsKey("forceSolution")) {
            context.log("[STABILITY] Force Solution detected. Forcing convergence.");
            return true;
        }

        return resolvePolicy(context).isConverged(trajectory, context, pressure);
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
    public boolean shouldProgress(EvolutionPhase current, Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure) {
        // Intent expansion always progresses once intent is clear
        if (current == EvolutionPhase.INTENT_EXPANSION) {
            context.log("[STABILITY] Intent expansion phase complete. Progressing.");
            return true;
        }

        if (context != null && context.getOrchestrationState().getMetadata().containsKey("forceSolution")) {
            context.log("[STABILITY] Force Solution detected. Forcing progression.");
            return true;
        }

        if (trajectory == null) return true;

        return resolvePolicy(context).shouldProgress(current, trajectory, context, pressure);
    }

    private IStabilityPolicy resolvePolicy(TaskContext context) {
        // Dynamic policy resolution from context metadata or service registry
        IStabilityPolicy policy = (IStabilityPolicy) context.getOrchestrationState().getMetadata().get("stabilityPolicy");
        if (policy == null) {
            policy = new DefaultStabilityPolicy();
        }
        return policy;
    }
}
