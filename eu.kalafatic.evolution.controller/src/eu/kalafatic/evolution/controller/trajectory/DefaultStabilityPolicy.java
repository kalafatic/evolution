package eu.kalafatic.evolution.controller.trajectory;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;

/**
 * Default, context-aware implementation of stability policy.
 */
public class DefaultStabilityPolicy implements IStabilityPolicy {

    @Override
    public double calculateStability(Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure) {
        if (trajectory == null) return 0.0;

        double pressureResolution = calculatePressureResolution(trajectory);
        double mutationEffectiveness = calculateMutationEffectiveness(trajectory);
        double deltaDecay = calculateDeltaDecay(trajectory);
        double confidenceStability = calculateConfidenceStability(trajectory);

        // Dynamic weighting based on trajectory metadata if present
        double stability = (pressureResolution * 0.3) +
                          (mutationEffectiveness * 0.2) +
                          (deltaDecay * 0.3) +
                          (confidenceStability * 0.2);

        return stability;
    }

    @Override
    public boolean isConverged(Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure) {
        if (trajectory == null) return false;

        // Convergence logic is now driven by abstract signals found in metadata/context
        double threshold = context.getOrchestrationState().getMetadata().containsKey("convergenceThreshold") ?
            (Double) context.getOrchestrationState().getMetadata().get("convergenceThreshold") : 0.92;

        int minGen = context.getOrchestrationState().getMetadata().containsKey("minEvolutionaryDepth") ?
            (Integer) context.getOrchestrationState().getMetadata().get("minEvolutionaryDepth") : 2;

        // MEDIATED MODE: Demand deeper refinement for architectural discovery
        if (context.getBehaviorProfile().hasTrait(eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
            minGen = Math.max(minGen, 3);
        }

        // DIAGNOSTIC OPTIMIZATION: Early convergence for analytical tasks in Mediated Mode
        if (context.getBehaviorProfile().hasTrait(eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
            String goal = context.getOrchestrationState().getRawInput();
            if (goal != null && isAnalytical(goal)) {
                // For extremely simple analytical tasks, we might allow earlier convergence,
                // but we still want at least 2 generations of refinement.
                if (trajectory.getGeneration() >= Math.max(2, minGen - 1)) {
                    return true;
                }
            }
        }

        double stability = calculateStability(trajectory, context, pressure);
        int generation = trajectory.getGeneration();

        return generation >= minGen && stability >= threshold;
    }

    @Override
    public boolean shouldProgress(EvolutionPhase current, Trajectory trajectory, TaskContext context, EvolutionaryPressureVector pressure) {
        if (trajectory == null) return true;

        if (isConverged(trajectory, context, pressure)) return true;

        int generation = trajectory.getGeneration();
        int maxGen = context.getOrchestrationState().getMetadata().containsKey("maxEvolutionaryDepth") ?
            (Integer) context.getOrchestrationState().getMetadata().get("maxEvolutionaryDepth") : 8;

        return generation >= maxGen;
    }

    private double calculatePressureResolution(Trajectory trajectory) {
        List<EvolutionaryPressureVector> history = trajectory.getPressureHistory();
        if (history == null || history.size() < 2) return 0.5;
        double first = history.get(0).getTotalPressure();
        double last = history.get(history.size() - 1).getTotalPressure();
        if (first <= 0.0) return 1.0;
        return Math.max(0.0, Math.min(1.0, 0.5 + (1.0 - (last / first))));
    }

    private double calculateMutationEffectiveness(Trajectory trajectory) {
        List<Double> fitness = trajectory.getFitnessHistory();
        if (fitness == null || fitness.size() < 2 || trajectory.getGeneration() <= 0) return 0.5;
        double gain = fitness.get(fitness.size() - 1) - fitness.get(0);
        return Math.max(0.0, Math.min(1.0, 0.5 + (gain / trajectory.getGeneration())));
    }

    private double calculateDeltaDecay(Trajectory trajectory) {
        List<Double> history = trajectory.getFitnessHistory();
        if (history == null || history.size() < 2) return 0.0;
        double delta = Math.abs(history.get(history.size() - 1) - history.get(history.size() - 2));
        return Math.max(0.0, 1.0 - (delta * 4.0));
    }

    private boolean isAnalytical(String goal) {
        if (goal == null) return false;
        String lower = goal.toLowerCase();
        return lower.contains("analyze") || lower.contains("aalyze") || lower.contains("anlyze") ||
               lower.contains("investigate") || lower.contains("report") || lower.contains("summarize");
    }

    private double calculateConfidenceStability(Trajectory trajectory) {
        List<Double> history = trajectory.getConfidenceHistory();
        if (history == null || history.size() < 2) return 0.5;
        double delta = Math.abs(history.get(history.size() - 1) - history.get(history.size() - 2));
        return Math.max(0.0, 1.0 - (delta * 2.0));
    }
}
