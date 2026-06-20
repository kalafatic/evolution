package eu.kalafatic.evolution.controller.trajectory;

import java.util.List;
import eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine;
import eu.kalafatic.evolution.controller.kernel.TrajectoryMutationEngine;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhaseMachine;
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
    private final EvolutionPhaseMachine phaseMachine = new EvolutionPhaseMachine();

    public boolean evolve(Trajectory trajectory, TaskContext context) {
        context.log("[EVOLUTION] Driving recursive trajectory iteration: " + trajectory.getTrajectoryId());

        // 1. Analyze Pressure
        EvolutionaryPressureVector pressure = pressureEngine.analyze(trajectory, context);
        trajectory.recordPressure(pressure);

        // 2. Check Stability
        if (stabilityAnalyzer.isConverged(trajectory, context, pressure)) {
            context.log("[EVOLUTION] Trajectory " + trajectory.getTrajectoryId() + " has reached stability under pressure (Gen: " + trajectory.getGeneration() + "). Convergence confirmed.");
            return true;
        }

        // 3. Propose Mutations (Orchestrator-owned discovery of territorial expansion)
        List<BranchVariant.Action> mutations = mutationEngine.proposeMutations(trajectory, pressure, context);
        context.log("[EVOLUTION] Orchestrator proposed " + mutations.size() + " targeted mutations for generation " + (trajectory.getGeneration() + 1));

        // Update trajectory generation
        trajectory.setGeneration(trajectory.getGeneration() + 1);

        return false;
    }

    /**
     * Determines the next phase based on trajectory stability and progression rules.
     */
    public EvolutionPhase determineNextPhase(EvolutionPhase current, Trajectory trajectory, TaskContext context) {
        // ADAPTIVE KERNEL: Phase acceleration based on Evolution Intensity
        int intensity = 4; // Default high
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session =
            eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(context.getSessionId());
        if (session instanceof eu.kalafatic.evolution.controller.orchestration.SessionContext) {
            int depth = ((eu.kalafatic.evolution.controller.orchestration.SessionContext)session).getCognitiveState().getCognitiveDepth();
            intensity = eu.kalafatic.evolution.controller.orchestration.cognitive.CognitiveStateEngine.getEvolutionIntensity(depth);
        }

        if (intensity == 1 && current.ordinal() < EvolutionPhase.FINAL_SYNTHESIS.ordinal()) {
            context.log("[EVOLUTION] Low Intensity detected. Accelerating directly to FINAL_SYNTHESIS.");
            return EvolutionPhase.FINAL_SYNTHESIS;
        }

        // Compute pressure for informed decision making
        EvolutionaryPressureVector pressure = null;
        if (trajectory != null) {
            pressure = pressureEngine.analyze(trajectory, context);
        }

        if (stabilityAnalyzer.isConverged(trajectory, context, pressure)) {
             // If converged, we can skip to FINAL_SYNTHESIS if not already there or further
             if (current.ordinal() < EvolutionPhase.FINAL_SYNTHESIS.ordinal()) {
                 context.log("[EVOLUTION] Stability reached. Accelerating to FINAL_SYNTHESIS.");
                 return EvolutionPhase.FINAL_SYNTHESIS;
             }
        }

        if (stabilityAnalyzer.shouldProgress(current, trajectory, context, pressure)) {
            EvolutionPhase next = phaseMachine.next(current);
            context.log("[EVOLUTION] Progression allowed. Next phase: " + next);
            return next;
        } else {
            context.log("[EVOLUTION] Stability not yet reached. Recursing in phase: " + current);
            return current;
        }
    }

    public StabilityAnalyzer getStabilityAnalyzer() {
        return stabilityAnalyzer;
    }
}
