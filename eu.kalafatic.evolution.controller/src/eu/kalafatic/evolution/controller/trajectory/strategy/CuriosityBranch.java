package eu.kalafatic.evolution.controller.trajectory.strategy;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class CuriosityBranch implements EvolutionBranch {
    @Override
    public BranchType getType() {
        return BranchType.CURIOSITY;
    }

    @Override
    public boolean isApplicable(TaskContext context) {
        // Curiosity activates when stability is high or we've reached a stable solution
        Boolean stable = (Boolean) context.getOrchestrationState().getMetadata().get("artifact_stable");
        return Boolean.TRUE.equals(stable);
    }

    @Override
    public double getPriority(TaskContext context) {
        return 0.7;
    }

    @Override
    public double getCost(TaskContext context) {
        return 0.4;
    }

    @Override
    public String getInstructions() {
        return "Focus: Post-solution enhancement opportunities. " +
               "Suggest unit tests, Javadoc improvements, executable packaging (JAR), integration examples, or quality improvements.";
    }
}
