package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Result of a rollback operation.
 */
class RollbackResult {
    public boolean success;
    public String message;
    public long timestamp;
    public String rollbackCommitId;
    
    public RollbackResult() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public RollbackResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "RollbackResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", rollbackCommitId='" + rollbackCommitId + '\'' +
                '}';
    }
}
