package eu.kalafatic.evolution.servers.server;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.repository.SessionRepository;
import eu.kalafatic.evolution.servers.repository.UserRepository;
import eu.kalafatic.evolution.servers.service.AuthService;
import eu.kalafatic.evolution.servers.service.UserService;
import eu.kalafatic.evolution.servers.controller.AuthController;
import eu.kalafatic.evolution.servers.controller.StaticResourceController;
import eu.kalafatic.evolution.servers.mcp.server.McpServer;
import eu.kalafatic.evolution.servers.mcp.server.McpConfig;
import eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.mcp.tools.DemoTools;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import eu.kalafatic.evolution.servers.mcp.resources.DemoResources;
import eu.kalafatic.evolution.servers.mcp.prompts.PromptRegistry;
import eu.kalafatic.evolution.servers.mcp.prompts.DemoPrompts;
import eu.kalafatic.evolution.servers.mcp.connectors.DummyConnector;

public class EvolutionServer extends NanoHTTPD {
    private final DatabaseManager dbManager;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AuthService authService;
    private final UserService userService;

    private final AuthController authController;
    private final StaticResourceController staticResourceController;

    private McpServer mcpServer;

    public EvolutionServer(int port) {
        super(port);
        this.dbManager = new DatabaseManager();
        this.userRepository = new UserRepository(dbManager);
        this.sessionRepository = new SessionRepository(dbManager);
        this.authService = new AuthService(userRepository, sessionRepository);
        this.userService = new UserService(userRepository);

        this.authController = new AuthController(authService);
        this.staticResourceController = new StaticResourceController();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        if (Method.OPTIONS.equals(session.getMethod())) {
            response = newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
        } else {
            response = handleInternal(session);
        }
        addCorsHeaders(response);
        return response;
    }

    private void addCorsHeaders(Response res) {
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, x-evo-runtime");
    }

    private Response handleInternal(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.startsWith("/api/auth")) {
            return authController.handle(session);
        }

        return staticResourceController.serve(session);
    }

    public void startServer() throws IOException {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Server started on port " + getListeningPort());
        startMcpServer();
    }

    private void startMcpServer() {
        McpConfig config = McpConfig.load();
        ToolRegistry toolRegistry = new ToolRegistry();
        DemoTools.registerAll(toolRegistry);

        DummyConnector dummy = new DummyConnector();
        dummy.registerTools(toolRegistry);

        ResourceRegistry resourceRegistry = new ResourceRegistry();
        DemoResources.registerAll(resourceRegistry);

        PromptRegistry promptRegistry = new PromptRegistry();
        DemoPrompts.registerAll(promptRegistry);

        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(toolRegistry, resourceRegistry, promptRegistry);
        mcpServer = new McpServer(config.getPort(), dispatcher);
        try {
            mcpServer.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (mcpServer != null) {
            mcpServer.stopServer();
        }
        stop();
        System.out.println("Server stopped.");
    }

    public void restartServer() throws IOException {
        stopServer();
        startServer();
    }

    public boolean isRunning() {
        return isAlive();
    }

    public static void main(String[] args) {
        EvolutionServer server = new EvolutionServer(8080);
        try {
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
