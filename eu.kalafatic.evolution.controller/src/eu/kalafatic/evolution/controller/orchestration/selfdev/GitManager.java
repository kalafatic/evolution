package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.ShellTool;

public class GitManager {
    private final File projectRoot;
    private final TaskContext context;
    private final ShellTool shell = new ShellTool();

    public GitManager(File projectRoot, TaskContext context) {
        this.projectRoot = projectRoot;
        this.context = context;
    }

    public String createBranch(String branchName) throws Exception {
        if (!isGitRepository()) {
            context.log("[GIT] Warning: Not a git repository. Skipping branch creation.");
            return "Skipped (not a git repo)";
        }

        String actualBranchName = branchName;
        if (branchExists(branchName)) {
            actualBranchName = branchName + "-" + System.currentTimeMillis();
            context.log("[GIT] Branch " + branchName + " already exists. Using " + actualBranchName);
        }

        context.log("[GIT] Creating and switching to branch: " + actualBranchName);
        return executeWithRetry("git checkout -b \"" + actualBranchName + "\"");
    }

    public String checkout(String branchName) throws Exception {
        if (!isGitRepository()) return "Skipped";
        context.log("[GIT] Switching to branch: " + branchName);
        return executeWithRetry("git checkout \"" + branchName + "\"");
    }

    public String commit(String message) throws Exception {
        context.log("[GIT] Committing changes: " + message);
        executeWithRetry("git add .");
        // Sanitize message to avoid shell injection
        String safeMessage = message.replace("\"", "\\\"");
        return executeWithRetry("git commit -m \"" + safeMessage + "\"");
    }

    public String rollback() throws Exception {
        context.log("[GIT] Rolling back changes (hard reset)");
        return executeWithRetry("git reset --hard HEAD");
    }

    public String deleteBranch(String branchName) throws Exception {
        if (!isGitRepository()) return "Skipped";
        context.log("[GIT] Deleting branch: " + branchName);
        return executeWithRetry("git branch -D \"" + branchName + "\"");
    }

    public String getCurrentBranch() throws Exception {
        try {
            return shell.execute("git rev-parse --abbrev-ref HEAD", projectRoot, context).trim();
        } catch (Exception e) {
            context.log("[GIT] Warning: Could not determine current branch (repo might be empty). Falling back to 'main'.");
            return "main";
        }
    }

    public void ensureInitialCommit() throws Exception {
        try {
            executeWithRetry("git rev-parse HEAD");
        } catch (Exception e) {
            context.log("[GIT] Empty repository detected. Creating initial commit.");
            executeWithRetry("git add .");
            executeWithRetry("git commit --allow-empty -m \"Initial commit (Evo)\"");
        }
    }

    public String merge(String branchName) throws Exception {
        if (!isGitRepository()) return "Skipped";
        context.log("[GIT] Merging branch: " + branchName);
        return executeWithRetry("git merge \"" + branchName + "\"");
    }

    public String createWorktree(String branchName, String path) throws Exception {
        if (!isGitRepository()) return "Skipped";
        context.log("[GIT] Creating worktree for branch " + branchName + " at " + path);
        return executeWithRetry("git worktree add \"" + path + "\" \"" + branchName + "\"");
    }

    public String removeWorktree(String path) throws Exception {
        context.log("[GIT] Removing worktree at " + path);
        return executeWithRetry("git worktree remove --force " + path);
    }

    public void forceCheckout(String branchName) throws Exception {
        if (!isGitRepository()) return;
        context.log("[GIT] Force switching to branch: " + branchName);
        executeWithRetry("git checkout -f \"" + branchName + "\"");
    }

    public boolean isGitRepository() {
        File gitDir = new File(projectRoot, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    private boolean branchExists(String branchName) {
        try {
            String output = shell.execute("git branch --list \"" + branchName + "\"", projectRoot, context);
            return output != null && !output.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private String executeWithRetry(String command) throws Exception {
        int maxRetries = 3;
        int delay = 1000;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                checkAndRemoveLock();
                return shell.execute(command, projectRoot, context);
            } catch (Exception e) {
                lastException = e;
                if (e.getMessage() != null && e.getMessage().contains("index.lock")) {
                    context.log("[GIT] index.lock detected. Retrying (" + (i + 1) + "/" + maxRetries + ")...");
                    Thread.sleep(delay);
                } else {
                    throw e;
                }
            }
        }
        throw lastException;
    }

    private void checkAndRemoveLock() {
        File lockFile = new File(projectRoot, ".git/index.lock");
        if (lockFile.exists()) {
            // Check if it's "stale" - e.g. older than 30 seconds
            long age = System.currentTimeMillis() - lockFile.lastModified();
            if (age > 30000) {
                context.log("[GIT] Found stale index.lock (age: " + age + "ms). Removing it.");
                if (!lockFile.delete()) {
                    context.log("[GIT] Warning: Failed to delete stale index.lock");
                }
            }
        }
    }
}
