package eu.kalafatic.evolution.controller.trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Long-running adaptive behavior path (Competing Hypotheses).
 */
public class Trajectory {
    public enum Phase { EXPLORATION, EXPLOITATION, CONVERGENCE, COLLAPSE }

    private final String trajectoryId;
    private final String goalContext;
    private final List<SignalRecord> signalHistory = new ArrayList<>();
    private Phase phase = Phase.EXPLORATION;

    // Core Darwinian metrics
    private double fitnessScore = 0.5;
    private double stabilityScore = 0.5;
    private double confidenceLevel = 0.5;
    private double divergenceScore = 0.0;

    // Adaptive future forecasting
    private List<String> projectedSteps = new ArrayList<>();
    private String prosConsAnalysis;
    private String semanticJustification;
    private List<Double> fitnessHistory = new ArrayList<>();

    // Physical truth anchoring
    private String counterfactualDelta;

    // Darwinian trends
    public String testTrend = "STABLE";
    public String buildTrend = "STABLE";
    public String failureChange = "NONE";

    public Trajectory() {
        this("traj-" + System.currentTimeMillis(), "Autonomous Evolution");
    }

    public Trajectory(String trajectoryId, String goalContext) {
        this.trajectoryId = trajectoryId;
        this.goalContext = goalContext;
    }

    public String getTrajectoryId() { return trajectoryId; }
    public String getGoalContext() { return goalContext; }
    public List<SignalRecord> getSignalHistory() { return signalHistory; }
    public Phase getPhase() { return phase; }

    public double getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(double fitnessScore) { this.fitnessScore = fitnessScore; }

    public double getStabilityScore() { return stabilityScore; }
    public void setStabilityScore(double stabilityScore) { this.stabilityScore = stabilityScore; }

    public double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public double getDivergenceScore() { return divergenceScore; }
    public void setDivergenceScore(double divergenceScore) { this.divergenceScore = divergenceScore; }

    public List<String> getProjectedSteps() { return projectedSteps; }
    public void setProjectedSteps(List<String> projectedSteps) { this.projectedSteps = projectedSteps; }

    public String getProsConsAnalysis() { return prosConsAnalysis; }
    public void setProsConsAnalysis(String prosConsAnalysis) { this.prosConsAnalysis = prosConsAnalysis; }

    public String getSemanticJustification() { return semanticJustification; }
    public void setSemanticJustification(String semanticJustification) { this.semanticJustification = semanticJustification; }

    public List<Double> getFitnessHistory() { return fitnessHistory; }

    public String getCounterfactualDelta() { return counterfactualDelta; }
    public void setCounterfactualDelta(String counterfactualDelta) { this.counterfactualDelta = counterfactualDelta; }

    public void setPhase(Phase phase) { this.phase = phase; }

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
