package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Collections;
import java.util.List;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class GitManager {
    private final File root;
    private final GitTool gitTool = new GitTool();

    public GitManager(File root) {
        this.root = root;
    }

    public boolean isGitRepository() {
        return new File(root, ".git").exists();
    }

    public void ensureInitialCommit() throws Exception {
        if (!isGitRepository()) {
            gitTool.execute("init", root, null);
            gitTool.execute("add .", root, null);
            gitTool.execute("commit -m \"Initial commit [EVO-SEED]\"", root, null);
        }
    }

    public String getCurrentBranch() throws Exception {
        return gitTool.execute("rev-parse --abbrev-ref HEAD", root, null).trim();
    }

    public void createBranch(String branchName) throws Exception {
        gitTool.execute("checkout -b " + branchName, root, null);
    }

    public void forceCheckout(String branchName) throws Exception {
        gitTool.execute("checkout -f " + branchName, root, null);
    }

    public void merge(String branchName) throws Exception {
        gitTool.execute("merge " + branchName, root, null);
    }

    public void commit(String message) throws Exception {
        commit(message, null);
    }

    public void commit(String message, TaskContext context) throws Exception {
        String fullMsg = message;
        if (context != null && context.getOrchestrationState() != null) {
            String iterId = context.getOrchestrationState().getCurrentIterationId();
            fullMsg += "\n\n[EVO-META] Iteration: " + iterId;
        }
        gitTool.execute("add .", root, context);
        gitTool.execute("commit --allow-empty -m \"" + fullMsg + "\"", root, context);
    }

    public void rollback() throws Exception {
        // Rollback just means cleaning up uncommitted changes in this context.
        // If we want to revert the last commit, it should be an explicit revert or reset --hard HEAD~1
        // But for DarwinFlow's general 'rollback on failure' before commit, reset --hard HEAD is safer.
        gitTool.execute("reset --hard HEAD", root, null);
    }

    public void createWorktree(String branch, String path) throws Exception {
        gitTool.execute("worktree add " + path + " " + branch, root, null);
    }

    public void removeWorktree(String path) throws Exception {
        gitTool.execute("worktree remove " + path, root, null);
    }
}
