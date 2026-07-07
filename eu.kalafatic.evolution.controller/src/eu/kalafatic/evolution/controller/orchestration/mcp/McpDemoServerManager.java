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
    private int port = 38080;

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

        // Check if port is in use
        try (java.net.ServerSocket ss = new java.net.ServerSocket(port)) {
            // Port is free
        } catch (IOException e) {
            throw new IOException("Port " + port + " is already in use. MCP Demo Server cannot start.");
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
