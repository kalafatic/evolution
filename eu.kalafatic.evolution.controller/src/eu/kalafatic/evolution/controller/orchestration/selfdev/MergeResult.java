package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Result of a merge operation.
 */
public class MergeResult {
    public boolean merged;
    public String commitId;
    public String deploymentUrl;
    public String message;
    public long timestamp;
    
    public MergeResult() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public MergeResult(boolean merged, String commitId, String message) {
        this.merged = merged;
        this.commitId = commitId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "MergeResult{" +
                "merged=" + merged +
                ", commitId='" + commitId + '\'' +
                ", deploymentUrl='" + deploymentUrl + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
