package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;

/**
 * Event containing real-time progress of an evolutionary iteration.
 */
public class EvolutionProgressEvent {
    private String sessionId;
    private int iterationCount;
    private int generation;
    private String lineage;
    private EvolutionStage stage;
    private int completedSteps;
    private int totalSteps;
    private String currentBranch;
    private String currentModel;
    private String currentTask;
    private String goal;
    private String winnerId;
    private String parentId;
    private String currentDimension;
    private String currentDimensionDescription;
    private String platformType;
    private int lockedDecisionCount;
    private boolean autoApprove;
    private boolean gitAutomation;
    private boolean stepMode;
    private int maxIterations;
    private int minIterations;
    private int branchingLimit;
    private int minBranchingLimit;
    private long timestamp;
    private long startTime;
    private List<BranchStatus> branchStatuses = new ArrayList<>();

    public EvolutionProgressEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }

    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }

    public String getLineage() { return lineage; }
    public void setLineage(String lineage) { this.lineage = lineage; }

    public EvolutionStage getStage() { return stage; }
    public void setStage(EvolutionStage stage) { this.stage = stage; }

    public int getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public String getCurrentBranch() { return currentBranch; }
    public void setCurrentBranch(String currentBranch) { this.currentBranch = currentBranch; }

    public String getCurrentModel() { return currentModel; }
    public void setCurrentModel(String currentModel) { this.currentModel = currentModel; }

    public String getCurrentTask() { return currentTask; }
    public void setCurrentTask(String currentTask) { this.currentTask = currentTask; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getCurrentDimension() { return currentDimension; }
    public void setCurrentDimension(String currentDimension) { this.currentDimension = currentDimension; }

    public String getCurrentDimensionDescription() { return currentDimensionDescription; }
    public void setCurrentDimensionDescription(String currentDimensionDescription) { this.currentDimensionDescription = currentDimensionDescription; }

    public String getPlatformType() { return platformType; }
    public void setPlatformType(String platformType) { this.platformType = platformType; }

    public int getLockedDecisionCount() { return lockedDecisionCount; }
    public void setLockedDecisionCount(int lockedDecisionCount) { this.lockedDecisionCount = lockedDecisionCount; }

    public boolean isAutoApprove() { return autoApprove; }
    public void setAutoApprove(boolean autoApprove) { this.autoApprove = autoApprove; }

    public boolean isGitAutomation() { return gitAutomation; }
    public void setGitAutomation(boolean gitAutomation) { this.gitAutomation = gitAutomation; }

    public boolean isStepMode() { return stepMode; }
    public void setStepMode(boolean stepMode) { this.stepMode = stepMode; }

    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }

    public int getMinIterations() { return minIterations; }
    public void setMinIterations(int minIterations) { this.minIterations = minIterations; }

    public int getBranchingLimit() { return branchingLimit; }
    public void setBranchingLimit(int branchingLimit) { this.branchingLimit = branchingLimit; }

    public int getMinBranchingLimit() { return minBranchingLimit; }
    public void setMinBranchingLimit(int minBranchingLimit) { this.minBranchingLimit = minBranchingLimit; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public List<BranchStatus> getBranchStatuses() { return branchStatuses; }
    public void setBranchStatuses(List<BranchStatus> branchStatuses) { this.branchStatuses = branchStatuses; }

    public static class BranchStatus {
        private String id;
        private String strategy;
        private String status; // waiting, active, complete, failed
        private Double score;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
