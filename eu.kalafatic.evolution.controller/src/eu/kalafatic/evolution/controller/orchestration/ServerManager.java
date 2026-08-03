package eu.kalafatic.evolution.controller.orchestration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 */
public class ServerManager {
    private static ServerManager instance;
    private final Map<Integer, EvolutionServer> activeServers = new ConcurrentHashMap<>();
    private final Map<Integer, eu.kalafatic.evolution.servers.server.EvolutionServer> activeSecondaryServers = new ConcurrentHashMap<>();
    private int primaryPort = 48080;

    private ServerManager() {}

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    public synchronized void start(int port) throws IOException {
        boolean isMcp = port == 38080;
        boolean isSupervisor = port == 8089;
        boolean isSecondary = (port % 10000 == 8080 && port >= 50000) || port == 58080;

        if (isMcp) {
            eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().setPort(port);
            eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().start();
            System.out.println("Started MCP server on port " + port);
        } else if (isSupervisor) {
            System.out.println("Starting Supervisor on port " + port + " is handled by the self-dev bootstrap process.");
        } else if (isSecondary) {
            if (activeSecondaryServers.containsKey(port)) {
                stopSecondary(port);
            }
            eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = new eu.kalafatic.evolution.servers.server.EvolutionServer(port);
            secondaryServer.startServer();
            activeSecondaryServers.put(port, secondaryServer);
            System.out.println("Started secondary Evolution server (AI Chat) on port " + port);
        } else {
            if (activeServers.containsKey(port)) {
                stop(port);
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

            // Auto-start MCP Demo Server
            try {
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().start();
            } catch (IOException e) {
                eu.kalafatic.utils.log.Log.log("Could not auto-start MCP Demo Server: " + e.getMessage());
            }

            // Auto-start secondary server on port + 10000 (e.g. 58080)
            int secondaryPort = port + 10000;
            try {
                if (activeSecondaryServers.containsKey(secondaryPort)) {
                    stopSecondary(secondaryPort);
                }
                eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = new eu.kalafatic.evolution.servers.server.EvolutionServer(secondaryPort);
                secondaryServer.startServer();
                activeSecondaryServers.put(secondaryPort, secondaryServer);
                System.out.println("Auto-started secondary Evolution server (AI Chat) on port " + secondaryPort);
            } catch (Exception e) {
                System.err.println("Failed to auto-start secondary background server on port " + secondaryPort + ": " + e.getMessage());
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
        boolean isMcp = port == 38080;
        boolean isSupervisor = port == 8089;
        boolean isSecondary = (port % 10000 == 8080 && port >= 50000) || port == 58080;

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
            // Also stop corresponding secondary server if it exists
            stopSecondary(port + 10000);
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
        boolean isMcp = port == 38080;
        boolean isSupervisor = port == 8089;
        boolean isSecondary = (port % 10000 == 8080 && port >= 50000) || port == 58080;

        if (isMcp) {
            return eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().isRunning();
        } else if (isSupervisor) {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress("127.0.0.1", 8089), 200);
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
        statuses.put(eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().getPort(),
                     eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().isRunning());
        statuses.put(8089, isRunning(8089));
        return statuses;
    }
}
