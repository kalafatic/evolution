package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;

public class DefaultMutationEngine implements MutationEngine {
    private final eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine engine;

    public DefaultMutationEngine(eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine engine) {
        this.engine = engine;
    }

    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory, EvolutionaryPressureVector pressure) throws Exception {
        return engine.generateVariants(goal, snapshot, failureMemory, trajectory, pressure);
    }
}
