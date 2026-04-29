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
        context.log("[GIT] Creating and switching to branch: " + branchName);
        // Use a safer command execution pattern if available in ShellTool, but sticking to provided ShellTool API for now.
        return shell.execute("git checkout -b " + branchName, projectRoot, context);
    }

    public String checkout(String branchName) throws Exception {
        context.log("[GIT] Switching to branch: " + branchName);
        return shell.execute("git checkout " + branchName, projectRoot, context);
    }

    public String commit(String message) throws Exception {
        context.log("[GIT] Committing changes: " + message);
        shell.execute("git add .", projectRoot, context);
        // Sanitize message to avoid shell injection
        String safeMessage = message.replace("\"", "\\\"");
        return shell.execute("git commit -m \"" + safeMessage + "\"", projectRoot, context);
    }

    public String rollback() throws Exception {
        context.log("[GIT] Rolling back changes (hard reset)");
        return shell.execute("git reset --hard HEAD", projectRoot, context);
    }

    public String deleteBranch(String branchName) throws Exception {
        context.log("[GIT] Deleting branch: " + branchName);
        return shell.execute("git branch -D " + branchName, projectRoot, context);
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
            shell.execute("git rev-parse HEAD", projectRoot, context);
        } catch (Exception e) {
            context.log("[GIT] Empty repository detected. Creating initial empty commit.");
            shell.execute("git commit --allow-empty -m \"Initial commit (Evo)\"", projectRoot, context);
        }
    }

    public String merge(String branchName) throws Exception {
        context.log("[GIT] Merging branch: " + branchName);
        return shell.execute("git merge " + branchName, projectRoot, context);
    }

    public String createWorktree(String branchName, String path) throws Exception {
        context.log("[GIT] Creating worktree for branch " + branchName + " at " + path);
        return shell.execute("git worktree add " + path + " " + branchName, projectRoot, context);
    }

    public String removeWorktree(String path) throws Exception {
        context.log("[GIT] Removing worktree at " + path);
        return shell.execute("git worktree remove --force " + path, projectRoot, context);
    }

    public void forceCheckout(String branchName) throws Exception {
        context.log("[GIT] Force switching to branch: " + branchName);
        shell.execute("git checkout -f " + branchName, projectRoot, context);
    }
}
