package eu.kalafatic.evolution.servers.model;

public class ResourceItem {
    private Long id;
    private String resourceType;
    private String resourceKey;
    private String displayName;
    private String description;
    private boolean enabled;
    private String metadata;

    public ResourceItem() {}

    public ResourceItem(Long id, String resourceType, String resourceKey, String displayName, String description, boolean enabled, String metadata) {
        this.id = id;
        this.resourceType = resourceType;
        this.resourceKey = resourceKey;
        this.displayName = displayName;
        this.description = description;
        this.enabled = enabled;
        this.metadata = metadata;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceKey() { return resourceKey; }
    public void setResourceKey(String resourceKey) { this.resourceKey = resourceKey; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
