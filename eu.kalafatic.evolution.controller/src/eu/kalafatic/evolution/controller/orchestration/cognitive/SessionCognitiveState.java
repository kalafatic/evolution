package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the holistic cognitive state of a session.
 */
public class SessionCognitiveState {
    private CapabilityType currentCapability = CapabilityType.CHAT;
    private SessionIntent currentIntent = SessionIntent.LEARNING;
    private CognitiveDirection currentDirection = CognitiveDirection.STABLE;
    private double confidence = 1.0;
    private List<CapabilityType> trajectory = new ArrayList<>();
    private Map<CapabilityType, Double> capabilityScores = new HashMap<>();
    private List<CapabilitySignal> capabilityHistory = new ArrayList<>();

    // Trajectory Metrics
    private double velocity = 0.0;
    private double acceleration = 0.0;
    private CapabilityType dominantTrend = CapabilityType.CHAT;
    private double trendStability = 1.0;
    private int cognitiveDepth = 1;

    public SessionCognitiveState() {
        for (CapabilityType type : CapabilityType.values()) {
            capabilityScores.put(type, 0.0);
        }
    }

    public CapabilityType getCurrentCapability() { return currentCapability; }
    public void setCurrentCapability(CapabilityType currentCapability) { this.currentCapability = currentCapability; }

    public SessionIntent getCurrentIntent() { return currentIntent; }
    public void setCurrentIntent(SessionIntent currentIntent) { this.currentIntent = currentIntent; }

    public CognitiveDirection getCurrentDirection() { return currentDirection; }
    public void setCurrentDirection(CognitiveDirection currentDirection) { this.currentDirection = currentDirection; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public List<CapabilityType> getTrajectory() { return trajectory; }
    public void setTrajectory(List<CapabilityType> trajectory) { this.trajectory = trajectory; }

    public Map<CapabilityType, Double> getCapabilityScores() { return capabilityScores; }
    public void setCapabilityScores(Map<CapabilityType, Double> capabilityScores) { this.capabilityScores = capabilityScores; }

    public List<CapabilitySignal> getCapabilityHistory() { return capabilityHistory; }
    public void setCapabilityHistory(List<CapabilitySignal> capabilityHistory) { this.capabilityHistory = capabilityHistory; }

    public double getVelocity() { return velocity; }
    public void setVelocity(double velocity) { this.velocity = velocity; }

    public double getAcceleration() { return acceleration; }
    public void setAcceleration(double acceleration) { this.acceleration = acceleration; }

    public CapabilityType getDominantTrend() { return dominantTrend; }
    public void setDominantTrend(CapabilityType dominantTrend) { this.dominantTrend = dominantTrend; }

    public double getTrendStability() { return trendStability; }
    public void setTrendStability(double trendStability) { this.trendStability = trendStability; }

    public int getCognitiveDepth() { return cognitiveDepth; }
    public void setCognitiveDepth(int cognitiveDepth) { this.cognitiveDepth = cognitiveDepth; }

    public void addSignal(CapabilitySignal signal) {
        capabilityHistory.add(signal);
        if (capabilityHistory.size() > 20) {
            capabilityHistory.remove(0);
        }
    }
}
