package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Engine responsible for identifying and propagating architectural pressures.
 */
@EvolutionComponent(
    domain = "kernel",
    role = "pressure-authority",
    purpose = "Identifies persistent forces driving recursive evolution",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.HIGH
)
public class EvolutionaryPressureEngine {

    public EvolutionaryPressureVector analyze(Trajectory trajectory, TaskContext context) {
        context.log("[PRESSURE] Analyzing evolutionary pressures for trajectory: " + trajectory.getTrajectoryId());

        EvolutionaryPressureVector vector = new EvolutionaryPressureVector();

        // 1. Reliability/Failure Exposure Pressure (Driven by fitness and test failures)
        double fitness = trajectory.getFitnessScore();
        vector.failureExposure = (fitness < 0.8) ? 1.0 - fitness : 0.1;

        // 2. Ambiguity/Cognitive Pressure (Driven by trajectory confidence and discovery depth)
        vector.ambiguity = Math.max(0.1, 1.0 - trajectory.getConfidenceLevel());

        // 3. Extensibility Pressure (Higher in early generations to drive modularity)
        int generation = trajectory.getGeneration();
        vector.extensibility = generation < 3 ? 0.8 : 0.3;

        // 4. Maintainability/Complexity Pressure (Increases with trajectory size/actions)
        int actions = trajectory.getMutationLineage().size();
        vector.dependencyComplexity = Math.min(0.9, actions * 0.15);

        // 5. Implementation Uncertainty (Based on fitness trend)
        if (trajectory.getFitnessHistory().size() >= 2) {
            double last = trajectory.getFitnessHistory().get(trajectory.getFitnessHistory().size() - 1);
            double prev = trajectory.getFitnessHistory().get(trajectory.getFitnessHistory().size() - 2);
            vector.implementationUncertainty = (last <= prev) ? 0.8 : 0.2;
        } else {
            vector.implementationUncertainty = 0.5;
        }

        // 6. Operational Pressure (Baseline for simulation, can be grounded in runtime signals later)
        vector.scalability = 0.2;
        vector.integrationInstability = 0.3;
        vector.concurrencyPressure = 0.1;
        vector.performanceSensitivity = 0.1;

        context.log(String.format("[PRESSURE] Trajectory %s | Total: %.2f (Fail: %.2f, Amb: %.2f, Ext: %.2f, Cmplx: %.2f)",
            trajectory.getTrajectoryId(), vector.getTotalPressure(), vector.failureExposure, vector.ambiguity, vector.extensibility, vector.dependencyComplexity));

        return vector;
    }
}
