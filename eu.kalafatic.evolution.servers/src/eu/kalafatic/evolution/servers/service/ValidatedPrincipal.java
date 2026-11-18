package eu.kalafatic.evolution.servers.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Result of successful token validation: the authenticated principal with the
 * scopes granted to the (application, user) pair.
 */
public class ValidatedPrincipal {
    private final String username;
    private final String applicationId;
    private final long applicationAccessId;
    private final Set<String> scopes;

    public ValidatedPrincipal(String username, String applicationId, long applicationAccessId, Set<String> scopes) {
        this.username = username;
        this.applicationId = applicationId;
        this.applicationAccessId = applicationAccessId;
        this.scopes = scopes != null ? Collections.unmodifiableSet(new HashSet<>(scopes)) : Collections.emptySet();
    }

    public String getUsername() { return username; }
    public String getApplicationId() { return applicationId; }
    public long getApplicationAccessId() { return applicationAccessId; }
    public Set<String> getScopes() { return scopes; }

    public boolean hasScope(String scope) {
        return scopes.contains(scope) || scopes.contains("ecosystem.admin");
    }

    public boolean isEcosystemAdmin() {
        return scopes.contains("ecosystem.admin");
    }
}
