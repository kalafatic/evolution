package eu.kalafatic.evolution.controller.orchestration.intent;

/**
 * Represents the evolutionary complexity of a task, replacing the binary requiresPlanning signal.
 */
public class EvolutionaryComplexityVector {

    public double ambiguity;

    public double branchability;

    public double semanticUncertainty;

    public double dependencyExpansionRisk;

    public double architecturePressure;

    public double recursionPotential;

    public double determinismConfidence;

    @Override
    public String toString() {
        return String.format("EvolutionaryComplexityVector[ambiguity=%.2f, branchability=%.2f, uncertainty=%.2f, risk=%.2f, pressure=%.2f, recursion=%.2f, confidence=%.2f]",
            ambiguity, branchability, semanticUncertainty, dependencyExpansionRisk, architecturePressure, recursionPotential, determinismConfidence);
    }
}
