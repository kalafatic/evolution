package eu.kalafatic.evolution.controller.orchestration.mutation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Encapsulates an interactive Mutation session operating on an isolated Git repository workspace.
 */
public class MutationSession {

    private final String sessionId;
    private final String repoUrl;
    private final String branch;
    private final File workspaceDir;
    private final GitVersionControlProvider gitProvider;
    private long lastActivityTime;

    public MutationSession(String sessionId, String repoUrl, String branch, File workspaceDir) {
        this.sessionId = sessionId;
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.workspaceDir = workspaceDir;
        this.gitProvider = new GitVersionControlProvider();
        this.lastActivityTime = System.currentTimeMillis();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getBranch() {
        return branch;
    }

    public File getWorkspaceDir() {
        return workspaceDir;
    }

    public GitVersionControlProvider getGitProvider() {
        return gitProvider;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void touch() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public String getDiff() throws Exception {
        touch();
        if (!workspaceDir.exists()) return "";
        return gitProvider.getDiff(workspaceDir, "HEAD");
    }

    public List<String> getChangedFiles() throws Exception {
        touch();
        if (!workspaceDir.exists()) return new ArrayList<>();
        return gitProvider.getChangedFiles(workspaceDir, "HEAD");
    }

    public void commit(String message) throws Exception {
        touch();
        if (!workspaceDir.exists()) throw new IllegalStateException("Workspace directory does not exist.");
        String commitMsg = (message != null && !message.trim().isEmpty()) ? message : "Mutation interactive evolution update";
        gitProvider.commitChanges(workspaceDir, commitMsg);
    }

    public void push() throws Exception {
        touch();
        if (!workspaceDir.exists()) throw new IllegalStateException("Workspace directory does not exist.");
        gitProvider.push(workspaceDir);
    }

    /**
     * Discovers build tool in workspace and executes test command.
     */
    public String executeBuildAndTest() throws Exception {
        touch();
        if (!workspaceDir.exists()) return "Workspace directory not found.";

        ShellTool shell = new ShellTool();

        // 1. Maven
        if (new File(workspaceDir, "pom.xml").exists()) {
            File mvnw = new File(workspaceDir, System.getProperty("os.name").toLowerCase().contains("win") ? "mvnw.cmd" : "mvnw");
            String cmd = mvnw.exists() ? (mvnw.getAbsolutePath() + " test") : "mvn test";
            return shell.execute(cmd, workspaceDir, null);
        }

        // 2. Gradle
        if (new File(workspaceDir, "build.gradle").exists() || new File(workspaceDir, "build.gradle.kts").exists()) {
            File gradlew = new File(workspaceDir, System.getProperty("os.name").toLowerCase().contains("win") ? "gradlew.bat" : "gradlew");
            String cmd = gradlew.exists() ? (gradlew.getAbsolutePath() + " test") : "gradle test";
            return shell.execute(cmd, workspaceDir, null);
        }

        // 3. npm / pnpm / yarn
        if (new File(workspaceDir, "package.json").exists()) {
            if (new File(workspaceDir, "pnpm-lock.yaml").exists()) {
                return shell.execute("pnpm test", workspaceDir, null);
            } else if (new File(workspaceDir, "yarn.lock").exists()) {
                return shell.execute("yarn test", workspaceDir, null);
            } else {
                return shell.execute("npm test", workspaceDir, null);
            }
        }

        // 4. Python pytest
        if (new File(workspaceDir, "pytest.ini").exists() || new File(workspaceDir, "requirements.txt").exists() || new File(workspaceDir, "setup.py").exists()) {
            return shell.execute("pytest", workspaceDir, null);
        }

        // 5. CMake / Make
        if (new File(workspaceDir, "CMakeLists.txt").exists()) {
            return shell.execute("ctest", workspaceDir, null);
        }
        if (new File(workspaceDir, "Makefile").exists()) {
            return shell.execute("make test", workspaceDir, null);
        }

        // Fallback: search git status or return info
        return "No known build/test configuration detected (pom.xml, build.gradle, package.json, pytest, Makefile).";
    }
}
