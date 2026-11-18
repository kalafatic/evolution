package eu.kalafatic.evolution.servers.model;

/**
 * Identity of an application that connects to EVO Cognitive Services over MCP.
 */
public class ApplicationIdentity {
    private Long id;
    private String applicationId;
    private String name;
    private String kind;
    private Long ownerUserId;
    private boolean enabled;
    private String createdAt;
    private String updatedAt;
    private String description;

    public ApplicationIdentity() {}

    public ApplicationIdentity(Long id, String applicationId, String name, String kind,
            Long ownerUserId, boolean enabled, String createdAt, String updatedAt, String description) {
        this.id = id;
        this.applicationId = applicationId;
        this.name = name;
        this.kind = kind;
        this.ownerUserId = ownerUserId;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
