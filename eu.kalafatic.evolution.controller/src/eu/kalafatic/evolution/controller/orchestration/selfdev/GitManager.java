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
        }

        // Ensure HEAD exists (it won't in a fresh init without commits)
        try {
            gitTool.execute("rev-parse HEAD", root, null);
        } catch (Exception e) {
            // HEAD does not exist, create initial commit
            gitTool.execute("add .", root, null);
            gitTool.execute("commit --allow-empty -m \"Initial commit [EVO-SEED]\"", root, null);
        }
    }

    public String getCurrentBranch() throws Exception {
        try {
            return gitTool.execute("rev-parse --abbrev-ref HEAD", root, null).trim();
        } catch (Exception e) {
            // Fallback for new repositories without commits
            return gitTool.execute("symbolic-ref --short HEAD", root, null).trim();
        }
    }

    public String getHeadCommit() throws Exception {
        return gitTool.execute("rev-parse HEAD", root, null).trim();
    }

    public void createBranch(String branchName) throws Exception {
        try {
            gitTool.execute("rev-parse --verify " + branchName, root, null);
            // Branch exists, just checkout
            gitTool.execute("checkout " + branchName, root, null);
        } catch (Exception e) {
            // Branch doesn't exist, create it
            gitTool.execute("checkout -b " + branchName, root, null);
        }
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
            fullMsg += " [EVO-META] Iteration: " + iterId;
        }
        gitTool.execute("add .", root, context);
        gitTool.execute("commit --allow-empty -m \"" + fullMsg + "\"", root, context);
    }

    public void rollback() throws Exception {
        // Correcting regression: reset --hard HEAD clears uncommitted dirty state without destroying history
        gitTool.execute("reset --hard HEAD", root, null);
    }

    public void createWorktree(String branch, String path) throws Exception {
        gitTool.execute("worktree add " + path + " " + branch, root, null);
    }

    public void removeWorktree(String path) throws Exception {
        gitTool.execute("worktree remove " + path, root, null);
    }
}
