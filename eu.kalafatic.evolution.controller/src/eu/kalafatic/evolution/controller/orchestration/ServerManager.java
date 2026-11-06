package eu.kalafatic.evolution.controller.orchestration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortAvailabilityChecker;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.forge.controller.api.DatasetController;
import eu.kalafatic.evolution.forge.controller.api.ModelController;
import eu.kalafatic.evolution.forge.controller.api.SessionController;
import eu.kalafatic.evolution.forge.controller.api.SnapshotController;
import eu.kalafatic.evolution.forge.controller.api.TrainingController;
import eu.kalafatic.evolution.forge.controller.impl.DatasetControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.ModelControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.SessionControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.SnapshotControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.TrainingControllerImpl;

/**
 * Singleton manager for multi-port EvolutionServer lifecycle.
 * Integrates with authoritative PortRegistry and deterministic collision policy.
 */
public class ServerManager {
    private static ServerManager instance;
    private final Map<Integer, EvolutionServer> activeServers = new ConcurrentHashMap<>();
    private final Map<Integer, eu.kalafatic.evolution.servers.server.EvolutionServer> activeSecondaryServers = new ConcurrentHashMap<>();
    private int primaryPort = PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort();

    private ServerManager() {}

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    public synchronized void start() throws IOException {
        int primary = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_HTTP_PRIMARY.getServiceId());
        start(primary > 0 ? primary : PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort());
    }

    public synchronized void start(int port) throws IOException {
        int mcpPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_MCP.getServiceId());
        int supervisorPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_SUPERVISOR_API.getServiceId());
        int secondaryPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId());

        boolean isMcp = (port == mcpPort) || (port == PortRegistry.EVO_MCP.getDefaultPort()) || (port == 38080);
        boolean isSupervisor = (port == supervisorPort) || (port == PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()) || (port == 8089);
        boolean isSecondary = (port == secondaryPort) || (port == PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort()) || (port == 58080);

        if (!isMcp && !isSupervisor && !isSecondary) {
            java.io.File readyFile = new java.io.File("self-dev-run/evo_ready.txt");
            if (readyFile.exists()) {
                readyFile.delete();
            }
        }

        if (isMcp) {
            eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().setPort(port);
            eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().start();
            System.out.println("Started MCP server on authoritative port " + port);
        } else if (isSupervisor) {
            System.out.println("Starting Supervisor on port " + port + " is handled by the self-dev bootstrap process.");
        } else if (isSecondary) {
            if (activeSecondaryServers.containsKey(port) && activeSecondaryServers.get(port).isRunning()) {
                return; // Already running
            }
            // Strict conflict check before launch
            PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(port, "Secondary Evolution Server (AI Chat / Portal)");
            if (report.isInUse()) {
                Log.log("[ServerManager] " + report.toString());
                throw new IOException(report.toString());
            }

            eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = new eu.kalafatic.evolution.servers.server.EvolutionServer(port);
            secondaryServer.startServer();
            activeSecondaryServers.put(port, secondaryServer);
            System.out.println("Started secondary Evolution server (AI Chat / Portal) on authoritative port " + port);
        } else {
            if (activeServers.containsKey(port) && activeServers.get(port).isAlive()) {
                return; // Already running
            }

            // Strict conflict check before launch
            PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(port, "Primary Evolution Server");
            if (report.isInUse()) {
                Log.log("[ServerManager] " + report.toString());
                throw new IOException(report.toString());
            }

            EvolutionServer server = new EvolutionServer(port);

            // Wire Controllers
            SessionController sc = new SessionControllerImpl(null);
            ModelController mc = new ModelControllerImpl(null);
            DatasetController dc = new DatasetControllerImpl(null);
            TrainingController tc = new TrainingControllerImpl(null);
            SnapshotController snc = new SnapshotControllerImpl(null);
            server.setForgeControllers(sc, mc, dc, tc, snc);

            server.startServer();
            activeServers.put(port, server);
            this.primaryPort = port;

            // Write readiness marker file
            java.io.File runDir = new java.io.File("self-dev-run");
            if (!runDir.exists()) {
                runDir.mkdirs();
            }
            java.io.File readyFile = new java.io.File(runDir, "evo_ready.txt");
            try {
                java.nio.file.Files.write(readyFile.toPath(), String.valueOf(System.currentTimeMillis()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("[ServerManager] Wrote readiness marker file to: " + readyFile.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("[ServerManager] Failed to write readiness marker file: " + e.getMessage());
            }

            // Auto-start MCP Demo Server on authoritative port (28280)
            try {
                int configuredMcpPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_MCP.getServiceId());
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().setPort(configuredMcpPort);
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().start();
            } catch (IOException e) {
                eu.kalafatic.utils.log.Log.log("Could not auto-start MCP Demo Server: " + e.getMessage());
            }

            // Auto-start secondary server on authoritative secondary port (28180)
            int effectiveSecondaryPort = secondaryPort > 0 ? secondaryPort : PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort();
            try {
                if (activeSecondaryServers.containsKey(effectiveSecondaryPort)) {
                    stopSecondary(effectiveSecondaryPort);
                }
                PortAvailabilityChecker.ConflictReport secReport = PortAvailabilityChecker.inspectPort(effectiveSecondaryPort, "Secondary Evolution Server");
                if (!secReport.isInUse()) {
                    eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = new eu.kalafatic.evolution.servers.server.EvolutionServer(effectiveSecondaryPort);
                    secondaryServer.startServer();
                    activeSecondaryServers.put(effectiveSecondaryPort, secondaryServer);
                    System.out.println("Auto-started secondary Evolution server (AI Chat / Portal) on authoritative port " + effectiveSecondaryPort);
                } else {
                    System.err.println("[ServerManager] Cannot auto-start secondary server: " + secReport.toString());
                }
            } catch (Exception e) {
                System.err.println("Failed to auto-start secondary background server on port " + effectiveSecondaryPort + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private synchronized void stopSecondary(int port) {
        eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = activeSecondaryServers.get(port);
        if (secondaryServer != null) {
            secondaryServer.stopServer();
            activeSecondaryServers.remove(port);
        }
    }

    public synchronized void stop(int port) {
        int mcpPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_MCP.getServiceId());
        int supervisorPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_SUPERVISOR_API.getServiceId());
        int secondaryPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId());

        boolean isMcp = (port == mcpPort) || (port == PortRegistry.EVO_MCP.getDefaultPort()) || (port == 38080);
        boolean isSupervisor = (port == supervisorPort) || (port == PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()) || (port == 8089);
        boolean isSecondary = (port == secondaryPort) || (port == PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort()) || (port == 58080);

        if (isMcp) {
            eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().stop();
        } else if (isSupervisor) {
            System.out.println("Stopping Supervisor port " + port + " is handled by destroying the supervisor OS process.");
        } else if (isSecondary) {
            stopSecondary(port);
        } else {
            EvolutionServer server = activeServers.get(port);
            if (server != null) {
                server.stop();
                activeServers.remove(port);
            }
            // Stop active secondary server
            int secPort = secondaryPort > 0 ? secondaryPort : PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort();
            stopSecondary(secPort);
        }
    }

    public synchronized void stop() {
        stop(primaryPort);
    }

    public synchronized void stopAll() {
        for (Integer port : new java.util.HashSet<>(activeServers.keySet())) {
            stop(port);
        }
        for (eu.kalafatic.evolution.servers.server.EvolutionServer s : activeSecondaryServers.values()) {
            try {
                s.stopServer();
            } catch (Exception e) {}
        }
        activeSecondaryServers.clear();
        try {
            eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().stop();
        } catch (Exception e) {}
    }

    public synchronized void restart(int port) throws IOException {
        stop(port);
        start(port);
    }

    public synchronized boolean isRunning(int port) {
        int mcpPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_MCP.getServiceId());
        int supervisorPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_SUPERVISOR_API.getServiceId());
        int secondaryPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_HTTP_SECONDARY.getServiceId());

        boolean isMcp = (port == mcpPort) || (port == PortRegistry.EVO_MCP.getDefaultPort()) || (port == 38080);
        boolean isSupervisor = (port == supervisorPort) || (port == PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()) || (port == 8089);
        boolean isSecondary = (port == secondaryPort) || (port == PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort()) || (port == 58080);

        if (isMcp) {
            return eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().isRunning();
        } else if (isSupervisor) {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 200);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (isSecondary) {
            eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = activeSecondaryServers.get(port);
            return secondaryServer != null && secondaryServer.isRunning();
        } else {
            EvolutionServer server = activeServers.get(port);
            return server != null && server.isAlive();
        }
    }

    public synchronized boolean isAnyRunning() {
        return !activeServers.isEmpty() || !activeSecondaryServers.isEmpty() || eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().isRunning();
    }

    public int getPort() {
        return primaryPort;
    }
    
    public Map<Integer, Boolean> getServerStatuses() {
        Map<Integer, Boolean> statuses = new HashMap<>();
        for (Map.Entry<Integer, EvolutionServer> entry : activeServers.entrySet()) {
            statuses.put(entry.getKey(), entry.getValue().isAlive());
        }
        for (Map.Entry<Integer, eu.kalafatic.evolution.servers.server.EvolutionServer> entry : activeSecondaryServers.entrySet()) {
            statuses.put(entry.getKey(), entry.getValue().isRunning());
        }
        int mcpPort = eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().getPort();
        statuses.put(mcpPort, eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().isRunning());

        int supervisorPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_SUPERVISOR_API.getServiceId());
        statuses.put(supervisorPort, isRunning(supervisorPort));
        return statuses;
    }
}
