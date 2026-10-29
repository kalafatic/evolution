package eu.kalafatic.evolution.controller.orchestration.adapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalized data record representing a single process unit (e.g. Iteration, Epoch, or Workflow Step)
 * across different underlying engine process types.
 */
public class ProcessUnitRecord {
    private int unitNumber;
    private String unitType = "ITERATION";
    private String unitName = "";
    private String goal = "";
    private String status = "READY";
    private String result = "N/A";
    private double score = 0.0;
    private String metricsSummary = "";
    private String errorMessage = "";
    private List<BranchRecord> branches = new ArrayList<>();
    private List<String> changedFiles = new ArrayList<>();
    private List<String> flowSteps = new ArrayList<>();
    private int activeFlowStepIndex = -1;

    public static class BranchRecord {
        private String branch;
        private String strategy;
        private String result;
        private double score;
        private String errorMessage;
        private List<String> changedFiles = new ArrayList<>();

        public BranchRecord(String branch, String strategy, String result, double score) {
            this.branch = branch;
            this.strategy = strategy;
            this.result = result;
            this.score = score;
        }

        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<String> getChangedFiles() { return changedFiles; }
        public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }
    }

    public ProcessUnitRecord(int unitNumber, String unitName) {
        this.unitNumber = unitNumber;
        this.unitName = unitName;
    }

    public int getUnitNumber() { return unitNumber; }
    public void setUnitNumber(int unitNumber) { this.unitNumber = unitNumber; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getMetricsSummary() { return metricsSummary; }
    public void setMetricsSummary(String metricsSummary) { this.metricsSummary = metricsSummary; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public List<BranchRecord> getBranches() { return branches; }
    public void setBranches(List<BranchRecord> branches) { this.branches = branches; }
    public List<String> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }
    public List<String> getFlowSteps() { return flowSteps; }
    public void setFlowSteps(List<String> flowSteps) { this.flowSteps = flowSteps; }
    public int getActiveFlowStepIndex() { return activeFlowStepIndex; }
    public void setActiveFlowStepIndex(int activeFlowStepIndex) { this.activeFlowStepIndex = activeFlowStepIndex; }
}
