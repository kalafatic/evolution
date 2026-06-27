package eu.kalafatic.evolution.controller.trajectory.strategy;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class StabilizationBranch implements EvolutionBranch {
    @Override
    public BranchType getType() {
        return BranchType.STABILIZATION;
    }

    @Override
    public boolean isApplicable(TaskContext context) {
        // Active when risk is high or many build errors
        Double risk = (Double) context.getOrchestrationState().getMetadata().get("structural_risk");
        return (risk != null && risk > 0.6);
    }

    @Override
    public double getPriority(TaskContext context) {
        Double risk = (Double) context.getOrchestrationState().getMetadata().get("structural_risk");
        return risk != null ? risk : 0.5;
    }

    @Override
    public double getCost(TaskContext context) {
        return 0.2;
    }

    @Override
    public String getInstructions() {
        return "Focus: Simplification, cleanup, consistency enforcement, redundancy removal. " +
               "Refactor the code to improve maintainability and ensure it follows existing patterns.";
    }
}
