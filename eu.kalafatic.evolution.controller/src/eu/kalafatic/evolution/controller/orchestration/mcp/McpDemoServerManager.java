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
        if (demoServer != null) {
            stop();
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
