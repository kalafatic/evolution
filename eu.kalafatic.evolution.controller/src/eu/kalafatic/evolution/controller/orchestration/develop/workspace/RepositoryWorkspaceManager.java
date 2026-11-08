package eu.kalafatic.evolution.controller.orchestration.develop.workspace;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutor;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutorImpl;

/**
 * Manages shared bare Git repository mirrors and isolated task worktrees / workspaces.
 * Prevents repeatedly cloning identical repositories, provides user isolation,
 * sanitizes arguments against shell injection, and coordinates safe workspace cleanup.
 */
public class RepositoryWorkspaceManager {

    private static final RepositoryWorkspaceManager INSTANCE = new RepositoryWorkspaceManager();

    private static final Pattern SAFE_BRANCH_PATTERN = Pattern.compile("^[a-zA-Z0-9._/\\-]+$");

    private final File baseCacheDir;
    private final ProcessExecutor processExecutor = new ProcessExecutorImpl();
    private final Map<String, ReentrantLock> repoLocks = new ConcurrentHashMap<>();

    private RepositoryWorkspaceManager() {
        String home = System.getProperty("user.home", ".");
        this.baseCacheDir = new File(home, ".evo/repo-cache");
        if (!baseCacheDir.exists()) {
            baseCacheDir.mkdirs();
        }
    }

    public static RepositoryWorkspaceManager getInstance() {
        return INSTANCE;
    }

    public File getBaseCacheDir() {
        return baseCacheDir;
    }

    public String sanitizeBranch(String branch) {
        if (branch == null || branch.trim().isEmpty()) {
            return "main";
        }
        String trimmed = branch.trim();
        if (!SAFE_BRANCH_PATTERN.matcher(trimmed).matches() || trimmed.contains("..") || trimmed.startsWith("-")) {
            throw new IllegalArgumentException("Invalid or unsafe branch name: " + branch);
        }
        return trimmed;
    }

    public String computeRepoKey(String repoUrl, String userContext) {
        if (repoUrl == null) repoUrl = "";
        if (userContext == null) userContext = "default";

        String sanitizedUrl = repoUrl.replaceAll("https?://[^@]+@", "https://").trim();
        String combined = userContext.trim() + "::" + sanitizedUrl;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 16);
        } catch (Exception ex) {
            return Integer.toHexString(combined.hashCode());
        }
    }

    public File getOrFetchMirror(String repoUrl, String userContext) throws Exception {
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            return null;
        }

        String repoKey = computeRepoKey(repoUrl, userContext);
        File mirrorDir = new File(baseCacheDir, repoKey + ".git");

        ReentrantLock lock = repoLocks.computeIfAbsent(repoKey, k -> new ReentrantLock());
        lock.lock();
        try {
            if (!mirrorDir.exists()) {
                mirrorDir.mkdirs();
                Log.log("[WORKSPACE] Cloning bare mirror for " + repoUrl + " into " + mirrorDir.getAbsolutePath());
                ProcessExecutor.ProcessResult res = processExecutor.execute(
                        "git clone --mirror " + repoUrl.trim() + " .",
                        mirrorDir, null, 180000);
                if (!res.isSuccess()) {
                    Log.log("[WORKSPACE] Mirror clone failed: " + res.getStderr());
                    deleteDirectory(mirrorDir);
                    throw new IllegalStateException("Failed to clone bare mirror: " + res.getStderr());
                }
            } else {
                Log.log("[WORKSPACE] Fetching updates for bare mirror: " + mirrorDir.getName());
                ProcessExecutor.ProcessResult fetchRes = processExecutor.execute(
                        "git remote update", mirrorDir, null, 60000);
                if (!fetchRes.isSuccess()) {
                    Log.log("[WORKSPACE] Mirror update warning: " + fetchRes.getStderr());
                }
            }
            return mirrorDir;
        } finally {
            lock.unlock();
        }
    }

    public boolean provisionWorktree(File mirrorDir, String branch, File destinationDir) {
        if (mirrorDir == null || !mirrorDir.exists() || destinationDir == null) {
            return false;
        }

        String targetBranch = sanitizeBranch(branch);
        if (destinationDir.exists()) {
            deleteDirectory(destinationDir);
        }
        destinationDir.mkdirs();

        try {
            String cmd = "git --git-dir=\"" + mirrorDir.getAbsolutePath() + "\" worktree add -f \"" + destinationDir.getAbsolutePath() + "\" " + targetBranch;
            ProcessExecutor.ProcessResult res = processExecutor.execute(cmd, mirrorDir, null, 60000);
            if (res.isSuccess()) {
                Log.log("[WORKSPACE] Created isolated worktree in " + destinationDir.getAbsolutePath() + " on branch " + targetBranch);
                return true;
            } else {
                String fallbackCmd = "git --git-dir=\"" + mirrorDir.getAbsolutePath() + "\" worktree add -b " + targetBranch + " -f \"" + destinationDir.getAbsolutePath() + "\" HEAD";
                ProcessExecutor.ProcessResult fallbackRes = processExecutor.execute(fallbackCmd, mirrorDir, null, 60000);
                if (fallbackRes.isSuccess()) {
                    Log.log("[WORKSPACE] Created isolated worktree with new branch in " + destinationDir.getAbsolutePath());
                    return true;
                }
                Log.log("[WORKSPACE] Worktree provisioning failed: " + res.getStderr() + " | " + fallbackRes.getStderr());
            }
        } catch (Exception ex) {
            Log.log("[WORKSPACE] Worktree provision exception: " + ex.getMessage());
        }
        return false;
    }

    public void cleanupWorkspace(File mirrorDir, File workspaceDir) {
        if (workspaceDir == null || !workspaceDir.exists()) {
            return;
        }

        if (mirrorDir != null && mirrorDir.exists()) {
            try {
                String cmd = "git --git-dir=\"" + mirrorDir.getAbsolutePath() + "\" worktree remove -f \"" + workspaceDir.getAbsolutePath() + "\"";
                processExecutor.execute(cmd, mirrorDir, null, 30000);
                processExecutor.execute("git --git-dir=\"" + mirrorDir.getAbsolutePath() + "\" worktree prune", mirrorDir, null, 30000);
            } catch (Exception ignored) {
            }
        }

        deleteDirectory(workspaceDir);
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
