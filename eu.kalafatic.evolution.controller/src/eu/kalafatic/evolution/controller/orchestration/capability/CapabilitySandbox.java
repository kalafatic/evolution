package eu.kalafatic.evolution.controller.orchestration.capability;

import java.util.concurrent.Callable;

/**
 * Provides isolation for capability execution.
 * Handles failure isolation and telemetry collection.
 */
public class CapabilitySandbox {

    public static <V> V execute(ICapability capability, Callable<V> task) throws CapabilityException {
        long start = System.currentTimeMillis();
        try {
            return task.call();
        } catch (Exception e) {
            // Failure isolation
            throw new CapabilityException("Capability " + capability.getCapabilityId() + " failed during execution.", e);
        } finally {
            long duration = System.currentTimeMillis() - start;
            // Telemetry: Latency reporting could be updated here if CapabilityHealth was mutable or tracked in Registry
        }
    }
}
