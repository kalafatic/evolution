package eu.kalafatic.evolution.controller.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;

/**
 * Authoritative allocation manager, validation engine, and migration coordinator for platform ports.
 */
public class PortAllocationManager {

    private static final PortAllocationManager INSTANCE = new PortAllocationManager();

    private final Map<String, PortConfiguration> configurations = new ConcurrentHashMap<>();

    private PortAllocationManager() {
        initDefaults();
    }

    public static PortAllocationManager getInstance() {
        return INSTANCE;
    }

    public synchronized void initDefaults() {
        configurations.clear();
        for (PortRegistry reg : PortRegistry.values()) {
            configurations.put(reg.getServiceId(), PortConfiguration.fromRegistry(reg));
        }
    }

    /**
     * Resolves the authoritative port configuration for the given service.
     *
     * @param serviceId Service ID or alias.
     * @return PortConfiguration instance.
     */
    public PortConfiguration getConfiguration(String serviceId) {
        PortRegistry reg = PortRegistry.findByServiceId(serviceId);
        if (reg != null) {
            PortConfiguration cfg = configurations.get(reg.getServiceId());
            if (cfg != null) {
                return cfg;
            }
            cfg = PortConfiguration.fromRegistry(reg);
            configurations.put(reg.getServiceId(), cfg);
            return cfg;
        }
        return configurations.get(serviceId != null ? serviceId.trim().toUpperCase() : "");
    }

    public int getPort(String serviceId) {
        PortConfiguration cfg = getConfiguration(serviceId);
        return cfg != null ? cfg.getAssignedPort() : -1;
    }

    public List<PortConfiguration> getAllConfigurations() {
        return Collections.unmodifiableList(new ArrayList<>(configurations.values()));
    }

    /**
     * Updates an assigned port for a service after strict collision and range validation.
     *
     * @param serviceId Service identifier.
     * @param newPort Target port number.
     * @throws IllegalArgumentException if port is invalid or conflicts with another EVO service.
     */
    public synchronized void setAssignedPort(String serviceId, int newPort) {
        PortConfiguration existing = getConfiguration(serviceId);
        if (existing == null) {
            throw new IllegalArgumentException("Unknown EVO service: " + serviceId);
        }
        if (newPort < 1024 || newPort > 65535) {
            throw new IllegalArgumentException("Invalid port number " + newPort + ". Must be between 1024 and 65535.");
        }

        // Check internal collisions with other production services
        for (PortConfiguration cfg : configurations.values()) {
            if (!cfg.getServiceId().equalsIgnoreCase(existing.getServiceId()) && cfg.getAssignedPort() == newPort) {
                throw new IllegalArgumentException("Port collision: Port " + newPort + " is already assigned to EVO service [" + cfg.getServiceName() + "]");
            }
        }

        // Prevent collision with reserved debug ports
        if (PortRegistry.isDebugPort(newPort)) {
            PortRegistry debugOwner = PortRegistry.findByDebugPort(newPort);
            throw new IllegalArgumentException("Port collision: Port " + newPort + " is reserved for debug isolation [" +
                    (debugOwner != null ? debugOwner.getServiceName() : "Debug Service") + "]");
        }

        if (existing.getAssignedPort() != newPort) {
            configurations.put(existing.getServiceId(), existing.withAssignedPort(newPort));
            Log.log("[PortAllocationManager] Updated port for " + existing.getServiceId() + " to " + newPort);
        }
    }

    /**
     * Migrates legacy EVO configurations (e.g. 8080, 8089, 38080, 48080, 58080) to the new +20000 allocation standard.
     */
    public synchronized boolean migrateFromLegacy(Orchestrator orch) {
        if (orch == null) {
            return false;
        }
        boolean modified = false;

        // 1. Primary Server Settings
        ServerSettings srv = orch.getServerSettings();
        if (srv != null) {
            int port = srv.getPort();
            // Legacy ports: 8080, 48080, 48081
            if (port == 8080 || port == 48080 || port <= 0) {
                srv.setPort(PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort());
                setAssignedPort(PortRegistry.EVO_HTTP_PRIMARY.getServiceId(), PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort());
                modified = true;
                Log.log("[PortAllocationManager] Migrated primary server port from " + port + " to " + PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort());
            } else {
                setAssignedPort(PortRegistry.EVO_HTTP_PRIMARY.getServiceId(), port);
            }

            // Secondary / Portal Port: legacy 8180, 58080 or accidental collision with primary port
            int primaryAssigned = getPort(PortRegistry.EVO_HTTP_PRIMARY.getServiceId());
            int portalPort = srv.getPortalPort();
            if (portalPort == 8180 || portalPort == 58080 || portalPort <= 0 || portalPort == primaryAssigned) {
                srv.setPortalPort(PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort());
                srv.setPortalUrl("http://localhost:" + PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort() + "/dashboard.html");
                setAssignedPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId(), PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort());
                modified = true;
                Log.log("[PortAllocationManager] Migrated portal port from " + portalPort + " to " + PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort());
            } else {
                setAssignedPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId(), portalPort);
            }

            // MCP Port: legacy 8280, 38080, 48082 or accidental collision with other services
            int portalAssigned = getPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId());
            int mcpPort = srv.getMcpPort();
            if (mcpPort == 8280 || mcpPort == 38080 || mcpPort == 48082 || mcpPort <= 0 || mcpPort == primaryAssigned || mcpPort == portalAssigned) {
                srv.setMcpPort(PortRegistry.EVO_MCP.getDefaultPort());
                setAssignedPort(PortRegistry.EVO_MCP.getServiceId(), PortRegistry.EVO_MCP.getDefaultPort());
                modified = true;
                Log.log("[PortAllocationManager] Migrated MCP port from " + mcpPort + " to " + PortRegistry.EVO_MCP.getDefaultPort());
            } else {
                setAssignedPort(PortRegistry.EVO_MCP.getServiceId(), mcpPort);
            }
        }

        // 2. Orchestrator McpServerUrl
        String mcpUrl = orch.getMcpServerUrl();
        String canonicalMcpUrl = "http://localhost:" + PortRegistry.EVO_MCP.getDefaultPort() + "/mcp";
        if (mcpUrl == null || mcpUrl.contains("38080") || mcpUrl.contains("48082") || mcpUrl.contains(":8280") || mcpUrl.contains("/8280") || mcpUrl.equals("localhost:28280") || mcpUrl.trim().isEmpty()) {
            if (!canonicalMcpUrl.equals(mcpUrl)) {
                orch.setMcpServerUrl(canonicalMcpUrl);
                modified = true;
                Log.log("[PortAllocationManager] Migrated Orchestrator McpServerUrl to " + orch.getMcpServerUrl());
            }
        }

        // 3. Supervisor Settings: legacy 8089, 8480
        SupervisorSettings sup = orch.getSupervisorSettings();
        if (sup != null) {
            String settings = sup.getSettings();
            if (settings != null) {
                if (settings.contains("port=8089") || settings.contains("port=8480")) {
                    settings = settings.replaceAll("port=(?:8089|8480)", "port=" + PortRegistry.EVO_SUPERVISOR_API.getDefaultPort());
                    sup.setSettings(settings);
                    setAssignedPort(PortRegistry.EVO_SUPERVISOR_API.getServiceId(), PortRegistry.EVO_SUPERVISOR_API.getDefaultPort());
                    modified = true;
                    Log.log("[PortAllocationManager] Migrated SupervisorSettings port to " + PortRegistry.EVO_SUPERVISOR_API.getDefaultPort());
                }
                if (settings.contains("controlPort=28080") || settings.contains("controlPort=8580")) {
                    settings = settings.replaceAll("controlPort=(?:28080|8580)", "controlPort=" + PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort());
                    sup.setSettings(settings);
                    setAssignedPort(PortRegistry.EVO_SUPERVISOR_CONTROL.getServiceId(), PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort());
                    modified = true;
                    Log.log("[PortAllocationManager] Migrated SupervisorSettings controlPort to " + PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort());
                }
            }
        }

        return modified;
    }

    /**
     * Validates that all configured ports satisfy range bounds and have no collisions.
     */
    public List<String> validateIntegrity() {
        List<String> errors = new ArrayList<>();
        Map<Integer, String> seenPorts = new HashMap<>();

        for (PortConfiguration cfg : configurations.values()) {
            int port = cfg.getAssignedPort();
            if (port < 1024 || port > 65535) {
                errors.add("Service [" + cfg.getServiceName() + "] assigned invalid port: " + port);
                continue;
            }
            if (seenPorts.containsKey(port)) {
                errors.add("Internal port collision: Port " + port + " is assigned to both [" + seenPorts.get(port) + "] and [" + cfg.getServiceName() + "]");
            } else {
                seenPorts.put(port, cfg.getServiceName());
            }

            if (PortRegistry.isDebugPort(port)) {
                PortRegistry dbg = PortRegistry.findByDebugPort(port);
                errors.add("Production service [" + cfg.getServiceName() + "] collides with reserved debug port " + port +
                        " [" + (dbg != null ? dbg.getServiceName() : "Debug Service") + "]");
            }
        }

        return errors;
    }
}
