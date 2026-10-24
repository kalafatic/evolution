package eu.kalafatic.evolution.servers.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private String displayName;
    private String passwordHash;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private String description;

    public User() {}

    public User(Long id, String username, String displayName, String passwordHash, String role, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime lastLoginAt, String description) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
