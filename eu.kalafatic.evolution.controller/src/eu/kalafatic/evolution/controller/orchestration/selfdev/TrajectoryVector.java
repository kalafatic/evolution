package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Represents a trajectory as a point in multidimensional evolutionary space.
 * Each dimension is a discrete categorical bin from 0 to 3.
 */
public class TrajectoryVector {
    private int modularity = 1;
    private int resilience = 1;
    private int architecturalDepth = 1;
    private int serviceOrientation = 1;
    private int persistence = 1;
    private int determinism = 1;
    private int extensibility = 1;
    private int coupling = 1;
    private int abstraction = 1;
    private int riskAcceptance = 1;

    public TrajectoryVector() {}

    /**
     * Calculates weighted Euclidean distance between this vector and another.
     * Normalized to 0.0-1.0 range.
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

        return Math.sqrt(sum) / Math.sqrt(90.0); // Normalized (max sum is 10 * 3^2 = 90)
    }

    /**
     * Counts the number of unique axes that differ between this vector and another.
     */
    public int countAxisDifferences(TrajectoryVector other) {
        if (other == null) return 10;
        int count = 0;
        if (this.modularity != other.modularity) count++;
        if (this.resilience != other.resilience) count++;
        if (this.architecturalDepth != other.architecturalDepth) count++;
        if (this.serviceOrientation != other.serviceOrientation) count++;
        if (this.persistence != other.persistence) count++;
        if (this.determinism != other.determinism) count++;
        if (this.extensibility != other.extensibility) count++;
        if (this.coupling != other.coupling) count++;
        if (this.abstraction != other.abstraction) count++;
        if (this.riskAcceptance != other.riskAcceptance) count++;
        return count;
    }

    // Getters and Setters
    public int getModularity() { return modularity; }
    public void setModularity(int modularity) { this.modularity = modularity; }

    public int getResilience() { return resilience; }
    public void setResilience(int resilience) { this.resilience = resilience; }

    public int getArchitecturalDepth() { return architecturalDepth; }
    public void setArchitecturalDepth(int architecturalDepth) { this.architecturalDepth = architecturalDepth; }

    public int getServiceOrientation() { return serviceOrientation; }
    public void setServiceOrientation(int serviceOrientation) { this.serviceOrientation = serviceOrientation; }

    public int getPersistence() { return persistence; }
    public void setPersistence(int persistence) { this.persistence = persistence; }

    public int getDeterminism() { return determinism; }
    public void setDeterminism(int determinism) { this.determinism = determinism; }

    public int getExtensibility() { return extensibility; }
    public void setExtensibility(int extensibility) { this.extensibility = extensibility; }

    public int getCoupling() { return coupling; }
    public void setCoupling(int coupling) { this.coupling = coupling; }

    public int getAbstraction() { return abstraction; }
    public void setAbstraction(int abstraction) { this.abstraction = abstraction; }

    public int getRiskAcceptance() { return riskAcceptance; }
    public void setRiskAcceptance(int riskAcceptance) { this.riskAcceptance = riskAcceptance; }

    @Override
    public String toString() {
        return String.format("TV[mod=%d, res=%d, depth=%d, svc=%d, pers=%d, det=%d, ext=%d, coup=%d, abs=%d, risk=%d]",
            modularity, resilience, architecturalDepth, serviceOrientation, persistence,
            determinism, extensibility, coupling, abstraction, riskAcceptance);
    }
}
