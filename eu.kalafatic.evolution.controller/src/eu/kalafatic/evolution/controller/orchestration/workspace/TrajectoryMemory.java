package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks long-running reasoning paths and stabilizes orchestration trajectories.
 */
public class TrajectoryMemory {
    private final List<String> successfulStrategies = new ArrayList<>();
    private final List<String> recurringFailureLoops = new ArrayList<>();
    private final List<String> userPreferredStyles = new ArrayList<>();
    private final List<String> architectureEvolutionHistory = new ArrayList<>();
    private final List<String> branchLineagePatterns = new ArrayList<>();

    public void recordSuccessfulStrategy(String strategy) {
        if (!successfulStrategies.contains(strategy)) {
            successfulStrategies.add(strategy);
        }
    }

    public void recordFailureLoop(String failure) {
        if (!recurringFailureLoops.contains(failure)) {
            recurringFailureLoops.add(failure);
        }
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
}
