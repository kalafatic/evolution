package eu.kalafatic.evolution.controller.orchestration.selfdev;

//============================================================
// RESULT CLASSES
// ============================================================

/**
 * Result of a branch creation operation.
 */
public class BranchResult {
	public boolean success;
	public String branchName;
	public String commitId;
	public String message;
	public long timestamp;

	public BranchResult() {
		this.timestamp = System.currentTimeMillis();
	}

	public BranchResult(boolean success, String branchName, String message) {
		this.success = success;
		this.branchName = branchName;
		this.message = message;
		this.timestamp = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "BranchResult{" + "success=" + success + ", branchName='" + branchName + '\'' + ", commitId='" + commitId
				+ '\'' + ", message='" + message + '\'' + '}';
	}
}
