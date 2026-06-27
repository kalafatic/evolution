package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;

public class IterationRecord {
    private int iteration;
    private String goal;
    private String strategy;
    private String strategyType;
    private List<BranchVariant.Action> actions;
    private BranchVariant.ExpectedEffect expectedEffect;
    private String branch;
    private List<String> changedFiles;
    private String result; // SUCCESS / FAIL
    private String errorMessage;
    private int attempt;
    private double score;
    private long timestamp;
    private String status;

    // Extended fields
    private String iterationId;
    private String branchId;
    private String lineageId;
    private String rank;
    private String activationState;
    private String semanticAnchor;
    private String mutationTrace;
    private String inheritedContext;
    private List<String> rejectedSiblings;
    private List<String> rejectionReasons;
    private String activeDimension;
    private List<String> lockedDimensions;

    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }

    public List<BranchVariant.Action> getActions() { return actions; }
    public void setActions(List<BranchVariant.Action> actions) { this.actions = actions; }

    public BranchVariant.ExpectedEffect getExpectedEffect() { return expectedEffect; }
    public void setExpectedEffect(BranchVariant.ExpectedEffect expectedEffect) { this.expectedEffect = expectedEffect; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public List<String> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
	
    public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

    public String getIterationId() { return iterationId; }
    public void setIterationId(String iterationId) { this.iterationId = iterationId; }

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getLineageId() { return lineageId; }
    public void setLineageId(String lineageId) { this.lineageId = lineageId; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }

    public String getActivationState() { return activationState; }
    public void setActivationState(String activationState) { this.activationState = activationState; }

    public String getSemanticAnchor() { return semanticAnchor; }
    public void setSemanticAnchor(String semanticAnchor) { this.semanticAnchor = semanticAnchor; }

    public String getMutationTrace() { return mutationTrace; }
    public void setMutationTrace(String mutationTrace) { this.mutationTrace = mutationTrace; }

    public String getInheritedContext() { return inheritedContext; }
    public void setInheritedContext(String inheritedContext) { this.inheritedContext = inheritedContext; }

    public List<String> getRejectedSiblings() { return rejectedSiblings; }
    public void setRejectedSiblings(List<String> rejectedSiblings) { this.rejectedSiblings = rejectedSiblings; }

    public List<String> getRejectionReasons() { return rejectionReasons; }
    public void setRejectionReasons(List<String> rejectionReasons) { this.rejectionReasons = rejectionReasons; }

    public String getActiveDimension() { return activeDimension; }
    public void setActiveDimension(String activeDimension) { this.activeDimension = activeDimension; }

    public List<String> getLockedDimensions() { return lockedDimensions; }
    public void setLockedDimensions(List<String> lockedDimensions) { this.lockedDimensions = lockedDimensions; }
}
