package eu.kalafatic.evolution.controller.orchestration.mcp;

import java.io.IOException;
import eu.kalafatic.evolution.servers.mcp.demo.DemoConfiguration;
import eu.kalafatic.evolution.servers.mcp.demo.DemoDocumentationProvider;

/**
 * Singleton manager for the MCP Demo Documentation Server.
 */
public class McpDemoServerManager {
    private static McpDemoServerManager instance;
    private DemoDocumentationProvider demoServer;
    private int port = 28280;

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

        DemoConfiguration config = new DemoConfiguration();
        config.setPort(port);
        config.setHost("localhost");
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
