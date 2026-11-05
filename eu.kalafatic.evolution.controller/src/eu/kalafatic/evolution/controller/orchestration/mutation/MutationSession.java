package eu.kalafatic.evolution.controller.orchestration.mutation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.ConversationOutputController;
import eu.kalafatic.evolution.controller.orchestration.MessagePriority;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Encapsulates an interactive Mutation session operating on an isolated Git repository workspace.
 * Provides fine-grained lifecycle status, project detection, cancellation, explicit change approval,
 * build/test execution metrics, and full audit trail for arbitrary repositories.
 */
public class MutationSession {

    public enum Status {
        CREATED,
        INVESTIGATING,
        CHANGES_PROPOSED,
        WAITING_APPROVAL,
        APPROVED,
        BUILDING,
        TESTING,
        COMMITTED,
        PUSHED,
        CANCELLED,
        ERROR
    }

    public static class FileModification {
        private final String filePath;
        private final String changeType; // ADD, MODIFY, DELETE
        private final String content;

        public FileModification(String filePath, String changeType, String content) {
            this.filePath = filePath;
            this.changeType = changeType;
            this.content = content;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getChangeType() {
            return changeType;
        }

        public String getContent() {
            return content;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("filePath", filePath);
            obj.put("changeType", changeType);
            return obj;
        }
    }

    private final String sessionId;
    private final String repoUrl;
    private final String branch;
    private final File workspaceDir;
    private final GitVersionControlProvider gitProvider;
    private long lastActivityTime;

    private volatile Status status = Status.CREATED;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final List<String> auditLogs = new CopyOnWriteArrayList<>();
    private final List<FileModification> proposedChanges = new CopyOnWriteArrayList<>();
    private volatile boolean approved = false;
    private volatile String lastBuildOutput = "";
    private volatile String lastBuildStatus = "NOT_RUN";

    public MutationSession(String sessionId, String repoUrl, String branch, File workspaceDir) {
        this.sessionId = sessionId;
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.workspaceDir = workspaceDir;
        this.gitProvider = new GitVersionControlProvider();
        this.lastActivityTime = System.currentTimeMillis();
        addLog("Mutation session initialized for " + (repoUrl != null && !repoUrl.isEmpty() ? repoUrl : workspaceDir.getName()) + " on branch " + branch);
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        touch();
        addLog("Status transitioned to: " + status.name());
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        this.cancelled.set(true);
        setStatus(Status.CANCELLED);
        addLog("Session was cancelled by user request");
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
        touch();
        if (approved) {
            setStatus(Status.APPROVED);
            addLog("User explicitly approved proposed changes");
        } else {
            addLog("Approval state reset");
        }
    }

    public List<FileModification> getProposedChanges() {
        return Collections.unmodifiableList(proposedChanges);
    }

    public void setProposedChanges(List<FileModification> changes) {
        this.proposedChanges.clear();
        if (changes != null) {
            this.proposedChanges.addAll(changes);
        }
        touch();
        if (!proposedChanges.isEmpty()) {
            setStatus(Status.WAITING_APPROVAL);
            addLog("Proposed " + proposedChanges.size() + " file modification(s) awaiting user approval");
        }
    }

    public void applyProposedChanges() throws IOException {
        touch();
        if (!approved) {
            throw new IllegalStateException("Cannot apply sensitive changes: explicit user approval required.");
        }
        if (!workspaceDir.exists()) {
            throw new IllegalStateException("Workspace directory does not exist.");
        }

        for (FileModification mod : proposedChanges) {
            File targetFile = new File(workspaceDir, mod.getFilePath());
            if ("DELETE".equalsIgnoreCase(mod.getChangeType())) {
                if (targetFile.exists()) {
                    targetFile.delete();
                    addLog("Deleted file: " + mod.getFilePath());
                }
            } else {
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Files.writeString(targetFile.toPath(), mod.getContent() != null ? mod.getContent() : "");
                addLog("Applied " + mod.getChangeType() + " to file: " + mod.getFilePath());
            }
        }
        addLog("All proposed changes applied to workspace tree");
    }

    public String getLastBuildOutput() {
        return lastBuildOutput;
    }

    public String getLastBuildStatus() {
        return lastBuildStatus;
    }

    public List<String> getAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    public void addLog(String entry) {
        String logLine = "[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] " + entry;
        this.auditLogs.add(logLine);
        this.lastActivityTime = System.currentTimeMillis();
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
        String commitMsg = (message != null && !message.trim().isEmpty()) ? message.trim() : "Mutation interactive evolution update";
        gitProvider.commitChanges(workspaceDir, commitMsg);
        setStatus(Status.COMMITTED);
        addLog("Explicit Git commit executed: " + commitMsg);
    }

    public void push() throws Exception {
        touch();
        if (!workspaceDir.exists()) throw new IllegalStateException("Workspace directory does not exist.");
        gitProvider.push(workspaceDir);
        setStatus(Status.PUSHED);
        addLog("Explicit Git push executed to remote branch: " + branch);
    }

    /**
     * Inspects workspace layout and returns detected build system and languages.
     */
    public String detectProjectArchitecture() {
        if (!workspaceDir.exists()) return "Workspace missing";
        List<String> systems = new ArrayList<>();
        if (new File(workspaceDir, "pom.xml").exists()) systems.add("Maven (pom.xml)");
        if (new File(workspaceDir, "build.gradle").exists() || new File(workspaceDir, "build.gradle.kts").exists()) systems.add("Gradle");
        if (new File(workspaceDir, "package.json").exists()) systems.add("Node.js / npm");
        if (new File(workspaceDir, "requirements.txt").exists() || new File(workspaceDir, "setup.py").exists() || new File(workspaceDir, "pytest.ini").exists()) systems.add("Python");
        if (new File(workspaceDir, "CMakeLists.txt").exists()) systems.add("CMake");
        if (new File(workspaceDir, "Makefile").exists()) systems.add("Make");

        return systems.isEmpty() ? "Arbitrary Repository (Generic Files)" : String.join(", ", systems);
    }

    /**
     * Discovers build tool in workspace and executes test command with structured output logging.
     */
    public String executeBuildAndTest() throws Exception {
        touch();
        setStatus(Status.BUILDING);
        addLog("Starting automated build & test execution");

        if (!workspaceDir.exists()) {
            this.lastBuildStatus = "ERROR";
            this.lastBuildOutput = "Workspace directory not found.";
            setStatus(Status.ERROR);
            return lastBuildOutput;
        }

        ShellTool shell = new ShellTool();
        String output;

        try {
            // 1. Maven
            if (new File(workspaceDir, "pom.xml").exists()) {
                File mvnw = new File(workspaceDir, System.getProperty("os.name").toLowerCase().contains("win") ? "mvnw.cmd" : "mvnw");
                String cmd = mvnw.exists() ? (mvnw.getAbsolutePath() + " test") : "mvn test";
                output = shell.execute(cmd, workspaceDir, null);
            }
            // 2. Gradle
            else if (new File(workspaceDir, "build.gradle").exists() || new File(workspaceDir, "build.gradle.kts").exists()) {
                File gradlew = new File(workspaceDir, System.getProperty("os.name").toLowerCase().contains("win") ? "gradlew.bat" : "gradlew");
                String cmd = gradlew.exists() ? (gradlew.getAbsolutePath() + " test") : "gradle test";
                output = shell.execute(cmd, workspaceDir, null);
            }
            // 3. npm / pnpm / yarn
            else if (new File(workspaceDir, "package.json").exists()) {
                if (new File(workspaceDir, "pnpm-lock.yaml").exists()) {
                    output = shell.execute("pnpm test", workspaceDir, null);
                } else if (new File(workspaceDir, "yarn.lock").exists()) {
                    output = shell.execute("yarn test", workspaceDir, null);
                } else {
                    output = shell.execute("npm test", workspaceDir, null);
                }
            }
            // 4. Python pytest
            else if (new File(workspaceDir, "pytest.ini").exists() || new File(workspaceDir, "requirements.txt").exists() || new File(workspaceDir, "setup.py").exists()) {
                output = shell.execute("pytest", workspaceDir, null);
            }
            // 5. CMake / Make
            else if (new File(workspaceDir, "CMakeLists.txt").exists()) {
                output = shell.execute("ctest", workspaceDir, null);
            } else if (new File(workspaceDir, "Makefile").exists()) {
                output = shell.execute("make test", workspaceDir, null);
            } else {
                output = "No automated test framework detected. Project architecture: " + detectProjectArchitecture();
            }

            this.lastBuildOutput = output;
            boolean failed = output != null && (output.contains("BUILD FAILURE") || output.contains("FAILURES!!!") || output.contains("ERRORS!") || output.contains("npm ERR!"));
            this.lastBuildStatus = failed ? "FAILURE" : "SUCCESS";
            setStatus(failed ? Status.ERROR : Status.TESTING);
            addLog("Build & test finished with status: " + this.lastBuildStatus);
            return output;

        } catch (Exception ex) {
            this.lastBuildStatus = "ERROR";
            this.lastBuildOutput = "Execution exception: " + ex.getMessage();
            setStatus(Status.ERROR);
            addLog("Build & test execution error: " + ex.getMessage());
            throw ex;
        }
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("sessionId", sessionId);
        obj.put("repoUrl", repoUrl != null ? repoUrl : "");
        obj.put("branch", branch != null ? branch : "main");
        obj.put("workspaceDir", workspaceDir.getAbsolutePath());
        obj.put("lastActivityTime", lastActivityTime);
        obj.put("status", status.name());
        obj.put("isCancelled", isCancelled());
        obj.put("isApproved", isApproved());
        obj.put("projectArchitecture", detectProjectArchitecture());
        obj.put("lastBuildStatus", lastBuildStatus);
        obj.put("lastBuildOutput", lastBuildOutput);

        JSONArray props = new JSONArray();
        for (FileModification m : proposedChanges) {
            props.put(m.toJson());
        }
        obj.put("proposedChanges", props);
        obj.put("auditLogs", new JSONArray(auditLogs));

        return obj;
    }
}
