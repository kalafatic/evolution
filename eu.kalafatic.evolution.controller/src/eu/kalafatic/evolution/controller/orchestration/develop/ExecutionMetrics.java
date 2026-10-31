package eu.kalafatic.evolution.controller.orchestration.develop;

import java.util.ArrayList;
import java.util.List;

/**
 * Granular metrics and output details for an execution (build or test).
 */
public class ExecutionMetrics {
    private String operation; // BUILD or TEST
    private String command;
    private long startedAt;
    private long completedAt;
    private long durationMs;
    private int exitCode;
    private ExecutionStatus status = ExecutionStatus.NOT_AVAILABLE;
    private String stdout = "";
    private String stderr = "";
    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;
    private List<String> failedTestNames = new ArrayList<>();

    public ExecutionMetrics() {
    }

    public ExecutionMetrics(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
        if (this.startedAt > 0) {
            this.durationMs = completedAt - startedAt;
        }
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout != null ? stdout : "";
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr != null ? stderr : "";
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public List<String> getFailedTestNames() {
        return failedTestNames;
    }

    public void setFailedTestNames(List<String> failedTestNames) {
        this.failedTestNames = (failedTestNames != null) ? failedTestNames : new ArrayList<>();
    }
}
