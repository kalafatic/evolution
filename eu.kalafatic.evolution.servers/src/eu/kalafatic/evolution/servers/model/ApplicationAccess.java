package eu.kalafatic.evolution.servers.model;

/**
 * An issued access token binding an application to a user with granted scopes.
 * Only the SHA-256 hash of the token is stored.
 */
public class ApplicationAccess {
    private Long id;
    private Long applicationId;
    private Long userId;
    private String tokenHash;
    private String scopesJson;
    private boolean revoked;
    private String issuedAt;
    private String expiresAt;
    private String lastUsedAt;
    private String description;

    public ApplicationAccess() {}

    public ApplicationAccess(Long id, Long applicationId, Long userId, String tokenHash,
            String scopesJson, boolean revoked, String issuedAt, String expiresAt,
            String lastUsedAt, String description) {
        this.id = id;
        this.applicationId = applicationId;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.scopesJson = scopesJson;
        this.revoked = revoked;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getScopesJson() { return scopesJson; }
    public void setScopesJson(String scopesJson) { this.scopesJson = scopesJson; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(String lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
