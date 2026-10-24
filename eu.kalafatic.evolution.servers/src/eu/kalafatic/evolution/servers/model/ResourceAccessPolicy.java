package eu.kalafatic.evolution.servers.model;

import java.time.LocalDateTime;

public class ResourceAccessPolicy {
    private Long id;
    private String principalType; // USER or ROLE
    private Long principalId;
    private Long resourceId;
    private String action; // READ, EXECUTE, MANAGE, etc.
    private String effect; // ALLOW or DENY
    private LocalDateTime createdAt;
    private Long createdBy;

    public ResourceAccessPolicy() {}

    public ResourceAccessPolicy(Long id, String principalType, Long principalId, Long resourceId, String action, String effect, LocalDateTime createdAt, Long createdBy) {
        this.id = id;
        this.principalType = principalType;
        this.principalId = principalId;
        this.resourceId = resourceId;
        this.action = action;
        this.effect = effect;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrincipalType() { return principalType; }
    public void setPrincipalType(String principalType) { this.principalType = principalType; }

    public Long getPrincipalId() { return principalId; }
    public void setPrincipalId(Long principalId) { this.principalId = principalId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
