package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Central utility for programmatically managing the two Git repositories
 * used by the Evo project:
 * <ol>
 *   <li>Evolution codebase</li>
 *   <li>Evo RCP workspace sandbox</li>
 * </ol>
 */
public class EclipseGitEvoTool {

    // --- Constants ---
    private static final String EVOLUTION_REMOTE_KEY = "evolution.remote";
    private static final String EVOLUTION_LOCAL_KEY  = "evolution.local";
    private static final String EVO_REMOTE_KEY       = "evo.remote";
    private static final String EVO_LOCAL_KEY        = "evo.local";
    private static final String AUTO_CLONE_KEY       = "auto.clone";
    private static final String AUTO_REGISTER_KEY    = "auto.register";

    private static final String DEFAULT_EVOLUTION_REMOTE = "https://github.com/kalafatic/evolution.git";
    private static final String DEFAULT_EVO_REMOTE       = "https://github.com/kalafatic/evo.git";

    // --- Inner Classes & Enums ---

    /**
     * Possible states for Git operations.
     */
    public enum OpStatus {
        SUCCESS, WARNING, FAILED, MANUAL_ACTION_REQUIRED
    }

    /**
     * Structured result for Git operations.
     */
    public static class GitOpResult {
        private final OpStatus status;
        private final String message;

        public GitOpResult(OpStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public OpStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public boolean isSuccess() { return status == OpStatus.SUCCESS || status == OpStatus.WARNING; }

        @Override
        public String toString() {
            return "[" + status + "] " + message;
        }
    }

    /**
     * Detailed status of a repository.
     */
    public static class RepoStatus {
        public boolean exists;
        public boolean isValid;
        public boolean hasHead;
        public boolean hasRemote;
        public String branch;
        public boolean isDirty;
        public boolean canRead;
        public boolean canWrite;
        public String remoteUrl;
        public String localPath;

        @Override
        public String toString() {
            return String.format("RepoStatus[exists=%b, valid=%b, head=%b, dirty=%b, remote=%s]",
                    exists, isValid, hasHead, isDirty, remoteUrl);
        }
    }

    // --- Configuration ---
    private static final Properties config = new Properties();
    private static boolean autoClone = true;
    private static boolean autoRegister = true;

    // --- Utilities ---

    private static void log(String message) {
        System.out.println("[GIT] " + message);
    }

    private static String getEvolutionDefaultPath() {
        return Paths.get(System.getProperty("user.home"), "git", "evolution").toString();
    }

    private static String getEvoDefaultPath() {
        try {
            // org.eclipse.core.resources.ResourcesPlugin
            Path workspaceLoc = Paths.get(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
            return workspaceLoc.resolve("runtime").resolve("git").resolve("evo").toString();
        } catch (Exception e) {
            // Fallback
            return Paths.get(System.getProperty("user.home"), "git", "evo").toString();
        }
    }

    private static File getConfigFile() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, ".evo-git-tool.properties");
    }

    // --- Validation ---

    private static RepoStatus getRepoStatus(String localPath) {
        RepoStatus status = new RepoStatus();
        status.localPath = localPath;
        File dir = new File(localPath);

        status.exists = dir.exists();
        if (!status.exists) return status;

        status.canRead = dir.canRead();
        status.canWrite = dir.canWrite();

        try (Repository repo = new FileRepositoryBuilder()
                .setGitDir(new File(dir, ".git"))
                .setMustExist(true)
                .build()) {

            status.isValid = true;
            status.hasHead = repo.resolve("HEAD") != null;
            status.branch = repo.getBranch();

            String remote = repo.getConfig().getString("remote", "origin", "url");
            if (remote != null) {
                status.hasRemote = true;
                status.remoteUrl = remote;
            }

            try (Git git = new Git(repo)) {
                Status gitStatus = git.status().call();
                status.isDirty = !gitStatus.isClean();
            }

        } catch (Exception e) {
            status.isValid = false;
        }

        return status;
    }

    // --- Management ---

    public static String getEvolutionRepository() {
        return config.getProperty(EVOLUTION_LOCAL_KEY, getEvolutionDefaultPath());
    }

    public static String getWorkspaceRepository() {
        return config.getProperty(EVO_LOCAL_KEY, getEvoDefaultPath());
    }

    public static GitOpResult changeRepositoryLocation(String key, String newPath) {
        if (key == null || newPath == null) {
            return new GitOpResult(OpStatus.FAILED, "Key or path cannot be null");
        }
        config.setProperty(key, newPath);
        saveConfiguration();
        return new GitOpResult(OpStatus.SUCCESS, "Location updated for " + key);
    }

    public static GitOpResult changeRemoteUrl(String key, String newUrl) {
        if (key == null || newUrl == null) {
            return new GitOpResult(OpStatus.FAILED, "Key or URL cannot be null");
        }
        config.setProperty(key, newUrl);
        saveConfiguration();
        return new GitOpResult(OpStatus.SUCCESS, "Remote URL updated for " + key);
    }

    public static GitOpResult removeRepository(String localPath) {
        removeFromEgitView(localPath);
        return new GitOpResult(OpStatus.SUCCESS, "Repository removed from Eclipse view: " + localPath);
    }

    // --- Lifecycle ---

    public static GitOpResult initializeRepositories() {
        log("Initializing repositories...");
        loadConfiguration();

        GitOpResult checkResult = checkRepositories();
        log(checkResult.getMessage());

        if (!checkResult.isSuccess() && autoClone) {
            GitOpResult cloneResult = cloneMissingRepositories();
            log(cloneResult.getMessage());
        }

        GitOpResult validateResult = validateRepositories();
        log(validateResult.getMessage());

        if (autoRegister) {
            GitOpResult registerResult = registerRepositoriesInGitView();
            log(registerResult.getMessage());
        }

        refreshGitView();
        log("Initialization complete.");
        return new GitOpResult(OpStatus.SUCCESS, "Repositories initialized");
    }

    public static GitOpResult checkRepositories() {
        log("Checking repositories...");
        RepoStatus evoStatus = getRepoStatus(getEvolutionRepository());
        RepoStatus sandboxStatus = getRepoStatus(getWorkspaceRepository());

        if (evoStatus.exists && sandboxStatus.exists) {
            return new GitOpResult(OpStatus.SUCCESS, "Both repositories exist");
        } else if (evoStatus.exists || sandboxStatus.exists) {
            return new GitOpResult(OpStatus.WARNING, "One or more repositories missing");
        } else {
            return new GitOpResult(OpStatus.FAILED, "Repositories missing");
        }
    }

    public static GitOpResult validateRepositories() {
        log("Validating repositories...");
        RepoStatus evoStatus = getRepoStatus(getEvolutionRepository());
        RepoStatus sandboxStatus = getRepoStatus(getWorkspaceRepository());

        StringBuilder sb = new StringBuilder();
        boolean allValid = true;

        if (!evoStatus.isValid) {
            sb.append("Evolution repository is invalid. ");
            allValid = false;
        }
        if (!sandboxStatus.isValid) {
            sb.append("Workspace repository is invalid. ");
            allValid = false;
        }

        if (allValid) {
            return new GitOpResult(OpStatus.SUCCESS, "All repositories are valid");
        } else {
            return new GitOpResult(OpStatus.FAILED, sb.toString().trim());
        }
    }

    public static GitOpResult cloneMissingRepositories() {
        log("Cloning missing repositories...");
        GitOpResult r1 = cloneIfMissing(EVOLUTION_REMOTE_KEY, DEFAULT_EVOLUTION_REMOTE, getEvolutionRepository());
        GitOpResult r2 = cloneIfMissing(EVO_REMOTE_KEY, DEFAULT_EVO_REMOTE, getWorkspaceRepository());

        if (r1.isSuccess() && r2.isSuccess()) {
            return new GitOpResult(OpStatus.SUCCESS, "Cloning operations completed");
        } else {
            return new GitOpResult(OpStatus.WARNING, "Some cloning operations failed: " + r1.getMessage() + " | " + r2.getMessage());
        }
    }

    // --- Integration ---

    public static GitOpResult registerRepositoriesInGitView() {
        log("Registering repositories in Eclipse Git view...");
        addToEgitView(getEvolutionRepository());
        addToEgitView(getWorkspaceRepository());
        return new GitOpResult(OpStatus.SUCCESS, "Repositories registered");
    }

    public static void refreshGitView() {
        log("Refreshing Git view...");
        // This is primarily handled by EGit when repositories are added.
    }

    private static void addToEgitView(String localPath) {
        File dir = new File(localPath);
        File gitDir = new File(dir, ".git");
        if (!gitDir.exists()) return;

        try {
            // Using reflection to avoid direct dependency on EGit internal classes
            // and handle different versions of Eclipse/EGit.
            Class<?> utilClass = Class.forName("org.eclipse.egit.core.RepositoryUtil");
            Object util = utilClass.getMethod("getInstance").invoke(null);
            util.getClass().getMethod("addConfiguredRepository", File.class).invoke(util, gitDir);
            log("Added to EGit view: " + gitDir.getAbsolutePath());
        } catch (Exception e) {
            log("Failed to register repository with EGit: " + e.getMessage());
        }
    }

    private static void removeFromEgitView(String localPath) {
        File dir = new File(localPath);
        File gitDir = new File(dir, ".git");
        if (!gitDir.exists()) return;

        try {
            Class<?> utilClass = Class.forName("org.eclipse.egit.core.RepositoryUtil");
            Object util = utilClass.getMethod("getInstance").invoke(null);
            // RepositoryUtil.removeRepository(String path)
            util.getClass().getMethod("removeRepository", String.class).invoke(util, gitDir.getAbsolutePath());
            log("Removed from EGit view: " + gitDir.getAbsolutePath());
        } catch (Exception e) {
            log("Failed to remove repository from EGit: " + e.getMessage());
        }
    }

    // --- Persistence ---

    private static void loadConfiguration() {
        File configFile = getConfigFile();
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                config.load(in);
                autoClone = Boolean.parseBoolean(config.getProperty(AUTO_CLONE_KEY, "true"));
                autoRegister = Boolean.parseBoolean(config.getProperty(AUTO_REGISTER_KEY, "true"));
                log("Configuration loaded from " + configFile.getAbsolutePath());
            } catch (IOException e) {
                log("Failed to load configuration: " + e.getMessage());
            }
        }
    }

    private static void saveConfiguration() {
        File configFile = getConfigFile();
        config.setProperty(AUTO_CLONE_KEY, String.valueOf(autoClone));
        config.setProperty(AUTO_REGISTER_KEY, String.valueOf(autoRegister));
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            config.store(out, "Evo Git Tool Settings");
            log("Configuration saved to " + configFile.getAbsolutePath());
        } catch (IOException e) {
            log("Failed to save configuration: " + e.getMessage());
        }
    }

    private static GitOpResult cloneIfMissing(String configKey, String defaultRemote, String localPath) {
        File dir = new File(localPath);
        if (dir.exists() && new File(dir, ".git").exists()) {
            return new GitOpResult(OpStatus.SUCCESS, "Repository already exists at " + localPath);
        }

        String remoteUrl = config.getProperty(configKey, defaultRemote);
        log("Cloning " + remoteUrl + " to " + localPath);

        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return new GitOpResult(OpStatus.FAILED, "Failed to create directory: " + localPath);
            }

            try (Git git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(dir)
                    .setCloneAllBranches(true)
                    .setBare(false)
                    .call()) {
                log("Clone complete: " + localPath);
                return new GitOpResult(OpStatus.SUCCESS, "Cloned " + remoteUrl);
            }
        } catch (Exception e) {
            log("Remote clone failed, creating local repository as fallback: " + e.getMessage());
            try {
                try (Git git = Git.init().setDirectory(dir).call()) {
                    return new GitOpResult(OpStatus.WARNING, "Created local repository at " + localPath + " (remote unavailable)");
                }
            } catch (Exception e2) {
                return new GitOpResult(OpStatus.FAILED, "Failed to initialize local repository: " + e2.getMessage());
            }
        }
    }
}
