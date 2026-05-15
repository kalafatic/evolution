package eu.kalafatic.evolution.controller.trajectory.strategy;

import java.util.ArrayList;
import java.util.List;

public class BranchRegistry {
    private static final List<EvolutionBranch> strategies = new ArrayList<>();

    static {
        strategies.add(new ImplementationBranch());
        strategies.add(new AnalyticalBranch());
        strategies.add(new CuriosityBranch());
        strategies.add(new StabilizationBranch());
        strategies.add(new ExplorationBranch());
    }

    public static List<EvolutionBranch> getAvailableStrategies() {
        return new ArrayList<>(strategies);
    }
}
