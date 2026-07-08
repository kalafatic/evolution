package eu.kalafatic.evolution.servers.mcp.server;

import eu.kalafatic.evolution.servers.mcp.protocol.McpRequest;
import eu.kalafatic.evolution.servers.mcp.protocol.McpResponse;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import eu.kalafatic.evolution.servers.mcp.prompts.PromptRegistry;
import eu.kalafatic.evolution.servers.mcp.model.Implementation;
import java.util.HashMap;
import java.util.Map;

public class JsonRpcDispatcher {
    private final ToolRegistry toolRegistry;
    private final ResourceRegistry resourceRegistry;
    private final PromptRegistry promptRegistry;

    public JsonRpcDispatcher(ToolRegistry toolRegistry, ResourceRegistry resourceRegistry, PromptRegistry promptRegistry) {
        this.toolRegistry = toolRegistry;
        this.resourceRegistry = resourceRegistry;
        this.promptRegistry = promptRegistry;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public McpResponse dispatch(McpRequest request) {
        String method = request.getMethod();
        Object id = request.getId();

        try {
            switch (method) {
                case "initialize":
                    Map<String, Object> initResult = new HashMap<>();
                    initResult.put("protocolVersion", "2024-11-05");
                    initResult.put("capabilities", new HashMap<>());
                    initResult.put("serverInfo", new Implementation("EvoMcpServer", "1.0.0"));
                    return McpResponse.success(id, initResult);

                case "tools/list":
                    Map<String, Object> toolsResult = new HashMap<>();
                    toolsResult.put("tools", toolRegistry.listTools());
                    return McpResponse.success(id, toolsResult);

                case "tools/call":
                    return McpResponse.success(id, toolRegistry.callTool(request.getParams()));

                case "resources/list":
                    Map<String, Object> resourcesResult = new HashMap<>();
                    resourcesResult.put("resources", resourceRegistry.listResources());
                    return McpResponse.success(id, resourcesResult);

                case "resources/read":
                    return McpResponse.success(id, resourceRegistry.readResource(request.getParams()));

                case "prompts/list":
                    Map<String, Object> promptsResult = new HashMap<>();
                    promptsResult.put("prompts", promptRegistry.listPrompts());
                    return McpResponse.success(id, promptsResult);

                case "prompts/get":
                    return McpResponse.success(id, promptRegistry.getPrompt(request.getParams()));

                case "ping":
                    return McpResponse.success(id, new HashMap<>());

                case "shutdown":
                    // Handle shutdown if needed
                    return McpResponse.success(id, new HashMap<>());

                default:
                    return McpResponse.error(id, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            return McpResponse.error(id, -32603, e.getMessage());
        }
    }
}
