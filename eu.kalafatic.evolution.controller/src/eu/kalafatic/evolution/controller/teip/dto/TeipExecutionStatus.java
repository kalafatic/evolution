package eu.kalafatic.evolution.controller.teip.dto;

/**
 * Stable external status model for TEIP executions.
 */
public enum TeipExecutionStatus {
    QUEUED,
    RUNNING,
    WAITING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
