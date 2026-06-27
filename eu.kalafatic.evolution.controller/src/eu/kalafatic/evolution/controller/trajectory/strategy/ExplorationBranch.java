package eu.kalafatic.evolution.controller.trajectory.strategy;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class ExplorationBranch implements EvolutionBranch {
    @Override
    public BranchType getType() {
        return BranchType.EXPLORATION;
    }

    @Override
    public boolean isApplicable(TaskContext context) {
        Object epsObj = context.getOrchestrationState().getMetadata().get("eps");
        double eps = (epsObj instanceof Double) ? (Double) epsObj : 0.5;
        return eps > 0.7; // High novelty/uncertainty triggers exploration
    }

    @Override
    public double getPriority(TaskContext context) {
        Object epsObj = context.getOrchestrationState().getMetadata().get("eps");
        double eps = (epsObj instanceof Double) ? (Double) epsObj : 0.5;
        return eps * 0.8;
    }

    @Override
    public double getCost(TaskContext context) {
        return 0.8;
    }

    @Override
    public String getInstructions() {
        return "Focus: Alternative architectures, divergent thinking, research expansion. " +
               "Propose a significantly different approach to the problem to explore the design space.";
    }
}
