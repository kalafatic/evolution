package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class BranchVariant {
    private String branchName;
    private String strategy; // description of approach
    private double score;

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
