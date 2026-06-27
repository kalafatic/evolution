package eu.kalafatic.evolution.controller.supervision;

/**
 * Immutable record of an authority decision for the audit trail.
 */
public final class AuditRecord {
    private final long timestamp;
    private final String iterationId;
    private final String branchId;
    private final String authorityAction; // APPROVED, REJECTED, ACTIVATED, etc.
    private final String previousState;
    private final String newState;
    private final String actor; // AuthorityController, User, etc.
    private final String reason;
    private final String correlationId;

    public AuditRecord(String iterationId, String branchId, String authorityAction,
                       String previousState, String newState, String actor,
                       String reason, String correlationId) {
        this.timestamp = System.currentTimeMillis();
        this.iterationId = iterationId;
        this.branchId = branchId;
        this.authorityAction = authorityAction;
        this.previousState = previousState;
        this.newState = newState;
        this.actor = actor;
        this.reason = reason;
        this.correlationId = correlationId;
    }

    public long getTimestamp() { return timestamp; }
    public String getIterationId() { return iterationId; }
    public String getBranchId() { return branchId; }
    public String getAuthorityAction() { return authorityAction; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public String getCorrelationId() { return correlationId; }

    @Override
    public String toString() {
        return String.format("[%d] %s: %s -> %s (Reason: %s)", timestamp, authorityAction, previousState, newState, reason);
    }
}
