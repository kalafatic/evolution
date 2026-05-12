package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks long-running reasoning paths and stabilizes orchestration trajectories.
 */
public class TrajectoryMemory {
    private final List<String> successfulStrategies = new ArrayList<>();
    private final List<String> recurringFailureLoops = new ArrayList<>();
    private final List<String> userPreferredStyles = new ArrayList<>();
    private final List<String> architectureEvolutionHistory = new ArrayList<>();
    private final List<String> branchLineagePatterns = new ArrayList<>();

    private final Map<String, Integer> strategySuccessCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> strategyFailureCount = new ConcurrentHashMap<>();

    public void recordSuccessfulStrategy(String strategy) {
        if (!successfulStrategies.contains(strategy)) {
            successfulStrategies.add(strategy);
        }
        strategySuccessCount.merge(strategy, 1, Integer::sum);
    }

    public void recordFailureLoop(String failure) {
        if (!recurringFailureLoops.contains(failure)) {
            recurringFailureLoops.add(failure);
        }
        strategyFailureCount.merge(failure, 1, Integer::sum);
    }

    public void recordUserPreference(String preference) {
        if (!userPreferredStyles.contains(preference)) {
            userPreferredStyles.add(preference);
        }
    }

    public void recordArchitectureEvolution(String evolution) {
        architectureEvolutionHistory.add(evolution);
    }

    public void recordLineagePattern(String pattern) {
        branchLineagePatterns.add(pattern);
    }

    public List<String> getSuccessfulStrategies() {
        return successfulStrategies;
    }

    public List<String> getRecurringFailureLoops() {
        return recurringFailureLoops;
    }

    public List<String> getUserPreferredStyles() {
        return userPreferredStyles;
    }

    public List<String> getArchitectureEvolutionHistory() {
        return architectureEvolutionHistory;
    }

    public List<String> getBranchLineagePatterns() {
        return branchLineagePatterns;
    }

    public double getStrategyReliability(String strategy) {
        int success = strategySuccessCount.getOrDefault(strategy, 0);
        int failure = strategyFailureCount.getOrDefault(strategy, 0);
        if (success + failure == 0) return 0.5;
        return (double) success / (success + failure);
    }

    public Map<String, Integer> getStrategySuccessCount() {
        return strategySuccessCount;
    }

    public Map<String, Integer> getStrategyFailureCount() {
        return strategyFailureCount;
    }
}
