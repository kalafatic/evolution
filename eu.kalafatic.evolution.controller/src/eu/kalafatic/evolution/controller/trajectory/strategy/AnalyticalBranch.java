package eu.kalafatic.evolution.controller.trajectory.strategy;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class AnalyticalBranch implements EvolutionBranch {
    @Override
    public BranchType getType() {
        return BranchType.ANALYTICAL;
    }

    @Override
    public boolean isApplicable(TaskContext context) {
        Object epsObj = context.getOrchestrationState().getMetadata().get("eps");
        double eps = (epsObj instanceof Double) ? (Double) epsObj : 0.5;
        return eps > 0.4; // More analytical when there is some pressure/uncertainty
    }

    @Override
    public double getPriority(TaskContext context) {
        Object epsObj = context.getOrchestrationState().getMetadata().get("eps");
        double eps = (epsObj instanceof Double) ? (Double) epsObj : 0.5;
        return eps;
    }

    @Override
    public double getCost(TaskContext context) {
        return 0.3;
    }

    @Override
    public String getInstructions() {
        return "Focus: Architecture understanding, dependency analysis, risk detection. " +
               "Analyze how the changes affect the overall system stability and identify potential integration risks.";
    }
}
