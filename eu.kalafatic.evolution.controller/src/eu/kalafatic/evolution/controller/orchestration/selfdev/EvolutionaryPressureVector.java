package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Quantifies the evolutionary pressure acting on a dimension or branch.
 */
public class EvolutionaryPressureVector {
    @com.fasterxml.jackson.annotation.JsonProperty("ambiguity")
    public double ambiguity;
    @com.fasterxml.jackson.annotation.JsonProperty("extensibility")
    public double extensibility;
    @com.fasterxml.jackson.annotation.JsonProperty("scalability")
    public double scalability;
    @com.fasterxml.jackson.annotation.JsonProperty("failureExposure")
    public double failureExposure;
    @com.fasterxml.jackson.annotation.JsonProperty("implementationUncertainty")
    public double implementationUncertainty;
    @com.fasterxml.jackson.annotation.JsonProperty("dependencyComplexity")
    public double dependencyComplexity;
    @com.fasterxml.jackson.annotation.JsonProperty("integrationInstability")
    public double integrationInstability;
    @com.fasterxml.jackson.annotation.JsonProperty("concurrencyPressure")
    public double concurrencyPressure;
    @com.fasterxml.jackson.annotation.JsonProperty("performanceSensitivity")
    public double performanceSensitivity;

    public double getTotalPressure() {
        return (ambiguity + extensibility + scalability + failureExposure + implementationUncertainty +
                dependencyComplexity + integrationInstability + concurrencyPressure + performanceSensitivity) / 9.0;
    }
}
