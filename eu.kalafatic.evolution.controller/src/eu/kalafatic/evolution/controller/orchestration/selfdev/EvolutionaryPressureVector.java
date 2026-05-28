package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Quantifies the evolutionary pressure acting on a dimension or branch.
 */
public class EvolutionaryPressureVector {
    public double ambiguity;
    public double extensibility;
    public double scalability;
    public double failureExposure;
    public double implementationUncertainty;
    public double dependencyComplexity;
    public double integrationInstability;
    public double concurrencyPressure;
    public double performanceSensitivity;

    public double getTotalPressure() {
        return (ambiguity + extensibility + scalability + failureExposure + implementationUncertainty +
                dependencyComplexity + integrationInstability + concurrencyPressure + performanceSensitivity) / 9.0;
    }
}
