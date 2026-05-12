package eu.kalafatic.evolution.controller.orchestration.diagnostics;

import java.util.HashMap;
import java.util.Map;

/**
 * System-level reasoning health indicators.
 */
public final class CognitiveHealth {
    private double ambiguityPressure;
    private double mutationStability;
    private double contextCoherence;
    private double signalNoiseRatio;
    private double schedulerSaturation;
    private double branchEntropy;
    private double resolverConfidenceConsistency;

    public double getAmbiguityPressure() { return ambiguityPressure; }
    public void setAmbiguityPressure(double ambiguityPressure) { this.ambiguityPressure = ambiguityPressure; }

    public double getMutationStability() { return mutationStability; }
    public void setMutationStability(double mutationStability) { this.mutationStability = mutationStability; }

    public double getContextCoherence() { return contextCoherence; }
    public void setContextCoherence(double contextCoherence) { this.contextCoherence = contextCoherence; }

    public double getSignalNoiseRatio() { return signalNoiseRatio; }
    public void setSignalNoiseRatio(double signalNoiseRatio) { this.signalNoiseRatio = signalNoiseRatio; }

    public double getSchedulerSaturation() { return schedulerSaturation; }
    public void setSchedulerSaturation(double schedulerSaturation) { this.schedulerSaturation = schedulerSaturation; }

    public double getBranchEntropy() { return branchEntropy; }
    public void setBranchEntropy(double branchEntropy) { this.branchEntropy = branchEntropy; }

    public double getResolverConfidenceConsistency() { return resolverConfidenceConsistency; }
    public void setResolverConfidenceConsistency(double resolverConfidenceConsistency) { this.resolverConfidenceConsistency = resolverConfidenceConsistency; }

    public Map<String, Double> toMap() {
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("ambiguityPressure", ambiguityPressure);
        metrics.put("mutationStability", mutationStability);
        metrics.put("contextCoherence", contextCoherence);
        metrics.put("signalNoiseRatio", signalNoiseRatio);
        metrics.put("schedulerSaturation", schedulerSaturation);
        metrics.put("branchEntropy", branchEntropy);
        metrics.put("resolverConfidenceConsistency", resolverConfidenceConsistency);
        return metrics;
    }

    @Override
    public String toString() {
        return "CognitiveHealth" + toMap().toString();
    }
}
