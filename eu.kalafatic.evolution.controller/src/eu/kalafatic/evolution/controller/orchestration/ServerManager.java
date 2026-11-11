package eu.kalafatic.evolution.controller.orchestration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorRuntime;
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
 * Single authoritative manager for multi-port EvolutionServer lifecycle and web applications.
 * Integrates with PortRegistry, PortAllocationManager, and real readiness checks.
 */
public class ServerManager {
    private static ServerManager instance;

    // Active server instances
    private final Map<Integer, EvolutionServer> activeServers = new ConcurrentHashMap<>();
    private final Map<Integer, eu.kalafatic.evolution.servers.server.EvolutionServer> activeSecondaryServers = new ConcurrentHashMap<>();
    private final SupervisorRuntime supervisorRuntime = new SupervisorRuntime();

    // Server entries & runtime states by canonical port
    private final Map<Integer, ServerEntry> serverEntries = new ConcurrentHashMap<>();

    // Registered Web Applications
    private final Map<String, WebApplicationDescriptor> webApplications = new LinkedHashMap<>();

    // Listeners
    private final List<ServerLifecycleListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicBoolean autoStarting = new AtomicBoolean(false);
    private int primaryPort = PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort();

    private ServerManager() {
        initializeCanonicalEntries();
        initializeWebApplications();
    }

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    /**
     * Initializes ServerEntry records for all canonical ports.
     */
    private void initializeCanonicalEntries() {
        int primary = getPortForService(PortRegistry.EVO_HTTP_PRIMARY);
        int secondary = getPortForService(PortRegistry.EVO_HTTP_SECONDARY);
        int mcp = getPortForService(PortRegistry.EVO_MCP);
        int develop = getPortForService(PortRegistry.EVO_DEVELOP);
        int supervisorApi = getPortForService(PortRegistry.EVO_SUPERVISOR_API);
        int supervisorControl = getPortForService(PortRegistry.EVO_SUPERVISOR_CONTROL);

        registerServerEntry(primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId(), PortRegistry.EVO_HTTP_PRIMARY.getServiceName(), "HTTP", "http://127.0.0.1:" + primary + "/");
        registerServerEntry(secondary, PortRegistry.EVO_HTTP_SECONDARY.getServiceId(), PortRegistry.EVO_HTTP_SECONDARY.getServiceName(), "HTTP", "http://127.0.0.1:" + secondary + "/dashboard.html");
        registerServerEntry(mcp, PortRegistry.EVO_MCP.getServiceId(), PortRegistry.EVO_MCP.getServiceName(), "HTTP", "http://127.0.0.1:" + mcp + "/");
        registerServerEntry(develop, PortRegistry.EVO_DEVELOP.getServiceId(), PortRegistry.EVO_DEVELOP.getServiceName(), "HTTP", "http://127.0.0.1:" + develop + "/develop");
        registerServerEntry(supervisorApi, PortRegistry.EVO_SUPERVISOR_API.getServiceId(), PortRegistry.EVO_SUPERVISOR_API.getServiceName(), "HTTP", "http://127.0.0.1:" + supervisorApi + "/ping");
        registerServerEntry(supervisorControl, PortRegistry.EVO_SUPERVISOR_CONTROL.getServiceId(), PortRegistry.EVO_SUPERVISOR_CONTROL.getServiceName(), "HTTP/TCP", "http://127.0.0.1:" + supervisorControl + "/");
    }

    private int getPortForService(PortRegistry reg) {
        int assigned = PortAllocationManager.getInstance().getPort(reg.getServiceId());
        return (assigned > 0) ? assigned : reg.getDefaultPort();
    }

    private void registerServerEntry(int port, String id, String name, String type, String endpointUrl) {
        ServerEntry entry = new ServerEntry(id, name, type, port, "127.0.0.1", endpointUrl, ServerState.STOPPED);
        serverEntries.put(port, entry);
    }

    /**
     * Registers standard EVO web applications.
     */
    private void initializeWebApplications() {
        int primary = getPortForService(PortRegistry.EVO_HTTP_PRIMARY);
        int secondary = getPortForService(PortRegistry.EVO_HTTP_SECONDARY);
        int mcp = getPortForService(PortRegistry.EVO_MCP);
        int dev = getPortForService(PortRegistry.EVO_DEVELOP);

        // Web Applications hosted on Primary HTTP (and shared with secondary/develop endpoints)
        registerWebApp(new WebApplicationDescriptor("chat", "AI Chat", "/experimental/chat?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("dashboard", "Intelligence Portal", "/dashboard.html?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("develop", "Develop Agent", "/develop?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("mcp", "MCP Studio", "/mcp.html?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("forge", "Forge Studio", "/experimental/forge?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("architecture", "Architecture", "/experimental/architecture?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("mutation", "Mutation", "/mutation?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("admin", "Administration", "/admin.html?runtime=SWT", primary, PortRegistry.EVO_HTTP_PRIMARY.getServiceId()));
        registerWebApp(new WebApplicationDescriptor("mcp_docs", "MCP Documentation", "/", mcp, PortRegistry.EVO_MCP.getServiceId()));
    }

    public synchronized void registerWebApp(WebApplicationDescriptor desc) {
        webApplications.put(desc.getId(), desc);
    }

    public synchronized List<WebApplicationDescriptor> getWebApplications() {
        return new ArrayList<>(webApplications.values());
    }

    public synchronized List<ServerEntry> getServerEntries() {
        return new ArrayList<>(serverEntries.values());
    }

    public ServerEntry getServerEntry(int port) {
        return serverEntries.get(port);
    }

    public ServerEntry getServerEntryByServiceId(String serviceId) {
        for (ServerEntry entry : serverEntries.values()) {
            if (entry.getServiceId().equalsIgnoreCase(serviceId)) {
                return entry;
            }
        }
        return null;
    }

    public void addListener(ServerLifecycleListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            // Immediately notify the newly registered listener of current state
            for (ServerEntry entry : serverEntries.values()) {
                try {
                    listener.onServerStateChanged(entry);
                } catch (Throwable t) {
                    Log.log("[ServerManager] Error notifying listener: " + t.getMessage());
                }
            }
            for (WebApplicationDescriptor webApp : webApplications.values()) {
                if (isPortRunning(webApp.getServerPort())) {
                    try {
                        listener.onWebApplicationReady(webApp);
                    } catch (Throwable t) {
                        Log.log("[ServerManager] Error notifying webApp ready: " + t.getMessage());
                    }
                }
            }
        }
    }

    public void removeListener(ServerLifecycleListener listener) {
        listeners.remove(listener);
    }

    private void updateState(int port, ServerState state, String lastError) {
        ServerEntry entry = serverEntries.get(port);
        if (entry != null) {
            entry.setState(state);
            entry.setLastError(lastError);
            notifyStateChanged(entry);
        }
    }

    private void notifyStateChanged(ServerEntry entry) {
        for (ServerLifecycleListener l : listeners) {
            try {
                l.onServerStateChanged(entry);
            } catch (Throwable t) {
                Log.log("[ServerManager] Listener error on state change: " + t.getMessage());
            }
        }
        // Update web applications bound to this port
        boolean isRunning = (entry.getState() == ServerState.RUNNING);
        for (WebApplicationDescriptor app : webApplications.values()) {
            if (app.getServerPort() == entry.getPort()) {
                for (ServerLifecycleListener l : listeners) {
                    try {
                        if (isRunning) {
                            l.onWebApplicationReady(app);
                        } else if (entry.getState() == ServerState.STOPPED || entry.getState() == ServerState.FAILED) {
                            l.onWebApplicationUnavailable(app);
                        }
                    } catch (Throwable t) {
                        Log.log("[ServerManager] Listener error on web app readiness: " + t.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Non-blocking automatic startup of all configured EVO servers and web applications.
     */
    public CompletableFuture<Void> autoStartAll() {
        return CompletableFuture.runAsync(() -> {
            if (!autoStarting.compareAndSet(false, true)) {
                Log.log("[ServerManager] autoStartAll already in progress.");
                return;
            }
            try {
                Log.log("[ServerManager] Starting automatic startup of all configured EVO servers...");

                // 1. Primary HTTP Server (28080)
                int primary = getPortForService(PortRegistry.EVO_HTTP_PRIMARY);
                try {
                    start(primary);
                } catch (Throwable t) {
                    Log.log("[ServerManager] Primary server auto-start failed: " + t.getMessage());
                }

                // 2. Secondary HTTP / Portal Server (28180)
                int secondary = getPortForService(PortRegistry.EVO_HTTP_SECONDARY);
                try {
                    start(secondary);
                } catch (Throwable t) {
                    Log.log("[ServerManager] Secondary server auto-start failed: " + t.getMessage());
                }

                // 3. MCP Demo Server (28280)
                int mcp = getPortForService(PortRegistry.EVO_MCP);
                try {
                    start(mcp);
                } catch (Throwable t) {
                    Log.log("[ServerManager] MCP server auto-start failed: " + t.getMessage());
                }

                // 4. Develop Autonomous Coding Agent Endpoint (28380)
                int develop = getPortForService(PortRegistry.EVO_DEVELOP);
                try {
                    start(develop);
                } catch (Throwable t) {
                    Log.log("[ServerManager] Develop endpoint auto-start check failed: " + t.getMessage());
                }

                // 5. Supervisor API (28480) & Supervisor Control (28580)
                int supervisorApi = getPortForService(PortRegistry.EVO_SUPERVISOR_API);
                try {
                    start(supervisorApi);
                } catch (Throwable t) {
                    Log.log("[ServerManager] Supervisor API auto-start notice: " + t.getMessage());
                }

                int supervisorControl = getPortForService(PortRegistry.EVO_SUPERVISOR_CONTROL);
                try {
                    start(supervisorControl);
                } catch (Throwable t) {
                    Log.log("[ServerManager] Supervisor Control auto-start notice: " + t.getMessage());
                }

                Log.log("[ServerManager] Automatic server startup sequence finished.");
                List<ServerEntry> entriesList = getServerEntries();
                for (ServerLifecycleListener l : listeners) {
                    try {
                        l.onAllServersStarted(entriesList);
                    } catch (Throwable t) {
                        Log.log("[ServerManager] Listener error on onAllServersStarted: " + t.getMessage());
                    }
                }
            } finally {
                autoStarting.set(false);
            }
        });
    }

    public synchronized void start() throws IOException {
        int primary = getPortForService(PortRegistry.EVO_HTTP_PRIMARY);
        start(primary);
    }

    public synchronized void start(int port) throws IOException {
        int primary = getPortForService(PortRegistry.EVO_HTTP_PRIMARY);
        int secondaryPort = getPortForService(PortRegistry.EVO_HTTP_SECONDARY);
        int mcpPort = getPortForService(PortRegistry.EVO_MCP);
        int developPort = getPortForService(PortRegistry.EVO_DEVELOP);
        int supervisorPort = getPortForService(PortRegistry.EVO_SUPERVISOR_API);
        int controlPort = getPortForService(PortRegistry.EVO_SUPERVISOR_CONTROL);

        boolean isMcp = (port == mcpPort) || (port == PortRegistry.EVO_MCP.getDefaultPort()) || (port == 38080);
        boolean isSupervisor = (port == supervisorPort) || (port == PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()) || (port == 8089);
        boolean isSupervisorControl = (port == controlPort) || (port == PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort()) || (port == 28580) || (port == 8580);
        boolean isSecondary = (port == secondaryPort) || (port == PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort()) || (port == 58080);
        boolean isDevelop = (port == developPort) || (port == PortRegistry.EVO_DEVELOP.getDefaultPort()) || (port == 8380);

        updateState(port, ServerState.STARTING, null);

        if (isSupervisorControl) {
            // Supervisor Control TCP is handled by supervisor OS process
            boolean running = verifySocketConnection("127.0.0.1", port, 300);
            updateState(port, running ? ServerState.RUNNING : ServerState.STOPPED, null);
            return;
        }

        if (isSupervisor) {
            // Supervisor process check / start
            boolean running = verifySocketConnection("127.0.0.1", port, 300);
            if (!running) {
                try {
                    SelfDevContext sdContext = new SelfDevContext(new java.io.File("."), null);
                    supervisorRuntime.start(sdContext);
                    running = verifySocketConnection("127.0.0.1", port, 1000);
                } catch (Throwable t) {
                    Log.log("[ServerManager] Could not launch supervisor process: " + t.getMessage());
                }
            }
            updateState(port, running ? ServerState.RUNNING : ServerState.STOPPED, null);
            return;
        }

        if (isDevelop) {
            // Develop endpoint is served by the Primary EvolutionServer, or if separate port requested:
            if (port != primary) {
                // If distinct port from primary, start an EvolutionServer instance on developPort
                if (!activeServers.containsKey(port) || !activeServers.get(port).isAlive()) {
                    PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(port, "Autonomous Develop Server");
                    if (!report.isInUse()) {
                        EvolutionServer devServer = new EvolutionServer(port);
                        devServer.startServer();
                        activeServers.put(port, devServer);
                    }
                }
            }
            boolean ready = verifyHttpEndpoint("http://127.0.0.1:" + port + "/develop", 2000) || isPortRunning(port);
            updateState(port, ready ? ServerState.RUNNING : ServerState.FAILED, ready ? null : "Develop endpoint unreachable");
            return;
        }

        if (isMcp) {
            try {
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().setPort(port);
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().start();
                boolean ready = verifySocketConnection("127.0.0.1", port, 2000);
                updateState(port, ready ? ServerState.RUNNING : ServerState.FAILED, ready ? null : "MCP server port unreachable");
                System.out.println("Started MCP server on authoritative port " + port);
            } catch (Exception e) {
                updateState(port, ServerState.FAILED, e.getMessage());
                throw (e instanceof IOException) ? (IOException) e : new IOException(e);
            }
            return;
        }

        if (isSecondary) {
            if (activeSecondaryServers.containsKey(port) && activeSecondaryServers.get(port).isRunning()) {
                updateState(port, ServerState.RUNNING, null);
                return;
            }
            try {
                PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(port, "Secondary Evolution Server (AI Chat / Portal)");
                if (report.isInUse()) {
                    Log.log("[ServerManager] " + report.toString());
                    updateState(port, ServerState.FAILED, report.toString());
                    throw new IOException(report.toString());
                }

                eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = new eu.kalafatic.evolution.servers.server.EvolutionServer(port);
                secondaryServer.startServer();
                activeSecondaryServers.put(port, secondaryServer);
                boolean ready = verifySocketConnection("127.0.0.1", port, 2000);
                updateState(port, ready ? ServerState.RUNNING : ServerState.FAILED, ready ? null : "Secondary server port unreachable");
                System.out.println("Started secondary Evolution server (AI Chat / Portal) on authoritative port " + port);
            } catch (Exception e) {
                updateState(port, ServerState.FAILED, e.getMessage());
                throw (e instanceof IOException) ? (IOException) e : new IOException(e);
            }
            return;
        }

        // Primary HTTP Server
        java.io.File readyFile = new java.io.File("self-dev-run/evo_ready.txt");
        if (readyFile.exists()) {
            readyFile.delete();
        }

        if (activeServers.containsKey(port) && activeServers.get(port).isAlive()) {
            updateState(port, ServerState.RUNNING, null);
            return;
        }

        try {
            PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(port, "Primary Evolution Server");
            if (report.isInUse()) {
                Log.log("[ServerManager] " + report.toString());
                updateState(port, ServerState.FAILED, report.toString());
                throw new IOException(report.toString());
            }

            EvolutionServer server = new EvolutionServer(port);

            // Wire Forge Controllers
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
            java.io.File rf = new java.io.File(runDir, "evo_ready.txt");
            try {
                java.nio.file.Files.write(rf.toPath(), String.valueOf(System.currentTimeMillis()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) {}

            boolean ready = verifyHttpEndpoint("http://127.0.0.1:" + port + "/server/status", 2500) || verifySocketConnection("127.0.0.1", port, 2000);
            updateState(port, ready ? ServerState.RUNNING : ServerState.FAILED, ready ? null : "Server did not answer readiness probe");
            System.out.println("Started primary Evolution server on authoritative port " + port);

            // If Develop port equals primary port, mark Develop as RUNNING too
            if (developPort == port) {
                updateState(developPort, ready ? ServerState.RUNNING : ServerState.FAILED, null);
            }
        } catch (Exception e) {
            updateState(port, ServerState.FAILED, e.getMessage());
            throw (e instanceof IOException) ? (IOException) e : new IOException(e);
        }
    }

    private synchronized void stopSecondary(int port) {
        eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = activeSecondaryServers.get(port);
        if (secondaryServer != null) {
            try {
                secondaryServer.stopServer();
            } catch (Exception e) {
                Log.log("[ServerManager] Error stopping secondary server: " + e.getMessage());
            }
            activeSecondaryServers.remove(port);
        }
        updateState(port, ServerState.STOPPED, null);
    }

    public synchronized void stop(int port) {
        int primary = getPortForService(PortRegistry.EVO_HTTP_PRIMARY);
        int mcpPort = getPortForService(PortRegistry.EVO_MCP);
        int supervisorPort = getPortForService(PortRegistry.EVO_SUPERVISOR_API);
        int secondaryPort = getPortForService(PortRegistry.EVO_HTTP_SECONDARY);
        int controlPort = getPortForService(PortRegistry.EVO_SUPERVISOR_CONTROL);
        int developPort = getPortForService(PortRegistry.EVO_DEVELOP);

        boolean isMcp = (port == mcpPort) || (port == PortRegistry.EVO_MCP.getDefaultPort()) || (port == 38080);
        boolean isSupervisor = (port == supervisorPort) || (port == PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()) || (port == 8089);
        boolean isSupervisorControl = (port == controlPort) || (port == PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort()) || (port == 28580) || (port == 8580);
        boolean isSecondary = (port == secondaryPort) || (port == PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort()) || (port == 58080);
        boolean isDevelop = (port == developPort) || (port == PortRegistry.EVO_DEVELOP.getDefaultPort()) || (port == 8380);

        updateState(port, ServerState.STOPPING, null);

        if (isSupervisorControl || isSupervisor) {
            try {
                SelfDevContext sdContext = new SelfDevContext(new java.io.File("."), null);
                supervisorRuntime.stop(sdContext);
            } catch (Throwable t) {
                Log.log("[ServerManager] Error stopping supervisor: " + t.getMessage());
            }
            updateState(port, ServerState.STOPPED, null);
        } else if (isMcp) {
            try {
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().stop();
            } catch (Throwable t) {
                Log.log("[ServerManager] Error stopping MCP server: " + t.getMessage());
            }
            updateState(port, ServerState.STOPPED, null);
        } else if (isSecondary) {
            stopSecondary(port);
        } else if (isDevelop && port != primary) {
            EvolutionServer s = activeServers.get(port);
            if (s != null) {
                s.stopServer();
                activeServers.remove(port);
            }
            updateState(port, ServerState.STOPPED, null);
        } else {
            EvolutionServer server = activeServers.get(port);
            if (server != null) {
                server.stopServer();
                activeServers.remove(port);
            }
            updateState(port, ServerState.STOPPED, null);
            if (developPort == port) {
                updateState(developPort, ServerState.STOPPED, null);
            }
        }
    }

    public synchronized void stop() {
        stop(primaryPort);
    }

    public synchronized void stopAll() {
        for (Integer port : new java.util.HashSet<>(activeServers.keySet())) {
            stop(port);
        }
        for (Integer port : new java.util.HashSet<>(activeSecondaryServers.keySet())) {
            stopSecondary(port);
        }
        activeSecondaryServers.clear();
        try {
            eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().stop();
        } catch (Exception ignored) {}

        for (ServerEntry entry : serverEntries.values()) {
            entry.setState(ServerState.STOPPED);
            notifyStateChanged(entry);
        }
    }

    public synchronized void restart(int port) throws IOException {
        stop(port);
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}
        start(port);
    }

    public boolean isPortRunning(int port) {
        ServerEntry entry = serverEntries.get(port);
        if (entry != null && entry.getState() == ServerState.RUNNING) {
            return true;
        }
        return isRunning(port);
    }

    public synchronized boolean isRunning(int port) {
        int mcpPort = getPortForService(PortRegistry.EVO_MCP);
        int supervisorPort = getPortForService(PortRegistry.EVO_SUPERVISOR_API);
        int secondaryPort = getPortForService(PortRegistry.EVO_HTTP_SECONDARY);
        int controlPort = getPortForService(PortRegistry.EVO_SUPERVISOR_CONTROL);
        int developPort = getPortForService(PortRegistry.EVO_DEVELOP);

        boolean isMcp = (port == mcpPort) || (port == PortRegistry.EVO_MCP.getDefaultPort()) || (port == 38080);
        boolean isSupervisor = (port == supervisorPort) || (port == PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()) || (port == 8089);
        boolean isSupervisorControl = (port == controlPort) || (port == PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort()) || (port == 28580) || (port == 8580);
        boolean isSecondary = (port == secondaryPort) || (port == PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort()) || (port == 58080);
        boolean isDevelop = (port == developPort) || (port == PortRegistry.EVO_DEVELOP.getDefaultPort()) || (port == 8380);

        if (isMcp) {
            return eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().isRunning();
        } else if (isSupervisor || isSupervisorControl) {
            return verifySocketConnection("127.0.0.1", port, 200);
        } else if (isSecondary) {
            eu.kalafatic.evolution.servers.server.EvolutionServer secondaryServer = activeSecondaryServers.get(port);
            return secondaryServer != null && secondaryServer.isRunning();
        } else if (isDevelop) {
            int primary = getPortForService(PortRegistry.EVO_HTTP_PRIMARY);
            if (port == primary) {
                EvolutionServer s = activeServers.get(port);
                return s != null && s.isAlive();
            }
            EvolutionServer s = activeServers.get(port);
            return (s != null && s.isAlive()) || verifySocketConnection("127.0.0.1", port, 200);
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
        for (ServerEntry entry : serverEntries.values()) {
            statuses.put(entry.getPort(), entry.getState() == ServerState.RUNNING);
        }
        return statuses;
    }

    /**
     * Re-evaluates live readiness of all registered servers and synchronizes server entries.
     */
    public synchronized void refreshRuntimeStates() {
        for (ServerEntry entry : serverEntries.values()) {
            boolean alive = isRunning(entry.getPort());
            if (alive) {
                entry.setState(ServerState.RUNNING);
                entry.setLastError(null);
            } else if (entry.getState() == ServerState.RUNNING) {
                entry.setState(ServerState.STOPPED);
            }
            notifyStateChanged(entry);
        }
    }

    private boolean verifySocketConnection(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyHttpEndpoint(String endpointUrl, int timeoutMs) {
        try {
            URL url = new URI(endpointUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            return false;
        }
    }
}
