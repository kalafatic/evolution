package eu.kalafatic.evolution.controller.trajectory;

import java.util.List;
import eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine;
import eu.kalafatic.evolution.controller.kernel.TrajectoryMutationEngine;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhaseMachine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractionLevel;
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
        // 1. Compute pressure for informed decision making
        EvolutionaryPressureVector pressure = null;
        if (trajectory != null) {
            pressure = pressureEngine.analyze(trajectory, context);
        }

        // 2. ADAPTIVE KERNEL: Phase selection based on Evolution Intensity Profile
        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile =
            context.getExecutionProfile();
        int intensity = profile.getIntensity();

        EvolutionPhase nextCandidate = phaseMachine.next(current, profile);
        while (nextCandidate != null && nextCandidate.ordinal() < EvolutionPhase.FINAL_SYNTHESIS.ordinal()) {
            boolean skip = false;

            // Intensity-based skipping
            if (getMinimumIntensity(nextCandidate) > intensity) {
                context.log("[EVOLUTION] Skipping phase " + nextCandidate + " (Min intensity " + getMinimumIntensity(nextCandidate) + " > current " + intensity + ")");
                skip = true;
            }

            // Capability-based skipping (from Profile)
            if (!skip && nextCandidate == EvolutionPhase.IMPLEMENTATION_PLAN && !profile.useImplementation()) {
                context.log("[EVOLUTION] Skipping phase IMPLEMENTATION_PLAN (Implementation disabled for profile)");
                skip = true;
            }

            // Abstraction Level-based skipping (LOCKED Problem Space)
            AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
            if (!skip && lockedLevel != null) {
                if (nextCandidate == EvolutionPhase.ARCHITECTURE_VARIANTS &&
                    (lockedLevel == AbstractionLevel.DESIGN || lockedLevel == AbstractionLevel.IMPLEMENTATION)) {
                    context.log("[EVOLUTION] Skipping phase ARCHITECTURE_VARIANTS (Locked to " + lockedLevel + ")");
                    skip = true;
                } else if (nextCandidate == EvolutionPhase.SELECTION_REFINEMENT &&
                           lockedLevel == AbstractionLevel.IMPLEMENTATION) {
                    context.log("[EVOLUTION] Skipping phase SELECTION_REFINEMENT (Locked to IMPLEMENTATION)");
                    skip = true;
                }
            }

            if (skip) {
                nextCandidate = phaseMachine.next(nextCandidate);
            } else {
                break;
            }
        }
        if (stabilityAnalyzer.isConverged(trajectory, context, pressure)) {
             // If converged, we can skip to FINAL_SYNTHESIS if not already there or further
             if (current.ordinal() < EvolutionPhase.FINAL_SYNTHESIS.ordinal()) {
                 context.log("[EVOLUTION] Stability reached. Accelerating to FINAL_SYNTHESIS.");
                 return EvolutionPhase.FINAL_SYNTHESIS;
             }
        }

        // 4. Progression Decision
        if (stabilityAnalyzer.shouldProgress(current, trajectory, context, pressure)) {
            context.log("[EVOLUTION] Progression allowed. Next phase: " + nextCandidate);
            return nextCandidate;
        } else {
            context.log("[EVOLUTION] Stability not yet reached. Recursing in phase: " + current);
            return current;
        }
    }

    public StabilityAnalyzer getStabilityAnalyzer() {
        return stabilityAnalyzer;
    }

    private int getMinimumIntensity(EvolutionPhase phase) {
        switch (phase) {
            case INTENT_EXPANSION: return 1;
            case ARCHITECTURE_VARIANTS: return 3;
            case SELECTION_REFINEMENT: return 2;
            case IMPLEMENTATION_PLAN: return 2;
            case FINAL_SYNTHESIS: return 1;
            case DESIGN_SATISFIED: return 3;
            case TERMINAL_SUCCESS: return 1;
            case TERMINAL_FAILURE: return 1;
            default: return 1;
        }
    }
}
