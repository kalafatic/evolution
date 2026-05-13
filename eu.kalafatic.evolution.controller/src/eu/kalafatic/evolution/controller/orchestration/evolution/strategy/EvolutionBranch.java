package eu.kalafatic.evolution.controller.orchestration.evolution.strategy;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Strategy interface representing a cognitive branch of evolution.
 */
public interface EvolutionBranch {
    enum BranchType {
        IMPLEMENTATION,
        ANALYTICAL,
        CURIOSITY,
        STABILIZATION,
        EXPLORATION
    }

    BranchType getType();

    /**
     * Evaluates if this strategy is applicable given the current context.
     */
    boolean isApplicable(TaskContext context);

    /**
     * Returns the base priority score for this branch in the current context.
     */
    double getPriority(TaskContext context);

    /**
     * Returns the estimated cost of executing this branch.
     */
    double getCost(TaskContext context);

    /**
     * Provides the specific instructions for the LLM for this strategy.
     */
    String getInstructions();
}
