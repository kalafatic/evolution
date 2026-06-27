package eu.kalafatic.evolution.controller.supervision;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.workspace.TrajectoryMemory;

/**
 * Resolver policy that penalizes variants using strategies with historical high failure rates.
 */
public class TrajectoryStabilityPolicy implements ResolverPolicy {
    private final TrajectoryMemory memory;

    public TrajectoryStabilityPolicy(TrajectoryMemory memory) {
        this.memory = memory;
    }

    @Override
    public PolicyResult evaluate(BranchVariant variant) {
        if (memory == null) return new PolicyResult(0.5, 0.5, "Trajectory memory not available.");
        double reliability = memory.getStrategyReliability(variant.getStrategy());
        return new PolicyResult(reliability, 0.9, "Historical reliability score for strategy: " + variant.getStrategy());
    }

    @Override
    public String getName() {
        return "TrajectoryStabilityPolicy";
    }
}
