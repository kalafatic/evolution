package eu.kalafatic.evolution.controller.orchestration.capability;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

public class CapabilityHealth {
    private final double stabilityScore;
    private final String statusMessage;
    private final long latencyMs;

    public CapabilityHealth(double stabilityScore, String statusMessage, long latencyMs) {
        this.stabilityScore = stabilityScore;
        this.statusMessage = statusMessage;
        this.latencyMs = latencyMs;
    }

    public double getStabilityScore() {
        return stabilityScore;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public long getLatencyMs() {
        return latencyMs;
    }
}
