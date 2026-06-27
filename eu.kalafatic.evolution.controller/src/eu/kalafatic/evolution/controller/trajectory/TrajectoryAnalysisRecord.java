package eu.kalafatic.evolution.controller.trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured record for evolutionary memory and trajectory analysis.
 */
public class TrajectoryAnalysisRecord {
    private String iterationId;
    private String branchId;
    private String ancestorId;
    private String strategy;
    private double fitnessScore;
    private double scoreDelta;
    private List<String> failurePatterns = new ArrayList<>();
    private List<String> successPatterns = new ArrayList<>();
    private double convergenceVector;
    private double noveltyScore;
    private long timestamp;

    public TrajectoryAnalysisRecord() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getIterationId() { return iterationId; }
    public void setIterationId(String iterationId) { this.iterationId = iterationId; }

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getAncestorId() { return ancestorId; }
    public void setAncestorId(String ancestorId) { this.ancestorId = ancestorId; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public double getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(double fitnessScore) { this.fitnessScore = fitnessScore; }

    public double getScoreDelta() { return scoreDelta; }
    public void setScoreDelta(double scoreDelta) { this.scoreDelta = scoreDelta; }

    public List<String> getFailurePatterns() { return failurePatterns; }
    public void setFailurePatterns(List<String> failurePatterns) { this.failurePatterns = failurePatterns; }

    public List<String> getSuccessPatterns() { return successPatterns; }
    public void setSuccessPatterns(List<String> successPatterns) { this.successPatterns = successPatterns; }

    public double getConvergenceVector() { return convergenceVector; }
    public void setConvergenceVector(double convergenceVector) { this.convergenceVector = convergenceVector; }

    public double getNoveltyScore() { return noveltyScore; }
    public void setNoveltyScore(double noveltyScore) { this.noveltyScore = noveltyScore; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
