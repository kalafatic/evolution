package eu.kalafatic.evolution.controller.kernel;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;

public class DefaultMutationEngine implements MutationEngine {
    private final DarwinEngine engine;

    public DefaultMutationEngine(DarwinEngine engine) {
        this.engine = engine;
    }

    @Override
    public List<BranchVariant> generateVariants(String goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory) throws Exception {
        return engine.generateVariants(goal, snapshot, failureMemory, trajectory);
    }
}
