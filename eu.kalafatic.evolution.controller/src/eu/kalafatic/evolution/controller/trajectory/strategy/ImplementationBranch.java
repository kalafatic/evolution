package eu.kalafatic.evolution.controller.trajectory.strategy;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class ImplementationBranch implements EvolutionBranch {
    @Override
    public BranchType getType() {
        return BranchType.IMPLEMENTATION;
    }

    @Override
    public boolean isApplicable(TaskContext context) {
        // Implementation is almost always applicable unless we are purely in a curiosity phase
        return true;
    }

    @Override
    public double getPriority(TaskContext context) {
        return 1.0; // High priority for base implementation
    }

    @Override
    public double getCost(TaskContext context) {
        return 0.5;
    }

    @Override
    public String getInstructions() {
        return "Focus: Completing missing logic, correctness, functional completion. " +
               "Ensure the requested feature is fully implemented according to requirements.";
    }
}
