package eu.kalafatic.evolution.controller.resource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;

/**
 * Durable persistence coordinator for port configurations.
 * Synchronizes with the authoritative EMF model (`ServerSettings`, `SupervisorSettings`, `Orchestrator`)
 * and maintains atomic file backups to survive product exports, restarts, and upgrades.
 */
public class PortPersistence {

    private static final PortPersistence INSTANCE = new PortPersistence();

    private PortPersistence() {}

    public static PortPersistence getInstance() {
        return INSTANCE;
    }

    /**
     * Loads and synchronizes ports from the authoritative EMF Orchestrator model.
     * Executes automatic migration of legacy port configurations.
     */
    public synchronized void loadFromEmf(Orchestrator orchestrator) {
        if (orchestrator == null) {
            return;
        }

        PortAllocationManager pam = PortAllocationManager.getInstance();

        // Check if legacy migration is needed
        boolean migrated = pam.migrateFromLegacy(orchestrator);

        // Read primary server
        ServerSettings srv = orchestrator.getServerSettings();
        if (srv != null && srv.getPort() > 0) {
            try {
                pam.setAssignedPort(PortRegistry.EVO_HTTP_PRIMARY.getServiceId(), srv.getPort());
            } catch (Exception e) {
                Log.log("[PortPersistence] Primary server port invalid: " + e.getMessage());
            }
            if (srv.getPortalPort() > 0) {
                try {
                    pam.setAssignedPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId(), srv.getPortalPort());
                } catch (Exception e) {
                    Log.log("[PortPersistence] Portal port invalid or collision: " + e.getMessage());
                }
            }
            if (srv.getMcpPort() > 0) {
                try {
                    pam.setAssignedPort(PortRegistry.EVO_MCP.getServiceId(), srv.getMcpPort());
                } catch (Exception e) {
                    Log.log("[PortPersistence] MCP port invalid or collision: " + e.getMessage());
                }
            }
        }

        // Read supervisor settings
        SupervisorSettings sup = orchestrator.getSupervisorSettings();
        if (sup != null && sup.getSettings() != null) {
            String s = sup.getSettings();
            if (s.contains("port=")) {
                try {
                    String pStr = s.substring(s.indexOf("port=") + 5).trim();
                    if (pStr.contains(" ") || pStr.contains(";")) {
                        pStr = pStr.split("[ ;]")[0];
                    }
                    int p = Integer.parseInt(pStr);
                    if (p > 0) {
                        pam.setAssignedPort(PortRegistry.EVO_SUPERVISOR_API.getServiceId(), p);
                    }
                } catch (Exception ignored) {}
            }
            if (s.contains("controlPort=")) {
                try {
                    String pStr = s.substring(s.indexOf("controlPort=") + 12).trim();
                    if (pStr.contains(" ") || pStr.contains(";")) {
                        pStr = pStr.split("[ ;]")[0];
                    }
                    int p = Integer.parseInt(pStr);
                    if (p > 0) {
                        pam.setAssignedPort(PortRegistry.EVO_SUPERVISOR_CONTROL.getServiceId(), p);
                    }
                } catch (Exception ignored) {}
            }
        }

        if (migrated) {
            saveToEmf(orchestrator);
        }
    }

    /**
     * Persists active port allocations back into the EMF model and triggers atomic model save.
     */
    public synchronized void saveToEmf(Orchestrator orchestrator) {
        if (orchestrator == null) {
            return;
        }

        PortAllocationManager pam = PortAllocationManager.getInstance();

        ServerSettings srv = orchestrator.getServerSettings();
        if (srv == null) {
            srv = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createServerSettings();
            orchestrator.setServerSettings(srv);
        }

        int primaryPort = pam.getPort(PortRegistry.EVO_HTTP_PRIMARY.getServiceId());
        if (primaryPort > 0) {
            srv.setPort(primaryPort);
        }

        int secondaryPort = pam.getPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId());
        if (secondaryPort > 0) {
            srv.setPortalPort(secondaryPort);
            srv.setPortalUrl("http://localhost:" + secondaryPort + "/dashboard.html");
        }

        int mcpPort = pam.getPort(PortRegistry.EVO_MCP.getServiceId());
        if (mcpPort > 0) {
            srv.setMcpPort(mcpPort);
            orchestrator.setMcpServerUrl("http://localhost:" + mcpPort + "/mcp");
        }

        SupervisorSettings sup = orchestrator.getSupervisorSettings();
        if (sup == null) {
            sup = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createSupervisorSettings();
            orchestrator.setSupervisorSettings(sup);
        }
        int supPort = pam.getPort(PortRegistry.EVO_SUPERVISOR_API.getServiceId());
        int supControlPort = pam.getPort(PortRegistry.EVO_SUPERVISOR_CONTROL.getServiceId());
        sup.setSettings("port=" + supPort + ";controlPort=" + supControlPort);

        try {
            if (ProjectModelManager.getInstance() != null && orchestrator.eResource() != null) {
                ProjectModelManager.getInstance().saveResource(orchestrator.eResource());
                Log.log("[PortPersistence] Successfully persisted port configuration into EMF resource.");
            }
        } catch (Exception e) {
            Log.log("[PortPersistence] Warning: Could not save EMF resource: " + e.getMessage());
        }

        // Save atomic local backup
        saveAtomicBackup(pam.getAllConfigurations());
    }

    /**
     * Persists an atomic JSON/text backup file in the workspace runtime directory.
     */
    public synchronized void saveAtomicBackup(List<PortConfiguration> configs) {
        try {
            File baseDir = new File(System.getProperty("user.home"), ".evo");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            File targetFile = new File(baseDir, "ports.properties");
            File tempFile = new File(baseDir, "ports.properties.tmp");

            StringBuilder sb = new StringBuilder();
            sb.append("# Authoritative EVO Port Configuration\n");
            sb.append("# Automatically generated by EVO PortPersistence\n");
            for (PortConfiguration cfg : configs) {
                sb.append(cfg.getServiceId()).append(".port=").append(cfg.getAssignedPort()).append("\n");
                sb.append(cfg.getServiceId()).append(".defaultPort=").append(cfg.getDefaultPort()).append("\n");
                sb.append(cfg.getServiceId()).append(".enabled=").append(cfg.isEnabled()).append("\n");
                sb.append(cfg.getServiceId()).append(".bindingAddress=").append(cfg.getBindingAddress()).append("\n");
                sb.append(cfg.getServiceId()).append(".protocol=").append(cfg.getProtocol()).append("\n");
            }

            Files.writeString(tempFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Log.log("[PortPersistence] Atomic backup write failed: " + e.getMessage());
        }
    }
}
