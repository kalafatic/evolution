package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialized snapshot of the cognitive state for persistence and event broadcasting.
 */
public class SessionCognitiveSnapshot {
    private CapabilityType capability;
    private SessionIntent intent;
    private CognitiveDirection direction;
    private double confidence;
    private double velocity;
    private double stability;
    private List<CapabilityType> trajectory;
    private int depth;
    private Map<CapabilityType, Double> scores;
    private List<CapabilitySignal> history;

    public SessionCognitiveSnapshot() {}

    public SessionCognitiveSnapshot(SessionCognitiveState state) {
        this.capability = state.getCurrentCapability();
        this.intent = state.getCurrentIntent();
        this.direction = state.getCurrentDirection();
        this.confidence = state.getConfidence();
        this.velocity = state.getVelocity();
        this.stability = state.getTrendStability();
        this.trajectory = new ArrayList<>(state.getTrajectory());
        this.depth = state.getCognitiveDepth();
        this.scores = new HashMap<>(state.getCapabilityScores());
        this.history = new ArrayList<>(state.getCapabilityHistory());
    }

    public CapabilityType getCapability() { return capability; }
    public void setCapability(CapabilityType capability) { this.capability = capability; }

    public SessionIntent getIntent() { return intent; }
    public void setIntent(SessionIntent intent) { this.intent = intent; }

    public CognitiveDirection getDirection() { return direction; }
    public void setDirection(CognitiveDirection direction) { this.direction = direction; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public double getVelocity() { return velocity; }
    public void setVelocity(double velocity) { this.velocity = velocity; }

    public double getStability() { return stability; }
    public void setStability(double stability) { this.stability = stability; }

    public List<CapabilityType> getTrajectory() { return trajectory; }
    public void setTrajectory(List<CapabilityType> trajectory) { this.trajectory = trajectory; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public Map<CapabilityType, Double> getScores() { return scores; }
    public void setScores(Map<CapabilityType, Double> scores) { this.scores = scores; }

    public List<CapabilitySignal> getHistory() { return history; }
    public void setHistory(List<CapabilitySignal> history) { this.history = history; }
}
