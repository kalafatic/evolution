package eu.kalafatic.evolution.servers.model;

import java.time.LocalDateTime;

public class Session {
    private Long id;
    private String sessionId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccess;
    private String clientIp;
    private String workflowType;
    private String metadata;

    public Session() {}

    public Session(Long id, String sessionId, Long userId, LocalDateTime createdAt, LocalDateTime lastAccess, String clientIp) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.lastAccess = lastAccess;
        this.clientIp = clientIp;
    }

    public Session(Long id, String sessionId, Long userId, LocalDateTime createdAt, LocalDateTime lastAccess, String clientIp, String workflowType, String metadata) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.lastAccess = lastAccess;
        this.clientIp = clientIp;
        this.workflowType = workflowType;
        this.metadata = metadata;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastAccess() { return lastAccess; }
    public void setLastAccess(LocalDateTime lastAccess) { this.lastAccess = lastAccess; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getWorkflowType() { return workflowType; }
    public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
