package eu.kalafatic.evolution.servers.model;

import java.time.LocalDateTime;

public class Role {
    private Long id;
    private String name;
    private String description;
    private boolean systemRole;
    private boolean enabled;

    public Role() {}

    public Role(Long id, String name, String description, boolean systemRole, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystemRole() { return systemRole; }
    public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
