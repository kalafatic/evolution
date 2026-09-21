package eu.kalafatic.evolution.servers.mcp.resources;

import eu.kalafatic.evolution.servers.mcp.model.Resource;
import java.util.*;

public class DemoResources {
    public static class ServerInfoProvider implements ResourceRegistry.ResourceProvider {
        @Override
        public String read(String uri) {
            return "EvoMcpServer v1.0.0 running on NanoHTTPD";
        }
    }

    public static class ServerConfigProvider implements ResourceRegistry.ResourceProvider {
        @Override
        public String read(String uri) {
            return "{\"port\": 68080, \"enableLogging\": true}";
        }
    }

    public static class ConnectorsListProvider implements ResourceRegistry.ResourceProvider {
        @Override
        public String read(String uri) {
            return "[\"DummyConnector\", \"JiraConnector (Stub)\", \"GitHubConnector (Stub)\"]";
        }
    }

    public static void registerAll(ResourceRegistry registry) {
        registry.register(new Resource("server://info", "Server Info", "Information about the MCP server", "text/plain"),
            new ServerInfoProvider());

        registry.register(new Resource("server://config", "Server Configuration", "Current server settings", "application/json"),
            new ServerConfigProvider());

        registry.register(new Resource("connectors://list", "Available Connectors", "List of integrated enterprise connectors", "application/json"),
            new ConnectorsListProvider());
    }
}
