package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Multi-dimensional fitness scoring for evolutionary branches.
 */
public class BranchFitnessVector {
    public double simplicity;
    public double extensibility;
    public double consistency;
    public double reversibility;
    public double implementationCost;
    public double entropyReduction;
    public double futureOptionality;
    public double dependencyStability;
    public double lineageCoherence;
    public double mutationTolerance;

    public double getAggregateScore() {
        return (simplicity + extensibility + consistency + reversibility + implementationCost +
                entropyReduction + futureOptionality + dependencyStability + lineageCoherence + mutationTolerance) / 10.0;
    }
}
