package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

public class GitManager {
    private final File root;
    private final GitTool gitTool = new GitTool();
    private final Set<String> worktreeRegistry = ConcurrentHashMap.newKeySet();

    public GitManager(File root) {
        this.root = root;
    }

    public boolean isGitRepository() {
        File gitDir = new File(root, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    public void cleanupLocks() {
        File gitLock = new File(root, ".git/index.lock");
        if (gitLock.exists()) {
            gitLock.delete();
        }
    }

    public void ensureInitialCommit() throws Exception {
        cleanupLocks();
        if (!root.exists()) {
            root.mkdirs();
        }

        if (!isGitRepository()) {
            try {
                gitTool.execute("init", root, null);
                gitTool.execute("config user.email \"evolution@kalafatic.eu\"", root, null);
                gitTool.execute("config user.name \"Evolution Kernel\"", root, null);
                gitTool.execute("config commit.gpgsign false", root, null);
            } catch (Exception e) {
                // Ignore init failures in environments without git
            }
        }

        if (isGitRepository()) {
            // Ensure HEAD exists (it won't in a fresh init without commits)
            try {
                gitTool.execute("rev-parse --verify HEAD", root, null);
            } catch (Exception e) {
                // HEAD does not exist, create initial commit
                try {
                    gitTool.execute("add .", root, null);
                    gitTool.execute("commit --allow-empty -m \"Initial commit [EVO-SEED]\"", root, null);
                } catch (Exception ex) {
                    // Non-critical if commit fails in restricted environments
                }
            }
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

        if (context != null && context.getKernelContext() != null && context.getKernelContext().getEventBus() != null) {
            String hash = "";
            try { hash = getHeadCommit(); } catch (Exception e) {}
            context.getKernelContext().getEventBus().publish(new RuntimeEvent(RuntimeEventType.FILE_WRITTEN, context.getSessionId(), "GitManager", hash));
        }
    }

    public void rollback() throws Exception {
        rollback(null);
    }

    public void rollback(TaskContext context) throws Exception {
        cleanupLocks();

        if (context != null && context.getKernelContext() != null && context.getKernelContext().getEventBus() != null) {
            context.getKernelContext().getEventBus().publish(new RuntimeEvent(RuntimeEventType.RECOVERY_TRIGGERED, context.getSessionId(), "GitManager", "Git Rollback"));
        }

        // Hardening: reset --hard HEAD clears uncommitted dirty state, clean -fd removes untracked pollution.
        gitTool.execute("reset --hard HEAD", root, null);
        try {
            gitTool.execute("clean -fd", root, null);
        } catch (Exception e) {
            // Silently ignore clean failures if git is in a weird state
        }
    }

    public void createWorktree(String branch, String path) throws Exception {
        gitTool.execute("worktree add " + path + " " + branch, root, null);
        registerWorktree(path);
    }

    public void removeWorktree(String path) throws Exception {
        if (worktreeRegistry.contains(path)) {
            gitTool.execute("worktree remove " + path, root, null);
            unregisterWorktree(path);
        }
    }

    public void registerWorktree(String path) {
        worktreeRegistry.add(path);
    }

    public void unregisterWorktree(String path) {
        worktreeRegistry.remove(path);
    }

    public void pruneWorktrees() {
        try {
            gitTool.execute("worktree prune", root, null);
        } catch (Exception e) {}
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
