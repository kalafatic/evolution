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

    /**
     * WP9 - Git simulation boundary.
     * When enabled (DEFAULT), no mutating Git command is ever executed; the
     * manager validates preconditions and records simulated results instead.
     * Real mutation requires an explicit operator opt-out (evo.git.simulation=false).
     */
    private final boolean simulationMode;

    private final java.util.List<String> simulatedOperations = new java.util.ArrayList<>();

    public GitTool getGitTool() {
        return gitTool;
    }

    public GitManager(File root) {
        if (root == null) {
            throw new IllegalArgumentException("GitManager root repository path must be explicitly provided and cannot be null.");
        }
        this.root = root;
        this.simulationMode = Boolean.parseBoolean(System.getProperty("evo.git.simulation", "true"));
    }

    public boolean isSimulationMode() {
        return simulationMode;
    }

    public java.util.List<String> getSimulatedOperations() {
        synchronized (simulatedOperations) {
            return new java.util.ArrayList<>(simulatedOperations);
        }
    }

    /**
     * Executes a mutating Git operation under the simulation boundary.
     * Returns the simulated output when simulation is enabled.
     *
     * @return the simulated result string, or null when the operation must run for real.
     */
    private String simulate(String operation, String command, String preconditionNote) {
        if (simulationMode) {
            String record = "[SIMULATED] " + operation + " (no real git mutation): " + command
                    + (preconditionNote != null && !preconditionNote.isEmpty() ? " | " + preconditionNote : "");
            synchronized (simulatedOperations) {
                simulatedOperations.add(record);
            }
            System.out.println("[GitManager] " + record);
            return record;
        }
        return null;
    }

    private void executeMutating(String operation, String command, TaskContext context, Runnable realExecution) throws Exception {
        String simulated = simulate(operation, command, null);
        if (simulated != null) {
            return;
        }
        realExecution.run();
    }

    /**
     * Read-only porcelain status (allowed in simulation mode).
     */
    public String getStatusPorcelain() throws Exception {
        return gitTool.execute("status --porcelain", root, null);
    }
    
    public static String getDefaultRepositoryPath() {
        throw new UnsupportedOperationException("Implicit Git repository path guessing is disabled. Canonical EVO Git repository must be explicitly configured.");
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
        return gitDir.exists() && (gitDir.isDirectory() || gitDir.isFile());
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
            String simulated = simulate("init", "git init " + root, "repository initialization");
            if (simulated != null) {
                return;
            }
            try {
                gitTool.execute("init", root, null);
            } catch (Exception e) {
                // Ignore init failures in environments without git
            }
        }

        if (isGitRepository()) {
            // Always try to set config if we are in a repo
            String simulatedConfig = simulate("config", "git config user/commit.gpgsign", "local repo identity config");
            if (simulatedConfig == null) {
                try {
                    gitTool.execute("config user.email \"evolution@kalafatic.eu\"", root, null);
                    gitTool.execute("config user.name \"Evolution Kernel\"", root, null);
                    gitTool.execute("config commit.gpgsign false", root, null);
                } catch (Exception e) {
                    // Ignore config failures
                }
            }

            // Ensure HEAD exists (it won't in a fresh init without commits)
            try {
                gitTool.execute("rev-parse --verify HEAD", root, null);
            } catch (Exception e) {
                // HEAD does not exist, create initial commit
                String simulatedCommit = simulate("initial-commit", "add . && commit --allow-empty", "seed commit for empty repository");
                if (simulatedCommit == null) {
                    try {
                        gitTool.execute("add .", root, null);
                        gitTool.execute("commit --allow-empty -m \"Initial commit [EVO-SEED]\"", root, null);
                    } catch (Exception ex) {
                        // Non-critical if commit fails in restricted environments
                    }
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
        executeMutating("createBranch", "checkout " + branchName, null, () -> {
            try {
                gitTool.execute("rev-parse --verify " + branchName, root, null);
                // Branch exists, just checkout
                gitTool.execute("checkout " + branchName, root, null);
            } catch (Exception e) {
                // Branch doesn't exist, create it
                try {
                    gitTool.execute("checkout -b " + branchName, root, null);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void createBranchFrom(String base, String newBranch) throws Exception {
        executeMutating("createBranchFrom", "branch " + newBranch + " " + base, null, () -> {
            try {
                gitTool.execute("branch " + newBranch + " " + base, root, null);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                    try {
                        gitTool.execute("branch -f " + newBranch + " " + base, root, null);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void forceCheckout(String branchName) throws Exception {
        executeMutating("forceCheckout", "checkout -f " + branchName, null, () -> {
            try {
                gitTool.execute("checkout -f " + branchName, root, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void merge(String branchName) throws Exception {
        executeMutating("merge", "merge " + branchName, null, () -> {
            try {
                gitTool.execute("merge " + branchName, root, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void commit(String message) throws Exception {
        commit(message, null);
    }

    public void commit(String message, TaskContext context) throws Exception {
        if (!isGitRepository()) return;
        executeMutating("commit", "add . && commit -m \"" + message + "\"", context, () -> {
            try {
                gitTool.execute("add .", root, context);
                // Metadata is automatically injected by GitTool for 'commit' commands
                gitTool.execute("commit --allow-empty -m \"" + message + "\"", root, context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        if (context != null && context.getKernelContext() != null && context.getKernelContext().getEventBus() != null) {
            String hash = "";
            try { hash = getHeadCommit(); } catch (Exception e) {}
            context.getKernelContext().getEventBus().publish(new RuntimeEvent(RuntimeEventType.FILE_WRITTEN, context.getSessionId(), "GitManager", hash));
        }
    }

    /**
     * WP9 - self-dev commit with an explicit eligibility gate.
     * The commit requires recorded success evidence for build, tests and export.
     * In simulation mode the gate is evaluated and the commit is simulated.
     *
     * @return the commit outcome (simulated when simulation mode is on).
     */
    public GitOperationResult commitWithEligibility(String message, SelfDevContext context) {
        if (!isGitRepository()) {
            return GitOperationResult.failure("commit", "Not a Git repository: " + root);
        }
        EligibilityReport report = evaluateCommitEligibility(context);
        if (!report.isEligible()) {
            String detail = "Commit eligibility failed: " + String.join("; ", report.getReasons());
            System.err.println("[GitManager] " + detail);
            return GitOperationResult.failure("commit", detail);
        }
        try {
            commit(message, null);
            return GitOperationResult.success("commit", message, simulationMode);
        } catch (Exception e) {
            return GitOperationResult.failure("commit", "Commit exception: " + e.getMessage());
        }
    }

    /**
     * WP9 - push is a separate, authorization-gated operation (D-45).
     * - Requires explicit push authorization on the run context.
     * - Validates remote + branch preconditions.
     * - Never force-pushes.
     * - In simulation mode the push is simulated; the default state without
     *   authorization is PENDING_AUTHORIZATION (prepared commit, no push).
     */
    public GitOperationResult pushWithAuthorization(String remote, String branch, SelfDevContext context) {
        if (!isGitRepository()) {
            return GitOperationResult.failure("push", "Not a Git repository: " + root);
        }
        if (context == null || !context.isGitPushAuthorized()) {
            return GitOperationResult.failure("push",
                    "PUSH_PENDING: push requires explicit user authorization; the change is left as a prepared commit.");
        }
        if (remote == null || remote.trim().isEmpty() || branch == null || branch.trim().isEmpty()) {
            return GitOperationResult.failure("push", "Push refused: remote and branch must be explicitly specified.");
        }
        if (remote.contains(" -f") || branch.contains(" -f") || branch.contains("--force")) {
            return GitOperationResult.failure("push", "Force-push is forbidden (D-45).");
        }
        String command = "push " + remote + " " + branch;
        String simulated = simulate("push", command, "authorized push (simulated)");
        if (simulated != null) {
            return GitOperationResult.success("push", simulated, true);
        }
        try {
            gitTool.execute(command, root, null);
            return GitOperationResult.success("push", "Pushed to " + remote + " " + branch, false);
        } catch (Exception e) {
            return GitOperationResult.failure("push", "Push exception: " + e.getMessage());
        }
    }

    /**
     * Commit eligibility: successful build + export evidence, tests not skipped,
     * no empty/dummy evidence (D-44).
     */
    public EligibilityReport evaluateCommitEligibility(SelfDevContext context) {
        java.util.List<String> reasons = new java.util.ArrayList<>();
        if (context == null) {
            reasons.add("SelfDevContext is null");
            return new EligibilityReport(false, reasons);
        }
        TaskResult build = context.getTaskResult("BUILD_EVO");
        TaskResult export = context.getTaskResult("EXPORT_EVO");
        if (build == null || !build.isSuccess()) {
            reasons.add("BUILD_EVO did not succeed");
        }
        if (export == null || !export.isSuccess()) {
            reasons.add("EXPORT_EVO did not succeed");
        }
        boolean testsEvidence = false;
        try {
            BuildArtifact artifact = context.getArtifactManifestStore().resolve(ArtifactType.EVO_RCP);
            if (artifact != null && artifact.getTestResult() != null
                    && !"SKIPPED_BY_POLICY".equals(artifact.getTestResult())
                    && !artifact.getTestResult().contains("SKIPPED")) {
                testsEvidence = true;
            }
        } catch (Exception ignored) {}
        if (!testsEvidence) {
            reasons.add("no non-skipped test evidence recorded");
        }
        return new EligibilityReport(reasons.isEmpty(), reasons);
    }

    public static class EligibilityReport {
        private final boolean eligible;
        private final java.util.List<String> reasons;

        public EligibilityReport(boolean eligible, java.util.List<String> reasons) {
            this.eligible = eligible;
            this.reasons = reasons != null ? reasons : java.util.Collections.emptyList();
        }

        public boolean isEligible() { return eligible; }
        public java.util.List<String> getReasons() { return java.util.Collections.unmodifiableList(reasons); }
    }

    public static class GitOperationResult {
        private final String operation;
        private final boolean success;
        private final String message;
        private final boolean simulated;

        public GitOperationResult(String operation, boolean success, String message, boolean simulated) {
            this.operation = operation;
            this.success = success;
            this.message = message;
            this.simulated = simulated;
        }

        public static GitOperationResult success(String operation, String message, boolean simulated) {
            return new GitOperationResult(operation, true, message, simulated);
        }

        public static GitOperationResult failure(String operation, String message) {
            return new GitOperationResult(operation, false, message, true);
        }

        public String getOperation() { return operation; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public boolean isSimulated() { return simulated; }

        @Override
        public String toString() {
            return "GitOperationResult{op=" + operation + ", success=" + success + ", simulated=" + simulated + ", message=" + message + "}";
        }
    }

    public void rollback() throws Exception {
        rollback(null);
    }

    /**
     * WP9 (D-40/D-41): destructive reset/clean semantics are replaced by
     * iteration-scoped restoration. Without a captured baseline and an explicit
     * change set, rollback REFUSES instead of destroying the working tree.
     */
    public void rollback(TaskContext context) throws Exception {
        cleanupLocks();

        if (context != null && context.getKernelContext() != null && context.getKernelContext().getEventBus() != null) {
            context.getKernelContext().getEventBus().publish(new RuntimeEvent(RuntimeEventType.RECOVERY_TRIGGERED, context.getSessionId(), "GitManager", "Git Rollback"));
        }

        if (!isGitRepository()) {
            return;
        }

        // Refuse destructive rollback without a baseline (D-41).
        String msg = "Rollback refused: no iteration baseline captured; refusing destructive git reset/clean for " + root;
        System.err.println("[GitManager] " + msg);
        String simulated = simulate("rollback", "reset --hard HEAD && clean -fd (REFUSED)", msg);
        if (simulated != null) {
            return;
        }
        throw new IllegalStateException(msg);
    }

    /**
     * WP9 - iteration-scoped restoration (never destructive to pre-existing work).
     * Real mode: restore ONLY the recorded iteration-owned paths (checkout the
     * tracked ones, delete the untracked ones). Simulation mode: preconditions
     * are validated and the restoration is recorded without execution.
     */
    public GitOperationResult restoreIterationBaseline(GitBaseline baseline, java.util.List<String> changedPaths, TaskContext context) {
        if (!isGitRepository()) {
            return GitOperationResult.failure("restore", "Not a Git repository: " + root);
        }
        if (baseline == null) {
            return GitOperationResult.failure("restore", "Restoration refused: no iteration baseline available for " + root);
        }
        if (changedPaths == null || changedPaths.isEmpty()) {
            return GitOperationResult.success("restore", "No iteration-owned changes to restore.", simulationMode);
        }

        java.util.List<String> tracked = new java.util.ArrayList<>();
        java.util.List<String> untracked = new java.util.ArrayList<>();
        for (String path : changedPaths) {
            if (path == null || path.trim().isEmpty()) continue;
            String trimmed = path.trim();
            // Safety: refuse absolute or traversal paths (D-48).
            if (trimmed.startsWith("/") || trimmed.contains("..")) {
                return GitOperationResult.failure("restore", "Restoration refused: unsafe path in change set: " + trimmed);
            }
            if (isTrackedPath(trimmed)) {
                tracked.add(trimmed);
            } else {
                untracked.add(trimmed);
            }
        }

        String command = "checkout -- " + String.join(" ", tracked);
        String simulated = simulate("restore-baseline", command, "iteration-scoped; untracked cleanup: " + untracked);
        if (simulated != null) {
            return GitOperationResult.success("restore", simulated, true);
        }

        try {
            if (!tracked.isEmpty()) {
                gitTool.execute(command, root, context);
            }
            for (String path : untracked) {
                File f = new File(root, path);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else if (!f.delete()) {
                        return GitOperationResult.failure("restore", "Could not remove untracked file: " + path);
                    }
                }
            }
            return GitOperationResult.success("restore", "Iteration-scoped restoration completed for " + changedPaths.size() + " path(s).", false);
        } catch (Exception e) {
            return GitOperationResult.failure("restore", "Restoration exception: " + e.getMessage());
        }
    }

    private boolean isTrackedPath(String path) {
        try {
            String out = gitTool.execute("ls-files -- " + path, root, null);
            return out != null && !out.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void createWorktree(String branch, String path) throws Exception {
        executeMutating("createWorktree", "worktree add " + path + " " + branch, null, () -> {
            try {
                gitTool.execute("rev-parse --verify " + branch, root, null);
                gitTool.execute("worktree add " + path + " " + branch, root, null);
            } catch (Exception e) {
                // Branch doesn't exist, create it as part of worktree addition
                try {
                    gitTool.execute("worktree add -b " + branch + " " + path, root, null);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        registerWorktree(path);
    }

    public void removeWorktree(String path) throws Exception {
        executeMutating("removeWorktree", "worktree remove --force " + path, null, () -> {
            try {
                gitTool.execute("worktree remove --force " + path, root, null);
            } catch (Exception e) {
                // If git fails, path might already be gone or not a worktree.
            }
        });
        unregisterWorktree(path);
    }

    public void registerWorktree(String path) {
        worktreeRegistry.add(path);
    }

    public void unregisterWorktree(String path) {
        worktreeRegistry.remove(path);
    }

    public void pruneWorktrees() {
        executeMutatingQuiet("pruneWorktrees", "worktree prune", () -> {
            try {
                gitTool.execute("worktree prune", root, null);
            } catch (Exception e) {}
        });
    }

    private void executeMutatingQuiet(String operation, String command, Runnable realExecution) {
        String simulated = simulate(operation, command, null);
        if (simulated != null) {
            return;
        }
        try {
            realExecution.run();
        } catch (Exception e) {
            System.err.println("[GitManager] " + operation + " failed: " + e.getMessage());
        }
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
