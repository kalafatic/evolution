package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;

public class IterationRecord {
    private int iteration;
    private String goal;
    private String strategy;
    private List<BranchVariant.Action> actions;
    private BranchVariant.ExpectedEffect expectedEffect;
    private String branch;
    private List<String> changedFiles;
    private String result; // SUCCESS / FAIL
    private String errorMessage;
    private int attempt;
    private double score;
    private long timestamp;

    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public List<BranchVariant.Action> getActions() { return actions; }
    public void setActions(List<BranchVariant.Action> actions) { this.actions = actions; }

    public BranchVariant.ExpectedEffect getExpectedEffect() { return expectedEffect; }
    public void setExpectedEffect(BranchVariant.ExpectedEffect expectedEffect) { this.expectedEffect = expectedEffect; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public List<String> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
