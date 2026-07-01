package eu.kalafatic.evolution.controller.orchestration.engines;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceDeltaAnalyzer;

public class ExecutionEngine {

    public void applyWinner(BranchVariant selectedVariant, TaskContext context) {
        // LOGICAL SYNC: Ensure files from variant actions are always recorded in UI panel
        for (BranchVariant.Action action : selectedVariant.getActions()) {
            if (("WRITE".equals(action.getOperation()) || "CREATE".equals(action.getOperation())) && action.getTarget() != null) {
                context.getFileChangeTracker().recordChange(action.getTarget(), FileChangeTracker.ChangeType.EDITED);
            } else if ("DELETE".equals(action.getOperation()) && action.getTarget() != null) {
                context.getFileChangeTracker().recordChange(action.getTarget(), FileChangeTracker.ChangeType.REMOVED);
            }
        }
    }

    public WorkspaceDeltaAnalyzer.DeltaAnalysis analyzeWorkspace(String baseCommit, TaskContext context) {
        WorkspaceDeltaAnalyzer analyzer = new WorkspaceDeltaAnalyzer(context.getProjectRoot(), context);
        return analyzer.analyze(baseCommit);
    }

    public void commitWinner(String completedPhase, BranchVariant selectedVariant, IterationManager manager, TaskContext context) throws Exception {
        manager.getGitManager().commit("Darwin Evolution Phase " + completedPhase, context);
    }
}
