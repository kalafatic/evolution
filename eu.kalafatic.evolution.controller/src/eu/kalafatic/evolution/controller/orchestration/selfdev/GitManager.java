package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class GitManager {
    private final File root;
    private final GitTool gitTool = new GitTool();
    private final Set<String> worktreeRegistry = ConcurrentHashMap.newKeySet();

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

    public void createBranchFrom(String base, String newBranch) throws Exception {
        // IMMUTABLE BRANCH PROVISIONING: NEVER mutate active checkout during provisioning.
        // We use git branch <new_branch> <base> to create it without checkout.
        try {
            gitTool.execute("branch " + newBranch + " " + base, root, null);
        } catch (Exception e) {
            // If it fails (e.g. branch exists), we force it if required by architecture,
            // but here we try to be safe.
            if (e.getMessage().contains("already exists")) {
                gitTool.execute("branch -f " + newBranch + " " + base, root, null);
            } else {
                throw e;
            }
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
        gitTool.execute("add .", root, context);
        // Metadata is automatically injected by GitTool for 'commit' commands
        gitTool.execute("commit --allow-empty -m \"" + message + "\"", root, context);
    }

    public void rollback() throws Exception {
        // Correcting regression: reset --hard HEAD clears uncommitted dirty state without destroying history
        gitTool.execute("reset --hard HEAD", root, null);
    }

    public void createWorktree(String branch, String path) throws Exception {
        gitTool.execute("worktree add " + path + " " + branch, root, null);
        registerWorktree(path);
    }

    public void removeWorktree(String path) throws Exception {
        gitTool.execute("worktree remove " + path, root, null);
        unregisterWorktree(path);
    }

    public void registerWorktree(String path) {
        worktreeRegistry.add(path);
    }

    public void unregisterWorktree(String path) {
        worktreeRegistry.remove(path);
    }

    public void cleanupWorktrees() {
        for (String path : worktreeRegistry) {
            try {
                gitTool.execute("worktree remove " + path, root, null);
                // Also attempt physical deletion of the directory if git didn't do it
                File dir = new File(path);
                if (dir.exists()) {
                    deleteDirectory(dir);
                }
            } catch (Exception e) {
                // Silently continue for other worktrees
            }
        }
        worktreeRegistry.clear();
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) deleteDirectory(file);
        }
        directory.delete();
    }
}
