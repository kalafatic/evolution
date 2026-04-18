package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates behavior control and modularity for the platform.
 */
public class PlatformMode {
    private PlatformType type;
    private AutonomyLevel autonomyLevel;
    private int iterationLimit;
    private boolean allowSelfModify;
    private List<String> allowedPaths = new ArrayList<>();

    public PlatformMode() {
        this.type = PlatformType.SIMPLE_CHAT;
        this.autonomyLevel = AutonomyLevel.LOW;
        this.iterationLimit = 1;
        this.allowSelfModify = false;
    }

    public PlatformMode(PlatformType type, AutonomyLevel autonomyLevel, int iterationLimit, boolean allowSelfModify) {
        this.type = type;
        this.autonomyLevel = autonomyLevel;
        this.iterationLimit = iterationLimit;
        this.allowSelfModify = allowSelfModify;
    }

    public PlatformType getType() {
        return type;
    }

    public void setType(PlatformType type) {
        this.type = type;
    }

    public AutonomyLevel getAutonomyLevel() {
        return autonomyLevel;
    }

    public void setAutonomyLevel(AutonomyLevel autonomyLevel) {
        this.autonomyLevel = autonomyLevel;
    }

    public int getIterationLimit() {
        return iterationLimit;
    }

    public void setIterationLimit(int iterationLimit) {
        this.iterationLimit = iterationLimit;
    }

    public boolean isAllowSelfModify() {
        return allowSelfModify;
    }

    public void setAllowSelfModify(boolean allowSelfModify) {
        this.allowSelfModify = allowSelfModify;
    }

    public List<String> getAllowedPaths() {
        return allowedPaths;
    }

    public void setAllowedPaths(List<String> allowedPaths) {
        this.allowedPaths = allowedPaths;
    }

    @Override
    public String toString() {
        return "PlatformMode{" +
                "type=" + type +
                ", autonomyLevel=" + autonomyLevel +
                ", iterationLimit=" + iterationLimit +
                ", allowSelfModify=" + allowSelfModify +
                '}';
    }
}
