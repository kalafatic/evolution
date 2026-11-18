package eu.kalafatic.evolution.controller.harness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Product-local application descriptor for a connected coding application in EVO
 * (Phase 3, A-14). Persisted as JSON under the EVO data directory located through the
 * central ResourceManager path resolution (EVO AGENTS.md §7); the EMF orchestration model
 * remains authoritative for sessions/targets and is not extended for product-local
 * connected-application configuration.
 */
public class ConnectedApplicationDescriptor {

    public enum ApplicationStatus {
        UNKNOWN,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    private final String id;
    private String name;
    private String applicationType;
    private List<String> capabilities;
    private boolean enabled;
    private ApplicationStatus status;
    private String statusMessage;
    private Map<String, String> connectionConfiguration;
    private final Instant createdAt;
    private Instant updatedAt;

    public ConnectedApplicationDescriptor(
            String id,
            String name,
            String applicationType,
            List<String> capabilities,
            boolean enabled,
            ApplicationStatus status,
            String statusMessage,
            Map<String, String> connectionConfiguration,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name != null ? name : id;
        this.applicationType = applicationType != null ? applicationType : "UNKNOWN";
        this.capabilities = capabilities != null ? new ArrayList<>(capabilities) : new ArrayList<>();
        this.enabled = enabled;
        this.status = status != null ? status : ApplicationStatus.UNKNOWN;
        this.statusMessage = statusMessage != null ? statusMessage : "";
        this.connectionConfiguration = connectionConfiguration != null
                ? new LinkedHashMap<>(connectionConfiguration) : new LinkedHashMap<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.updatedAt = Instant.now(); }

    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; this.updatedAt = Instant.now(); }

    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities != null ? new ArrayList<>(capabilities) : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; this.updatedAt = Instant.now(); }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status != null ? status : ApplicationStatus.UNKNOWN; this.updatedAt = Instant.now(); }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage != null ? statusMessage : ""; this.updatedAt = Instant.now(); }

    public Map<String, String> getConnectionConfiguration() { return connectionConfiguration; }
    public void setConnectionConfiguration(Map<String, String> connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration != null
                ? new LinkedHashMap<>(connectionConfiguration) : new LinkedHashMap<>();
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
