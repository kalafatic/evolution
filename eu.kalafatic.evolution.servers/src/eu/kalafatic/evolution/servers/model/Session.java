package eu.kalafatic.evolution.servers.model;

import java.time.LocalDateTime;

public class Session {
    private Long id;
    private String sessionId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccess;
    private String clientIp;

    public Session() {}

    public Session(Long id, String sessionId, Long userId, LocalDateTime createdAt, LocalDateTime lastAccess, String clientIp) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.lastAccess = lastAccess;
        this.clientIp = clientIp;
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
}
