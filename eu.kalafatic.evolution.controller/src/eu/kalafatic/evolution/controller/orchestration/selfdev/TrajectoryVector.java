package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Represents a trajectory as a point in multidimensional evolutionary space.
 * Each dimension is a continuous coefficient from 0.0 to 1.0.
 */
public class TrajectoryVector {
    private double modularity = 0.5;
    private double resilience = 0.5;
    private double architecturalDepth = 0.5;
    private double serviceOrientation = 0.5;
    private double persistence = 0.5;
    private double determinism = 0.5;
    private double extensibility = 0.5;
    private double coupling = 0.5;
    private double abstraction = 0.5;
    private double riskAcceptance = 0.5;

    public TrajectoryVector() {}

    /**
     * Calculates weighted Euclidean distance between this vector and another.
     */
    public double distance(TrajectoryVector other) {
        if (other == null) return 1.0;

        double sum = 0.0;
        sum += Math.pow(this.modularity - other.modularity, 2);
        sum += Math.pow(this.resilience - other.resilience, 2);
        sum += Math.pow(this.architecturalDepth - other.architecturalDepth, 2);
        sum += Math.pow(this.serviceOrientation - other.serviceOrientation, 2);
        sum += Math.pow(this.persistence - other.persistence, 2);
        sum += Math.pow(this.determinism - other.determinism, 2);
        sum += Math.pow(this.extensibility - other.extensibility, 2);
        sum += Math.pow(this.coupling - other.coupling, 2);
        sum += Math.pow(this.abstraction - other.abstraction, 2);
        sum += Math.pow(this.riskAcceptance - other.riskAcceptance, 2);

        return Math.sqrt(sum) / Math.sqrt(10.0); // Normalized to 0.0-1.0 range
    }

    // Getters and Setters
    public double getModularity() { return modularity; }
    public void setModularity(double modularity) { this.modularity = modularity; }

    public double getResilience() { return resilience; }
    public void setResilience(double resilience) { this.resilience = resilience; }

    public double getArchitecturalDepth() { return architecturalDepth; }
    public void setArchitecturalDepth(double architecturalDepth) { this.architecturalDepth = architecturalDepth; }

    public double getServiceOrientation() { return serviceOrientation; }
    public void setServiceOrientation(double serviceOrientation) { this.serviceOrientation = serviceOrientation; }

    public double getPersistence() { return persistence; }
    public void setPersistence(double persistence) { this.persistence = persistence; }

    public double getDeterminism() { return determinism; }
    public void setDeterminism(double determinism) { this.determinism = determinism; }

    public double getExtensibility() { return extensibility; }
    public void setExtensibility(double extensibility) { this.extensibility = extensibility; }

    public double getCoupling() { return coupling; }
    public void setCoupling(double coupling) { this.coupling = coupling; }

    public double getAbstraction() { return abstraction; }
    public void setAbstraction(double abstraction) { this.abstraction = abstraction; }

    public double getRiskAcceptance() { return riskAcceptance; }
    public void setRiskAcceptance(double riskAcceptance) { this.riskAcceptance = riskAcceptance; }

    @Override
    public String toString() {
        return String.format("TV[mod=%.2f, res=%.2f, depth=%.2f, svc=%.2f, pers=%.2f, det=%.2f, ext=%.2f, coup=%.2f, abs=%.2f, risk=%.2f]",
            modularity, resilience, architecturalDepth, serviceOrientation, persistence,
            determinism, extensibility, coupling, abstraction, riskAcceptance);
    }
}
