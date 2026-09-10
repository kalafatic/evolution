package eu.kalafatic.evolution.controller.orchestration.develop;

import java.util.UUID;

/**
 * Task model for an autonomous development task.
 */
public class DevelopTask {
    private final String id;
    private String title;
    private String description;
    private String repository;
    private String branch;
    private DevelopPermissions permissions;
    private DevelopTaskStatus status;
    private final long createdAt;
    private long startedAt;
    private long completedAt;
    private String sessionId;
    private DevelopResult result;

    public DevelopTask(String title, String description, String repository, String branch, DevelopPermissions permissions) {
        this.id = "task-" + UUID.randomUUID().toString().substring(0, 8);
        this.title = title;
        this.description = description;
        this.repository = repository;
        this.branch = (branch != null && !branch.trim().isEmpty()) ? branch.trim() : "main";
        this.permissions = (permissions != null) ? permissions : new DevelopPermissions();
        this.status = DevelopTaskStatus.CREATED;
        this.createdAt = System.currentTimeMillis();
        this.result = new DevelopResult();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public DevelopPermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(DevelopPermissions permissions) {
        this.permissions = permissions;
    }

    public DevelopTaskStatus getStatus() {
        return status;
    }

    public void setStatus(DevelopTaskStatus status) {
        this.status = status;
        if (status == DevelopTaskStatus.PREPARING && startedAt == 0) {
            this.startedAt = System.currentTimeMillis();
        }
        if (status == DevelopTaskStatus.COMPLETED || status == DevelopTaskStatus.FAILED || status == DevelopTaskStatus.CANCELLED) {
            this.completedAt = System.currentTimeMillis();
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public DevelopResult getResult() {
        return result;
    }

    public void setResult(DevelopResult result) {
        this.result = result;
    }
}
