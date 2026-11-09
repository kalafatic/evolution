package eu.kalafatic.evolution.controller.orchestration.mcp;

import java.io.IOException;

import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.servers.mcp.demo.DemoConfiguration;
import eu.kalafatic.evolution.servers.mcp.demo.DemoDocumentationProvider;

/**
 * Singleton manager for the MCP Demo Documentation Server.
 */
public class McpDemoServerManager {
    private static McpDemoServerManager instance;
    private DemoDocumentationProvider demoServer;
    private int port = PortRegistry.EVO_MCP.getDefaultPort();

    private McpDemoServerManager() {}

    public static synchronized McpDemoServerManager getInstance() {
        if (instance == null) {
            instance = new McpDemoServerManager();
        }
        return instance;
    }

    public synchronized void start() throws IOException {
        if (demoServer != null && demoServer.isAlive()) {
            return; // Already running
        }

        eu.kalafatic.evolution.controller.resource.PortAvailabilityChecker.ConflictReport report =
                eu.kalafatic.evolution.controller.resource.PortAvailabilityChecker.inspectPort(port, "MCP Demo Documentation Server");
        if (report.isInUse()) {
            throw new IOException(report.toString());
        }

        int configuredPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_MCP.getServiceId());
        int effectivePort = (port > 0) ? port : (configuredPort > 0 ? configuredPort : PortRegistry.EVO_MCP.getDefaultPort());

        DemoConfiguration config = new DemoConfiguration();
        config.setPort(effectivePort);
        config.setHost(PortRegistry.EVO_MCP.getDefaultHost());
        config.setDocsFolder("docs");
        config.setEnabled(true);

        demoServer = new DemoDocumentationProvider(config);
        demoServer.startServer();
    }

    public synchronized void stop() {
        if (demoServer != null) {
            demoServer.stopServer();
            demoServer = null;
        }
    }

    public synchronized boolean isRunning() {
        return demoServer != null && demoServer.isAlive();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
