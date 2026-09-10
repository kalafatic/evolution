package eu.kalafatic.evolution.controller.orchestration.develop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.develop.vcs.GitService;
import eu.kalafatic.evolution.controller.orchestration.develop.vcs.GitServiceImpl;

/**
 * Isolated session for autonomous development tasks.
 */
public class DevelopSession {

    private final String sessionId;
    private final DevelopTask task;
    private final File workspaceDir;
    private final File repositoryDir;
    private final GitService gitService;
    private long lastActivityTime;
    private final List<String> logs = new ArrayList<>();
    private volatile boolean cancelled = false;

    public DevelopSession(String sessionId, DevelopTask task, File workspaceDir) {
        this.sessionId = sessionId;
        this.task = task;
        this.workspaceDir = workspaceDir;
        this.repositoryDir = new File(workspaceDir, "repository");
        if (!this.repositoryDir.exists()) {
            this.repositoryDir.mkdirs();
        }
        this.gitService = new GitServiceImpl();
        this.lastActivityTime = System.currentTimeMillis();
        this.task.setSessionId(sessionId);
    }

    public String getSessionId() {
        return sessionId;
    }

    public DevelopTask getTask() {
        return task;
    }

    public File getWorkspaceDir() {
        return workspaceDir;
    }

    public File getRepositoryDir() {
        return repositoryDir;
    }

    public GitService getGitService() {
        return gitService;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void touch() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
        this.task.setStatus(DevelopTaskStatus.CANCELLED);
        addLog("Task cancelled by user.");
    }

    public synchronized void addLog(String message) {
        touch();
        String timestamp = String.format("[%tF %<tT]", System.currentTimeMillis());
        String entry = timestamp + " " + message;
        logs.add(entry);
    }

    public synchronized List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public String getDiff() throws Exception {
        touch();
        if (!repositoryDir.exists()) return "";
        return gitService.getDiff(repositoryDir, "HEAD");
    }

    public List<String> getChangedFiles() throws Exception {
        touch();
        if (!repositoryDir.exists()) return new ArrayList<>();
        return gitService.getChangedFiles(repositoryDir, "HEAD");
    }

    public void commit(String message) throws Exception {
        touch();
        if (!task.getPermissions().canWrite()) {
            throw new IllegalStateException("WRITE permission denied for this session.");
        }
        if (!repositoryDir.exists()) {
            throw new IllegalStateException("Repository directory does not exist.");
        }
        gitService.commitChanges(repositoryDir, message);
        addLog("Committed changes with message: " + message);
    }

    public void push() throws Exception {
        touch();
        if (!task.getPermissions().canPush()) {
            throw new IllegalStateException("PUSH permission denied for this session.");
        }
        if (!repositoryDir.exists()) {
            throw new IllegalStateException("Repository directory does not exist.");
        }
        gitService.push(repositoryDir, "origin", task.getBranch());
        addLog("Pushed changes to branch: " + task.getBranch());
    }
}
