package eu.kalafatic.evolution.controller.kernel;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Engine responsible for mutating trajectories under pressure.
 */
@EvolutionComponent(
    domain = "kernel",
    role = "mutation-authority",
    purpose = "Mutates previously selected winners recursively based on pressure",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.CRITICAL
)
public class TrajectoryMutationEngine {

    public List<BranchVariant.Action> proposeMutations(Trajectory survivor, EvolutionaryPressureVector pressure, TaskContext context) {
        context.log("[MUTATION] Proposing recursive mutations for trajectory: " + survivor.getTrajectoryId());
        List<BranchVariant.Action> mutations = new ArrayList<>();

        if (pressure.failureExposure > 0.7) {
            mutations.add(createAction("RESILIENCE", "STRENGTHEN", "workspace", "Introduce robust error handling and validation."));
        }

        if (pressure.ambiguity > 0.6) {
            mutations.add(createAction("COGNITION", "CLARIFY", "architecture", "Refine architectural boundaries and interfaces."));
        }

        if (pressure.extensibility > 0.4) {
            mutations.add(createAction("STRUCTURE", "REFACTOR", "design", "Abstract hardcoded logic into extensible services."));
        }

        return mutations;
    }

    private BranchVariant.Action createAction(String domain, String operation, String target, String description) {
        BranchVariant.Action action = new BranchVariant.Action();
        action.setDomain(domain);
        action.setOperation(operation);
        action.setTarget(target);
        action.setDescription(description);
        return action;
    }
}
