package eu.kalafatic.evolution.controller.orchestration.capability;

import java.util.List;

import eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry;

/**
 * Darwin capability exposing the family coding harnesses (DCode by default) to capability
 * discovery and selection (Phase 6, WP-6.8). This is a capability view over the existing
 * {@link ConnectedApplicationRegistry} — no parallel harness registry is introduced.
 */
public class DCodeFamilyCapability implements ICapability {

    public static final String CAPABILITY_ID = "family.dcode.harness";
    public static final String CONTRACT_HARNESS_DELEGATION = "harness.delegation";
    public static final String CONTRACT_HARNESS_DISCOVERY = "harness.discovery";

    private CapabilityStatus status = CapabilityStatus.INITIALIZED;

    @Override
    public String getCapabilityId() {
        return CAPABILITY_ID;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public CapabilityStatus getStatus() {
        return status;
    }

    @Override
    public void initialize(CapabilityContext context) {
        this.status = CapabilityStatus.INITIALIZED;
    }

    @Override
    public void start() {
        this.status = CapabilityStatus.STARTED;
    }

    @Override
    public void stop() {
        this.status = CapabilityStatus.STOPPED;
    }

    @Override
    public List<String> getSupportedContracts() {
        return List.of(CONTRACT_HARNESS_DELEGATION, CONTRACT_HARNESS_DISCOVERY);
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    public CapabilityHealth getHealth() {
        ConnectedApplicationRegistry registry;
        try {
            registry = ConnectedApplicationRegistry.getInstance();
        } catch (Throwable t) {
            return new CapabilityHealth(0.0, "Harness registry unavailable", -1);
        }
        var descriptor = registry.findApplication("dcode");
        boolean registered = descriptor.isPresent();
        String message = registered
                ? "DCode harness registered (status: " + descriptor.get().getStatus() + ")"
                : "DCode harness not registered";
        return new CapabilityHealth(registered ? 1.0 : 0.0, message, -1);
    }
}
