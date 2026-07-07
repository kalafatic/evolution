package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.api.Git;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

public class GitManager {
	
    // OS-specific default directory names
    private static final String WINDOWS_REPO = "git-repo";
    private static final String MAC_REPO = "git-repo";
    private static final String LINUX_REPO = "git-repo";
    private static final String DEFAULT_REPO_NAME = "git-repo";
    
	public static String DEFAULT_GIT_URL ="https://github.com/kalafatic/evolution"; 
	public static String DEFAULT_GIT_PATH ="https://github.com/kalafatic/evolution"; 
	public static String DEFAULT_GIT_EVO_URL ="https://github.com/kalafatic/evo"; 
	public static String DEFAULT_GIT_EVO_PATH ="https://github.com/kalafatic/evo"; 
	
    private final File root;
    private final GitTool gitTool = new GitTool();
    private final Set<String> worktreeRegistry = ConcurrentHashMap.newKeySet();

    public GitTool getGitTool() {
        return gitTool;
    }

    public GitManager(File root) {
        this.root = root;
    }
    
    public static String getDefaultRepositoryPath() {
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name").toLowerCase();
        
        String repoName = "git";//DEFAULT_REPO_NAME;
//        
//        // You can customize repository name per OS if needed
//        if (osName.contains("win")) {
//            repoName = WINDOWS_REPO;
//        } else if (osName.contains("mac")) {
//            repoName = MAC_REPO;
//        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
//            repoName = LINUX_REPO;
//        }
        
        // Use File separator for OS independence
        return userHome + File.separator + repoName + File.separator;
    }
    
    public static Git createOrOpenRepo(String path) throws Exception {
	    File dir = new File(path);
	    if (dir.exists() && new File(dir, ".git").exists()) {
	        return Git.open(dir);
	    } else {
	        return Git.init().setDirectory(dir).call();
	    }
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
            } catch (Exception e) {
                // Ignore init failures in environments without git
            }
        }

        if (isGitRepository()) {
            // Always try to set config if we are in a repo
            try {
                gitTool.execute("config user.email \"evolution@kalafatic.eu\"", root, null);
                gitTool.execute("config user.name \"Evolution Kernel\"", root, null);
                gitTool.execute("config commit.gpgsign false", root, null);
            } catch (Exception e) {
                // Ignore config failures
            }

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
        if (!isGitRepository()) return null;
        try {
            String branch = gitTool.execute("rev-parse --abbrev-ref HEAD", root, null).trim();
            if ("HEAD".equals(branch)) {
                // We are in detached HEAD state, try to get a better name or just return null for "originalBranch"
                return null;
            }
            return branch;
        } catch (Exception e) {
            // Fallback for new repositories without commits
            try {
                return gitTool.execute("symbolic-ref --short HEAD", root, null).trim();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public String getHeadCommit() throws Exception {
        if (!isGitRepository()) return null;
        try {
            return gitTool.execute("rev-parse HEAD", root, null).trim();
        } catch (Exception e) {
            return null;
        }
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
        if (!isGitRepository()) return;
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

        if (isGitRepository()) {
            // Hardening: reset --hard HEAD clears uncommitted dirty state, clean -fd removes untracked pollution.
            gitTool.execute("reset --hard HEAD", root, null);
            try {
                gitTool.execute("clean -fd", root, null);
            } catch (Exception e) {
                // Silently ignore clean failures if git is in a weird state
            }
        }
    }

    public void createWorktree(String branch, String path) throws Exception {
        try {
            gitTool.execute("rev-parse --verify " + branch, root, null);
            gitTool.execute("worktree add " + path + " " + branch, root, null);
        } catch (Exception e) {
            // Branch doesn't exist, create it as part of worktree addition
            gitTool.execute("worktree add -b " + branch + " " + path, root, null);
        }
        registerWorktree(path);
    }

    public void removeWorktree(String path) throws Exception {
        try {
            gitTool.execute("worktree remove --force " + path, root, null);
        } catch (Exception e) {
            // If git fails, path might already be gone or not a worktree.
            // That's acceptable for cleanup.
        } finally {
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
                gitTool.execute("worktree remove --force " + path, root, null);
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
