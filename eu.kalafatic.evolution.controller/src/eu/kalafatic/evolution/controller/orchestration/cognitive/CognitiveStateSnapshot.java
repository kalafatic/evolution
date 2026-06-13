package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.List;

/**
 * Serialized snapshot of the cognitive state for event broadcasting.
 */
public class CognitiveStateSnapshot {
    private CapabilityType capability;
    private SessionIntent intent;
    private CognitiveDirection direction;
    private double confidence;
    private double velocity;
    private double stability;
    private List<CapabilityType> trajectory;
    private int depth;

    public CognitiveStateSnapshot() {}

    public CognitiveStateSnapshot(SessionCognitiveState state) {
        this.capability = state.getCurrentCapability();
        this.intent = state.getCurrentIntent();
        this.direction = state.getCurrentDirection();
        this.confidence = state.getConfidence();
        this.velocity = state.getVelocity();
        this.stability = state.getTrendStability();
        this.trajectory = state.getTrajectory();
        this.depth = state.getCognitiveDepth();
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
}
