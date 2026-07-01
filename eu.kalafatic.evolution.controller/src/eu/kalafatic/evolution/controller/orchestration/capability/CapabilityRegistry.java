package eu.kalafatic.evolution.controller.orchestration.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.selfdev.Evaluator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IDarwinEngine;

/**
 * Central runtime catalog for AI Kernel capabilities.
 */
public class CapabilityRegistry {
    private final Map<String, ICapability> capabilities = new HashMap<>();

    public CapabilityRegistry() {}

    public synchronized void register(ICapability capability) throws CapabilityException {
        if (capabilities.containsKey(capability.getCapabilityId())) {
            return; // Already registered
        }

        capability.initialize(new CapabilityContext());
        capabilities.put(capability.getCapabilityId(), capability);
    }

    public synchronized void startAll() throws CapabilityException {
        for (ICapability capability : capabilities.values()) {
            if (capability.getStatus() == CapabilityStatus.INITIALIZED) {
                capability.start();
            }
        }
    }

    public synchronized void stopAll() throws CapabilityException {
        for (ICapability capability : capabilities.values()) {
            if (capability.getStatus() == CapabilityStatus.STARTED) {
                capability.stop();
            }
        }
    }

    public synchronized ICapability getCapability(String id) {
        return capabilities.get(id);
    }

    public synchronized <T> T getContractImplementation(String contractId, Class<T> contractClass) {
        List<ICapability> matched = capabilities.values().stream()
                .filter(c -> c.getSupportedContracts().contains(contractId))
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
            return null;
        }

        // Return the first one for now
        ICapability capability = matched.get(0);
        if (contractClass.isInstance(capability)) {
            return contractClass.cast(capability);
        }

        return null;
    }

    public synchronized List<ICapability> getAllCapabilities() {
        return new ArrayList<>(capabilities.values());
    }

    public synchronized void reset() {
        capabilities.clear();
    }

    public synchronized Map<String, CapabilityHealth> getHealthReport() {
        Map<String, CapabilityHealth> report = new HashMap<>();
        for (ICapability capability : capabilities.values()) {
            report.put(capability.getCapabilityId(), capability.getHealth());
        }
        return report;
    }

    public synchronized void shutdown() {
        try {
            stopAll();
        } catch (CapabilityException e) {
            // Log error
        }
        reset();
    }
}
