package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Central utility for programmatically managing multiple Git repositories
 * used by the Evo project.
 */
public class EclipseGitEvoTool {

    // --- Repo IDs ---
    public static final String REPO_EVOLUTION = "evolution";
    public static final String REPO_WORKSPACE = "workspace";
    public static final String REPO_LLM       = "llm";

    // --- Configuration Keys ---
    private static final String AUTO_CLONE_KEY    = "auto.clone";
    private static final String AUTO_REGISTER_KEY = "auto.register";

    // --- Inner Classes & Enums ---

    public enum OpStatus {
        SUCCESS, WARNING, FAILED, MANUAL_ACTION_REQUIRED
    }

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
        public String toString() { return "[" + status + "] " + message; }
    }

    public static class RepoStatus {
        public String id;
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
            return String.format("RepoStatus[id=%s, exists=%b, valid=%b, head=%b, dirty=%b, remote=%s]",
                    id, exists, isValid, hasHead, isDirty, remoteUrl);
        }
    }

    public static class RepoConfig {
        public final String id;
        public String defaultRemote;
        public String defaultLocalPath;
        public String defaultBranch = "master";
        public String defaultUsername = "admin";
        public String defaultPassword = "";

        public RepoConfig(String id, String remote, String local) {
            this.id = id;
            this.defaultRemote = remote;
            this.defaultLocalPath = local;
        }
    }

    // --- State ---
    private static final Properties config = new Properties();
    private static final Map<String, RepoConfig> registry = new HashMap<>();
    private static boolean autoClone = true;
    private static boolean autoRegister = true;

    static {
        registerRepository(new RepoConfig(REPO_EVOLUTION, "https://github.com/kalafatic/evolution.git", getEvolutionDefaultPath()));
        registerRepository(new RepoConfig(REPO_WORKSPACE, "https://github.com/kalafatic/evo.git", getEvoDefaultPath()));
        registerRepository(new RepoConfig(REPO_LLM, "https://github.com/kalafatic/llm.git", getLlmDefaultPath()));
    }

    // --- Utilities ---

    private static void log(String message) { System.out.println("[GIT] " + message); }

    public static String getEvolutionDefaultPath() {
        try {
            File workspaceDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
            // evolution path inside RCP sandbox workspace sibling: ${workspace_loc}/../evolution
            return new File(workspaceDir.getParentFile(), "evolution").getAbsolutePath();
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.home"), "git", "evolution").toString();
        }
    }

    private static String getEvoDefaultPath() {
        try {
            Path workspaceLoc = Paths.get(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
            return workspaceLoc.resolve("runtime").resolve("git").resolve("evo").toString();
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.home"), "git", "evo").toString();
        }
    }

    public static String getLlmDefaultPath() {
        try {
            File workspaceDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
            // LLM path inside RCP sandbox workspace sibling: ${workspace_loc}/../llm
            return new File(workspaceDir.getParentFile(), "llm").getAbsolutePath();
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.home"), "git", "llm").toString();
        }
    }

    private static File getConfigFile() {
        return new File(System.getProperty("user.home"), ".evo-git-tool.properties");
    }

    // --- Registry ---

    public static void registerRepository(RepoConfig repo) { registry.put(repo.id, repo); }

    public static List<String> getRegisteredRepositoryIds() { return new ArrayList<>(registry.keySet()); }

    // --- Management ---

    public static String getEvolutionRepository() { return getRepositoryPath(REPO_EVOLUTION); }

    public static String getWorkspaceRepository() { return getRepositoryPath(REPO_WORKSPACE); }

    public static String getRepositoryPath(String id) {
        RepoConfig rc = registry.get(id);
        if (rc == null) return null;
        return config.getProperty(id + ".local", rc.defaultLocalPath);
    }

    public static String getRepositoryRemote(String id) {
        RepoConfig rc = registry.get(id);
        if (rc == null) return null;
        return config.getProperty(id + ".remote", rc.defaultRemote);
    }

    public static String getRepositoryBranch(String id) {
        RepoConfig rc = registry.get(id);
        if (rc == null) return null;
        return config.getProperty(id + ".branch", rc.defaultBranch);
    }

    public static String getRepositoryUsername(String id) {
        RepoConfig rc = registry.get(id);
        if (rc == null) return null;
        return config.getProperty(id + ".username", rc.defaultUsername);
    }

    public static String getRepositoryPassword(String id) {
        RepoConfig rc = registry.get(id);
        if (rc == null) return null;
        return config.getProperty(id + ".password", rc.defaultPassword);
    }

    public static GitOpResult changeRepositoryLocation(String id, String newPath) {
        if (!registry.containsKey(id)) return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
        config.setProperty(id + ".local", newPath);
        saveConfiguration();
        return new GitOpResult(OpStatus.SUCCESS, "Location updated for " + id);
    }

    public static GitOpResult changeRemoteUrl(String id, String newUrl) {
        if (!registry.containsKey(id)) return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
        config.setProperty(id + ".remote", newUrl);
        saveConfiguration();
        return new GitOpResult(OpStatus.SUCCESS, "Remote URL updated for " + id);
    }

    public static GitOpResult changeBranch(String id, String branch) {
        if (!registry.containsKey(id)) return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
        config.setProperty(id + ".branch", branch);
        saveConfiguration();
        return new GitOpResult(OpStatus.SUCCESS, "Branch updated for " + id);
    }

    public static GitOpResult changeCredentials(String id, String user, String pass) {
        if (!registry.containsKey(id)) return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
        config.setProperty(id + ".username", user);
        config.setProperty(id + ".password", pass);
        saveConfiguration();
        return new GitOpResult(OpStatus.SUCCESS, "Credentials updated for " + id);
    }

    public static GitOpResult removeRepository(String id) {
        String path = getRepositoryPath(id);
        if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo not found: " + id);
        removeFromEgitView(path);
        return new GitOpResult(OpStatus.SUCCESS, "Repository removed from Eclipse view: " + id);
    }

    // --- Lifecycle ---

    public static GitOpResult initializeRepositories() {
        log("Initializing repositories...");
        loadConfiguration();
        for (String id : registry.keySet()) {
            GitOpResult checkResult = checkRepository(id);
            log(id + ": " + checkResult.getMessage());
            if (!checkResult.isSuccess() && autoClone) {
                GitOpResult cloneResult = cloneRepository(id);
                log(id + " clone: " + cloneResult.getMessage());
            }
            if (autoRegister) {
                GitOpResult registerResult = registerRepositoriesInGitView(id);
                log(id + " register: " + registerResult.getMessage());
            }
        }
        refreshGitView();
        log("Initialization complete.");
        return new GitOpResult(OpStatus.SUCCESS, "Repositories initialized");
    }

    public static GitOpResult checkRepositories() {
        log("Checking all repositories...");
        boolean allExist = true;
        for (String id : registry.keySet()) {
            if (!getRepoStatusById(id).exists) allExist = false;
        }
        return allExist ? new GitOpResult(OpStatus.SUCCESS, "All repositories exist") : new GitOpResult(OpStatus.WARNING, "Some repositories missing");
    }

    public static GitOpResult checkRepository(String id) {
        RepoStatus status = getRepoStatusById(id);
        if (status.exists) {
            return status.isValid ? new GitOpResult(OpStatus.SUCCESS, "Valid") : new GitOpResult(OpStatus.WARNING, "Invalid Git repo");
        }
        return new GitOpResult(OpStatus.FAILED, "Missing");
    }

    public static GitOpResult validateRepositories() {
        log("Validating all repositories...");
        boolean allValid = true;
        for (String id : registry.keySet()) {
            if (!getRepoStatusById(id).isValid) allValid = false;
        }
        return allValid ? new GitOpResult(OpStatus.SUCCESS, "All repositories valid") : new GitOpResult(OpStatus.FAILED, "Some repositories invalid");
    }

    public static GitOpResult cloneMissingRepositories() {
        log("Cloning missing repositories...");
        for (String id : registry.keySet()) {
            cloneRepository(id);
        }
        return new GitOpResult(OpStatus.SUCCESS, "Clone check complete");
    }

    public static GitOpResult cloneRepository(String id) {
        RepoConfig rc = registry.get(id);
        if (rc == null) return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
        return cloneIfMissing(id, getRepositoryRemote(id), getRepositoryPath(id));
    }

    public static GitOpResult registerRepositoriesInGitView() {
        for (String id : registry.keySet()) registerRepositoriesInGitView(id);
        return new GitOpResult(OpStatus.SUCCESS, "All repositories registered");
    }

    public static GitOpResult registerRepositoriesInGitView(String id) {
        String path = getRepositoryPath(id);
        if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo not found: " + id);
        addToEgitView(path);
        return new GitOpResult(OpStatus.SUCCESS, "Registered");
    }

    public static void refreshGitView() { log("Refreshing Git view..."); }

    private static RepoStatus getRepoStatusById(String id) {
        String path = getRepositoryPath(id);
        RepoStatus status = getRepoStatus(path);
        status.id = id;
        return status;
    }

    private static RepoStatus getRepoStatus(String localPath) {
        RepoStatus status = new RepoStatus();
        status.localPath = localPath;
        if (localPath == null) return status;
        File dir = new File(localPath);
        status.exists = dir.exists();
        if (!status.exists) return status;
        status.canRead = dir.canRead();
        status.canWrite = dir.canWrite();
        try (Repository repo = new FileRepositoryBuilder().setGitDir(new File(dir, ".git")).setMustExist(true).build()) {
            status.isValid = true;
            status.hasHead = repo.resolve("HEAD") != null;
            status.branch = repo.getBranch();
            String remote = repo.getConfig().getString("remote", "origin", "url");
            if (remote != null) { status.hasRemote = true; status.remoteUrl = remote; }
            try (Git git = new Git(repo)) { status.isDirty = !git.status().call().isClean(); }
        } catch (Exception e) { status.isValid = false; }
        return status;
    }

    // --- Persistence ---

    private static void loadConfiguration() {
        File configFile = getConfigFile();
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                config.load(in);
                autoClone = Boolean.parseBoolean(config.getProperty(AUTO_CLONE_KEY, "true"));
                autoRegister = Boolean.parseBoolean(config.getProperty(AUTO_REGISTER_KEY, "true"));
            } catch (IOException e) { log("Failed to load configuration: " + e.getMessage()); }
        }
    }

    private static void saveConfiguration() {
        File configFile = getConfigFile();
        config.setProperty(AUTO_CLONE_KEY, String.valueOf(autoClone));
        config.setProperty(AUTO_REGISTER_KEY, String.valueOf(autoRegister));

        // Ensure all registered repo configs that might have been updated are reflected in the properties
        // This is handled by the changeXXX methods which update 'config' directly.

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            config.store(out, "Evo Git Tool Settings");
        } catch (IOException e) { log("Failed to save configuration: " + e.getMessage()); }
    }

    // --- Integration Helpers ---

    private static void addToEgitView(String localPath) {
        File gitDir = new File(localPath, ".git");
        if (!gitDir.exists()) return;
        try {
            Class<?> utilClass = Class.forName("org.eclipse.egit.core.RepositoryUtil");
            Object util = utilClass.getMethod("getInstance").invoke(null);
            util.getClass().getMethod("addConfiguredRepository", File.class).invoke(util, gitDir);
            log("Added to EGit view: " + gitDir.getAbsolutePath());
        } catch (Exception e) { log("Failed to register with EGit: " + e.getMessage()); }
    }

    private static void removeFromEgitView(String localPath) {
        File gitDir = new File(localPath, ".git");
        if (!gitDir.exists()) return;
        try {
            Class<?> utilClass = Class.forName("org.eclipse.egit.core.RepositoryUtil");
            Object util = utilClass.getMethod("getInstance").invoke(null);
            util.getClass().getMethod("removeRepository", String.class).invoke(util, gitDir.getAbsolutePath());
            log("Removed from EGit view: " + gitDir.getAbsolutePath());
        } catch (Exception e) { log("Failed to remove from EGit: " + e.getMessage()); }
    }

    private static GitOpResult cloneIfMissing(String id, String remoteUrl, String localPath) {
        File dir = new File(localPath);
        if (dir.exists() && new File(dir, ".git").exists()) return new GitOpResult(OpStatus.SUCCESS, "Already exists");
        log("Cloning " + id + " [" + remoteUrl + "] to " + localPath);
        try {
            if (!dir.exists() && !dir.mkdirs()) return new GitOpResult(OpStatus.FAILED, "Mkdirs failed");

            CloneCommand cloneCmd = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(dir)
                    .setCloneAllBranches(true)
                    .setBare(false);

            String user = getRepositoryUsername(id);
            String pass = getRepositoryPassword(id);
            if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
                cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, pass));
            }

            try (Git git = cloneCmd.call()) {
                return new GitOpResult(OpStatus.SUCCESS, "Cloned");
            }
        } catch (Exception e) {
            log("Remote clone failed for " + id + ", initializing local repo: " + e.getMessage());
            try {
                try (Git git = Git.init().setDirectory(dir).call()) { return new GitOpResult(OpStatus.WARNING, "Local init fallback"); }
            } catch (Exception e2) { return new GitOpResult(OpStatus.FAILED, "Init failed: " + e2.getMessage()); }
        }
    }
}
