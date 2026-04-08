package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import eu.kalafatic.evolution.controller.orchestration.ShellTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

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
        return shell.execute("git rev-parse --abbrev-ref HEAD", projectRoot, context).trim();
    }

    public String merge(String branchName) throws Exception {
        context.log("[GIT] Merging branch: " + branchName);
        return shell.execute("git merge " + branchName, projectRoot, context);
    }

    public void forceCheckout(String branchName) throws Exception {
        context.log("[GIT] Force switching to branch: " + branchName);
        shell.execute("git checkout -f " + branchName, projectRoot, context);
    }
}
