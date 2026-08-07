package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Thrown when the engine needs to pause and wait for user approval.
 * This is NOT an error - it's a signal that the engine is waiting.
 */
public class PauseExecutionException extends Exception {
    private final String approvalId;

    public PauseExecutionException(String approvalId) {
        super("Waiting for approval: " + approvalId);
        this.approvalId = approvalId;
    }

    public String getApprovalId() { return approvalId; }
}