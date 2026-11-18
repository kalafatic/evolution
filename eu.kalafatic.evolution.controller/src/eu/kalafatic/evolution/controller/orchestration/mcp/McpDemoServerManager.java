package eu.kalafatic.evolution.controller.orchestration.mcp;

import java.io.IOException;
import java.util.logging.Logger;

import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.mcp.demo.DemoConfiguration;
import eu.kalafatic.evolution.servers.mcp.demo.DemoDocumentationProvider;
import eu.kalafatic.evolution.servers.mcp.server.BearerTokenAuthenticator;
import eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher;
import eu.kalafatic.evolution.servers.service.AuthorizationService;

/**
 * Singleton manager for the MCP Demo Documentation Server.
 *
 * <p>The 28280 MCP server hosts the Cognitive Services tool set:
 * authentication (bearer application/user tokens), scope enforcement,
 * cognitive tools/resources and auditing are wired on the existing
 * dispatcher. Demo documentation resources are preserved.</p>
 */
public class McpDemoServerManager {
    private static final Logger LOGGER = Logger.getLogger(McpDemoServerManager.class.getName());

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

        int configuredPort = PortAllocationManager.getInstance().getPort(PortRegistry.EVO_MCP.getServiceId());
        int effectivePort = (this.port > 0) ? this.port : (configuredPort > 0 ? configuredPort : PortRegistry.EVO_MCP.getDefaultPort());
        this.port = effectivePort;

        eu.kalafatic.evolution.controller.resource.PortAvailabilityChecker.ConflictReport report =
                eu.kalafatic.evolution.controller.resource.PortAvailabilityChecker.inspectPort(effectivePort, "MCP Demo Documentation Server");
        if (report.isInUse()) {
            throw new IOException(report.toString());
        }

        String bindAddress = eu.kalafatic.evolution.controller.resource.NetworkInterfaceHelper.resolveBindAddress(
                PortRegistry.EVO_MCP.getServiceId(), PortRegistry.EVO_MCP.getDefaultHost());

        DemoConfiguration config = new DemoConfiguration();
        config.setPort(effectivePort);
        config.setHost(bindAddress);
        config.setDocsFolder("docs");
        config.setEnabled(true);

        demoServer = new DemoDocumentationProvider(config);
        wireCognitiveServices();
        demoServer.startServer();
    }

    /**
     * Wires authentication, authorization and the cognitive tools/resources
     * onto the dispatcher of the 28280 server. Cognitive tools require a
     * valid bearer token with the tool's scope; the demo documentation tools
     * keep their legacy behavior.
     */
    private void wireCognitiveServices() {
        try {
            if (demoServer == null || demoServer.getController() == null) {
                return;
            }
            JsonRpcDispatcher dispatcher = demoServer.getController().getMcpDispatcher();
            if (dispatcher == null) {
                LOGGER.warning("[MCP 28280] Dispatcher unavailable - cognitive services not wired");
                return;
            }
            DatabaseManager dbManager = new DatabaseManager();
            dispatcher.setAuthorizationService(AuthorizationService.getInstance(dbManager));
            dispatcher.setAuthenticator(new BearerTokenAuthenticator());
            dispatcher.setEnforcePromptAuthorization(true);
            CognitiveMcpToolProvider.registerCognitiveTools(dispatcher.getToolRegistry(), dispatcher);
            CognitiveMcpToolProvider.registerCognitiveResources(dispatcher.getResourceRegistry());
            LOGGER.info("[MCP 28280] Cognitive Services wired: authentication + authorization + tools + resources");
        } catch (Exception e) {
            LOGGER.severe("[MCP 28280] Failed to wire cognitive services: " + e.getMessage());
        }
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

    /** Test/diagnostic access to the demo server provider. */
    public DemoDocumentationProvider getDemoServer() {
        return demoServer;
    }
}
