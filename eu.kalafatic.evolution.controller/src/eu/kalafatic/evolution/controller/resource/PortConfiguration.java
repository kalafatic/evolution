package eu.kalafatic.evolution.controller.resource;

import java.io.Serializable;
import java.util.Objects;

/**
 * Centralized immutable configuration model for a single network service port assignment.
 */
public class PortConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String serviceId;
    private final String serviceName;
    private final int assignedPort;
    private final int defaultPort;
    private final String protocol;
    private final String bindingAddress;
    private final boolean enabled;
    private final String configurationVersion;
    private final String metadata;

    public PortConfiguration(String serviceId, String serviceName, int assignedPort, int defaultPort,
                             String protocol, String bindingAddress, boolean enabled,
                             String configurationVersion, String metadata) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId cannot be null").trim().toUpperCase();
        this.serviceName = serviceName != null ? serviceName : serviceId;
        this.assignedPort = assignedPort;
        this.defaultPort = defaultPort;
        this.protocol = protocol != null ? protocol.toLowerCase() : "http";
        this.bindingAddress = bindingAddress != null ? bindingAddress : "127.0.0.1";
        this.enabled = enabled;
        this.configurationVersion = configurationVersion != null ? configurationVersion : "2.6.5";
        this.metadata = metadata != null ? metadata : "";
    }

    public static PortConfiguration fromRegistry(PortRegistry registry) {
        return new PortConfiguration(
                registry.getServiceId(),
                registry.getServiceName(),
                registry.getDefaultPort(),
                registry.getDefaultPort(),
                registry.getProtocol(),
                registry.getDefaultHost(),
                registry.isAutoStart(),
                "2.6.5",
                "default"
        );
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getAssignedPort() {
        return assignedPort;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getBindingAddress() {
        return bindingAddress;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getConfigurationVersion() {
        return configurationVersion;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getUrl() {
        return protocol + "://" + bindingAddress + ":" + assignedPort;
    }

    public PortConfiguration withAssignedPort(int newPort) {
        return new PortConfiguration(
                serviceId, serviceName, newPort, defaultPort, protocol, bindingAddress, enabled, configurationVersion, metadata
        );
    }

    public PortConfiguration withEnabled(boolean newEnabled) {
        return new PortConfiguration(
                serviceId, serviceName, assignedPort, defaultPort, protocol, bindingAddress, newEnabled, configurationVersion, metadata
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortConfiguration that = (PortConfiguration) o;
        return assignedPort == that.assignedPort &&
                defaultPort == that.defaultPort &&
                enabled == that.enabled &&
                Objects.equals(serviceId, that.serviceId) &&
                Objects.equals(protocol, that.protocol) &&
                Objects.equals(bindingAddress, that.bindingAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, assignedPort, defaultPort, protocol, bindingAddress, enabled);
    }

    @Override
    public String toString() {
        return "PortConfiguration{" +
                "serviceId='" + serviceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", assignedPort=" + assignedPort +
                ", defaultPort=" + defaultPort +
                ", protocol='" + protocol + '\'' +
                ", bindingAddress='" + bindingAddress + '\'' +
                ", enabled=" + enabled +
                ", version='" + configurationVersion + '\'' +
                '}';
    }
}
