package eu.kalafatic.evolution.controller.orchestration;

import java.util.List;
import java.util.Map;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;

/**
 * Full runtime state checkpoint for EVO sessions.
 */
public class Checkpoint {
    private String sessionId;
    private long timestamp;

    // PhaseState & GoalState
    private String currentPhase;
    private String rawInput;
    private int iterationCount;

    // FileChangeState
    private Map<String, FileChangeTracker.ChangeType> changedFiles;

    // BranchLineage
    private List<IterationRecord> activeLineage;
    private String currentIterationId;

    // EvaluatorState
    private StateSnapshot lastSnapshot;

    // RuntimeArtifacts
    private List<WorkspaceArtifact> artifacts;

    // SelectionState (Metadata & Decisions)
    private Map<String, Object> metadata;

    // CognitiveState
    private List<CausalNode> cognitiveTraceNodes;
    private Map<String, List<String>> rejectedBranches;
    private List<Double> entropyHistory;
    private List<EvolutionDimension> dimensions;

    // TrajectoryMemory
    private Map<String, Trajectory> trajectories;

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    public String getRawInput() { return rawInput; }
    public void setRawInput(String rawInput) { this.rawInput = rawInput; }

    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }

    public Map<String, FileChangeTracker.ChangeType> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(Map<String, FileChangeTracker.ChangeType> changedFiles) { this.changedFiles = changedFiles; }

    public List<IterationRecord> getActiveLineage() { return activeLineage; }
    public void setActiveLineage(List<IterationRecord> activeLineage) { this.activeLineage = activeLineage; }

    public String getCurrentIterationId() { return currentIterationId; }
    public void setCurrentIterationId(String currentIterationId) { this.currentIterationId = currentIterationId; }

    public StateSnapshot getLastSnapshot() { return lastSnapshot; }
    public void setLastSnapshot(StateSnapshot lastSnapshot) { this.lastSnapshot = lastSnapshot; }

    public List<WorkspaceArtifact> getArtifacts() { return artifacts; }
    public void setArtifacts(List<WorkspaceArtifact> artifacts) { this.artifacts = artifacts; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public List<CausalNode> getCognitiveTraceNodes() { return cognitiveTraceNodes; }
    public void setCognitiveTraceNodes(List<CausalNode> cognitiveTraceNodes) { this.cognitiveTraceNodes = cognitiveTraceNodes; }

    public Map<String, List<String>> getRejectedBranches() { return rejectedBranches; }
    public void setRejectedBranches(Map<String, List<String>> rejectedBranches) { this.rejectedBranches = rejectedBranches; }

    public List<Double> getEntropyHistory() { return entropyHistory; }
    public void setEntropyHistory(List<Double> entropyHistory) { this.entropyHistory = entropyHistory; }

    public List<EvolutionDimension> getDimensions() { return dimensions; }
    public void setDimensions(List<EvolutionDimension> dimensions) { this.dimensions = dimensions; }

    public Map<String, Trajectory> getTrajectories() { return trajectories; }
    public void setTrajectories(Map<String, Trajectory> trajectories) { this.trajectories = trajectories; }
}
