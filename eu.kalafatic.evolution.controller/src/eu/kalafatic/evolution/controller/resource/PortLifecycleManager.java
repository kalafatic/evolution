package eu.kalafatic.evolution.controller.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Coordinates listener startup and shutdown following the canonical 8-step lifecycle:
 * 1. Load authoritative configuration.
 * 2. Validate configuration integrity.
 * 3. Resolve all service assignments.
 * 4. Check port availability and detect conflicts with OS diagnostics.
 * 5. Initialize dependent services.
 * 6. Start listeners.
 * 7. Register actual bound endpoints.
 * 8. Publish service readiness.
 */
public class PortLifecycleManager {

    private static final PortLifecycleManager INSTANCE = new PortLifecycleManager();

    public interface ManagedListener {
        String getServiceId();
        int getPort();
        void startListener() throws IOException;
        void stopListener();
        boolean isListening();
    }

    private final Map<String, ManagedListener> registeredListeners = new ConcurrentHashMap<>();
    private final Map<String, Integer> activeBoundEndpoints = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    private PortLifecycleManager() {}

    public static PortLifecycleManager getInstance() {
        return INSTANCE;
    }

    /**
     * Executes lifecycle stages 1 through 4: Load, Validate, Resolve, and Preflight Conflict Check.
     *
     * @param orchestrator Authoritative EMF model.
     * @throws IllegalStateException if configuration is invalid or ports are occupied.
     */
    public synchronized void prepareLifecycle(Orchestrator orchestrator) {
        Log.log("[PortLifecycleManager] Step 1: Loading authoritative configuration.");
        PortPersistence.getInstance().loadFromEmf(orchestrator);

        Log.log("[PortLifecycleManager] Step 2: Validating configuration integrity.");
        List<String> integrityIssues = PortAllocationManager.getInstance().validateIntegrity();
        if (!integrityIssues.isEmpty()) {
            throw new IllegalStateException("Port configuration integrity validation failed:\n" + String.join("\n", integrityIssues));
        }

        Log.log("[PortLifecycleManager] Step 3: Resolving service port assignments.");
        for (PortConfiguration cfg : PortAllocationManager.getInstance().getAllConfigurations()) {
            Log.log(" -> Resolved " + cfg.getServiceId() + " -> " + cfg.getUrl());
        }

        Log.log("[PortLifecycleManager] Step 4: Pre-flight port availability and conflict inspection.");
        List<String> conflicts = new ArrayList<>();
        for (PortConfiguration cfg : PortAllocationManager.getInstance().getAllConfigurations()) {
            if (cfg.isEnabled()) {
                PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(cfg.getAssignedPort(), cfg.getServiceName());
                if (report.isInUse()) {
                    // Check if already bound by ourselves
                    ManagedListener listener = registeredListeners.get(cfg.getServiceId());
                    if (listener == null || !listener.isListening()) {
                        conflicts.add(report.toString());
                    }
                }
            }
        }

        if (!conflicts.isEmpty()) {
            String errorMsg = "EVO cannot start network listeners due to occupied ports:\n" + String.join("\n\n", conflicts);
            Log.log("[PortLifecycleManager] " + errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        this.initialized = true;
        Log.log("[PortLifecycleManager] Pre-launch lifecycle preparation completed successfully.");
    }

    /**
     * Registers and coordinates startup of a network service listener (Steps 5-8).
     */
    public synchronized void startManagedListener(ManagedListener listener) throws IOException {
        if (listener == null) return;
        String serviceId = listener.getServiceId();

        // Check availability
        PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(listener.getPort(), serviceId);
        if (report.isInUse()) {
            ManagedListener existing = registeredListeners.get(serviceId);
            if (existing != null && existing.isListening()) {
                Log.log("[PortLifecycleManager] Listener for " + serviceId + " is already running on port " + listener.getPort());
                return;
            }
            throw new IOException(report.toString());
        }

        Log.log("[PortLifecycleManager] Step 6: Starting listener for " + serviceId + " on port " + listener.getPort());
        listener.startListener();

        registeredListeners.put(serviceId, listener);
        activeBoundEndpoints.put(serviceId, listener.getPort());

        Log.log("[PortLifecycleManager] Step 7: Registered bound endpoint " + serviceId + " -> " + listener.getPort());
        Log.log("[PortLifecycleManager] Step 8: Published readiness for " + serviceId);
    }

    /**
     * Stops a managed listener and unregisters its bound endpoint.
     */
    public synchronized void stopManagedListener(String serviceId) {
        if (serviceId == null) return;
        ManagedListener listener = registeredListeners.remove(serviceId);
        if (listener != null) {
            try {
                listener.stopListener();
                Log.log("[PortLifecycleManager] Stopped listener for " + serviceId);
            } catch (Exception e) {
                Log.log("[PortLifecycleManager] Error stopping listener " + serviceId + ": " + e.getMessage());
            }
        }
        activeBoundEndpoints.remove(serviceId);
    }

    /**
     * Shuts down all active network listeners cleanly on workbench shutdown.
     */
    public synchronized void shutdownAll() {
        Log.log("[PortLifecycleManager] Shutting down all managed EVO network listeners...");
        for (Map.Entry<String, ManagedListener> entry : registeredListeners.entrySet()) {
            try {
                entry.getValue().stopListener();
                Log.log("[PortLifecycleManager] Stopped " + entry.getKey());
            } catch (Exception e) {
                Log.log("[PortLifecycleManager] Exception during stop of " + entry.getKey() + ": " + e.getMessage());
            }
        }
        registeredListeners.clear();
        activeBoundEndpoints.clear();
        this.initialized = false;
        Log.log("[PortLifecycleManager] All network listeners released.");
    }

    public boolean isEndpointBound(String serviceId) {
        return activeBoundEndpoints.containsKey(serviceId);
    }

    public int getBoundPort(String serviceId) {
        return activeBoundEndpoints.getOrDefault(serviceId, -1);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
