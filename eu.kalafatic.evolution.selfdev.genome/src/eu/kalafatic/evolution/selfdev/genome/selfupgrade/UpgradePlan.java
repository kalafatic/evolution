package eu.kalafatic.evolution.selfdev.genome.selfupgrade;

import java.util.List;

import eu.kalafatic.evolution.selfdev.genome.core.Mode;

public class UpgradePlan {

    private String planId;

    private Mode mode;

    private String sourceProject;

    private String targetProject;

    private List<FileChange> changes;

    private List<ArchitecturalChange> architectureChanges;

    private List<String> affectedModules;

    private List<String> reasoningSteps;

    private double expectedFitnessGain;

    private RiskLevel riskLevel;

    private ValidationHints validationHints;

    private long createdAt;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getSourceProject() {
        return sourceProject;
    }

    public void setSourceProject(String sourceProject) {
        this.sourceProject = sourceProject;
    }

    public String getTargetProject() {
        return targetProject;
    }

    public void setTargetProject(String targetProject) {
        this.targetProject = targetProject;
    }

    public List<FileChange> getChanges() {
        return changes;
    }

    public void setChanges(List<FileChange> changes) {
        this.changes = changes;
    }

    public List<ArchitecturalChange> getArchitectureChanges() {
        return architectureChanges;
    }

    public void setArchitectureChanges(List<ArchitecturalChange> architectureChanges) {
        this.architectureChanges = architectureChanges;
    }

    public List<String> getAffectedModules() {
        return affectedModules;
    }

    public void setAffectedModules(List<String> affectedModules) {
        this.affectedModules = affectedModules;
    }

    public List<String> getReasoningSteps() {
        return reasoningSteps;
    }

    public void setReasoningSteps(List<String> reasoningSteps) {
        this.reasoningSteps = reasoningSteps;
    }

    public double getExpectedFitnessGain() {
        return expectedFitnessGain;
    }

    public void setExpectedFitnessGain(double expectedFitnessGain) {
        this.expectedFitnessGain = expectedFitnessGain;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public ValidationHints getValidationHints() {
        return validationHints;
    }

    public void setValidationHints(ValidationHints validationHints) {
        this.validationHints = validationHints;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
