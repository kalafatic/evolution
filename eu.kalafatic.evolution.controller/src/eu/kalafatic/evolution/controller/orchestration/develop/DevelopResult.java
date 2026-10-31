package eu.kalafatic.evolution.controller.orchestration.develop;

import java.util.ArrayList;
import java.util.List;

/**
 * Result outcome summary for a Develop task execution.
 */
public class DevelopResult {
    private DevelopTaskStatus status;
    private String summary;
    private List<String> changedFiles = new ArrayList<>();
    private String diff = "";
    private String buildResult = "";
    private String testResult = "";
    private String commitResult = "";
    private String error = "";
    private ExecutionMetrics buildMetrics;
    private ExecutionMetrics testMetrics;
    private String pullRequestUrl = "";
    private String deliveryStage = "CREATED";

    public DevelopResult() {
        this.status = DevelopTaskStatus.CREATED;
    }

    public ExecutionMetrics getBuildMetrics() {
        return buildMetrics;
    }

    public void setBuildMetrics(ExecutionMetrics buildMetrics) {
        this.buildMetrics = buildMetrics;
    }

    public ExecutionMetrics getTestMetrics() {
        return testMetrics;
    }

    public void setTestMetrics(ExecutionMetrics testMetrics) {
        this.testMetrics = testMetrics;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public void setPullRequestUrl(String pullRequestUrl) {
        this.pullRequestUrl = pullRequestUrl != null ? pullRequestUrl : "";
    }

    public String getDeliveryStage() {
        return deliveryStage;
    }

    public void setDeliveryStage(String deliveryStage) {
        this.deliveryStage = deliveryStage != null ? deliveryStage : "CREATED";
    }

    public DevelopTaskStatus getStatus() {
        return status;
    }

    public void setStatus(DevelopTaskStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<String> changedFiles) {
        this.changedFiles = (changedFiles != null) ? changedFiles : new ArrayList<>();
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = (diff != null) ? diff : "";
    }

    public String getBuildResult() {
        return buildResult;
    }

    public void setBuildResult(String buildResult) {
        this.buildResult = (buildResult != null) ? buildResult : "";
    }

    public String getTestResult() {
        return testResult;
    }

    public void setTestResult(String testResult) {
        this.testResult = (testResult != null) ? testResult : "";
    }

    public String getCommitResult() {
        return commitResult;
    }

    public void setCommitResult(String commitResult) {
        this.commitResult = (commitResult != null) ? commitResult : "";
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = (error != null) ? error : "";
    }
}
