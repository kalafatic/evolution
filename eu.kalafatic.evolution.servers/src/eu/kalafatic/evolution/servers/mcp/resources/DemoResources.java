package eu.kalafatic.evolution.servers.mcp.resources;

import eu.kalafatic.evolution.servers.mcp.model.Resource;
import java.util.*;

public class DemoResources {
    public static void registerAll(ResourceRegistry registry) {
        registry.register(new Resource("server://info", "Server Info", "Information about the MCP server", "text/plain"),
            uri -> "EvoMcpServer v1.0.0 running on NanoHTTPD");

        registry.register(new Resource("server://config", "Server Configuration", "Current server settings", "application/json"),
            uri -> "{\"port\": 68080, \"enableLogging\": true}");

        registry.register(new Resource("connectors://list", "Available Connectors", "List of integrated enterprise connectors", "application/json"),
            uri -> "[\"DummyConnector\", \"JiraConnector (Stub)\", \"GitHubConnector (Stub)\"]");
    }
}
