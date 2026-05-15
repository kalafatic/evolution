package eu.kalafatic.evolution.controller.trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Long-running adaptive behavior path.
 */
public class Trajectory {
    public enum Phase { EXPLORATION, EXPLOITATION, CONVERGENCE, COLLAPSE }

    private final String trajectoryId;
    private final String goalContext;
    private final List<SignalRecord> signalHistory = new ArrayList<>();
    private Phase phase = Phase.EXPLORATION;
    private double stabilityScore = 0.5;
    private double divergenceScore = 0.0;

    // Legacy fields for compatibility during transition
    public String buildTrend = "STABLE";
    public String testTrend = "STABLE";
    public String failureChange = "NONE";

    public Trajectory() {
        this("default", "none");
    }

    public Trajectory(String trajectoryId, String goalContext) {
        this.trajectoryId = trajectoryId;
        this.goalContext = goalContext;
    }

    public String getTrajectoryId() { return trajectoryId; }
    public String getGoalContext() { return goalContext; }
    public List<SignalRecord> getSignalHistory() { return signalHistory; }
    public Phase getPhase() { return phase; }
    public double getStabilityScore() { return stabilityScore; }
    public double getDivergenceScore() { return divergenceScore; }

    public void setPhase(Phase phase) { this.phase = phase; }
    public void setStabilityScore(double stabilityScore) { this.stabilityScore = stabilityScore; }
    public void setDivergenceScore(double divergenceScore) { this.divergenceScore = divergenceScore; }

    public void recordSignal(String signalName, Object value) {
        signalHistory.add(new SignalRecord(signalName, value, System.currentTimeMillis()));
    }

    public static class SignalRecord {
        public final String signalName;
        public final Object value;
        public final long timestamp;

        public SignalRecord(String signalName, Object value, long timestamp) {
            this.signalName = signalName;
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
