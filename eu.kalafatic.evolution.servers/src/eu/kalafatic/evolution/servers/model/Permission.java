package eu.kalafatic.evolution.servers.model;

public class Permission {
    private Long id;
    private String permissionKey;
    private String name;
    private String description;
    private String resourceType;
    private String action;

    public Permission() {}

    public Permission(Long id, String permissionKey, String name, String description, String resourceType, String action) {
        this.id = id;
        this.permissionKey = permissionKey;
        this.name = name;
        this.description = description;
        this.resourceType = resourceType;
        this.action = action;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
