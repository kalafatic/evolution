package eu.kalafatic.evolution.controller.orchestration.capability;

import java.util.List;

/**
 * Universal extension model for the AI Kernel.
 */
public interface ICapability {
    String getCapabilityId();
    String getVersion();

    CapabilityStatus getStatus();

    void initialize(CapabilityContext context) throws CapabilityException;
    void start() throws CapabilityException;
    void stop() throws CapabilityException;

    List<String> getSupportedContracts();
    List<String> getDependencies();

    CapabilityHealth getHealth();
}
