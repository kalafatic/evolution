package eu.kalafatic.evolution.controller.supervision;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryMemory;

/**
 * Resolver policy that penalizes variants using strategies with historical high failure rates.
 */
public class TrajectoryStabilityPolicy implements ResolverPolicy {
    private final TrajectoryMemory memory;

    public TrajectoryStabilityPolicy(TrajectoryMemory memory) {
        this.memory = memory;
    }

    @Override
    public double evaluate(BranchVariant variant) {
        if (memory == null) return 0.5;
        double reliability = memory.getStrategyReliability(variant.getStrategy());
        return reliability;
    }

    @Override
    public String getName() {
        return "TrajectoryStabilityPolicy";
    }
}
