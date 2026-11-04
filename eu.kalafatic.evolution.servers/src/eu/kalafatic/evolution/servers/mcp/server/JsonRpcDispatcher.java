package eu.kalafatic.evolution.servers.mcp.server;

import eu.kalafatic.evolution.servers.mcp.protocol.McpRequest;
import eu.kalafatic.evolution.servers.mcp.protocol.McpResponse;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import eu.kalafatic.evolution.servers.mcp.prompts.PromptRegistry;
import eu.kalafatic.evolution.servers.mcp.model.Implementation;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;
import eu.kalafatic.evolution.servers.service.AuthorizationService;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class JsonRpcDispatcher {
    private final ToolRegistry toolRegistry;
    private final ResourceRegistry resourceRegistry;
    private final PromptRegistry promptRegistry;
    private AuthorizationService authorizationService;

    public JsonRpcDispatcher(ToolRegistry toolRegistry, ResourceRegistry resourceRegistry, PromptRegistry promptRegistry) {
        this.toolRegistry = toolRegistry;
        this.resourceRegistry = resourceRegistry;
        this.promptRegistry = promptRegistry;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public ResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

    public PromptRegistry getPromptRegistry() {
        return promptRegistry;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public McpResponse dispatch(McpRequest request) {
        return dispatch(request, McpExecutionContext.system());
    }

    public McpResponse dispatch(McpRequest request, McpExecutionContext context) {
        if (context == null) {
            context = McpExecutionContext.system();
        }
        String method = request != null ? request.getMethod() : null;
        Object id = request != null ? request.getId() : null;

        if (method == null) {
            return McpResponse.error(id, -32600, "Invalid Request: missing method");
        }

        String principal = context.getPrincipal();

        try {
            switch (method) {
                case "initialize":
                    Map<String, Object> initResult = new HashMap<>();
                    initResult.put("protocolVersion", "2024-11-05");
                    Map<String, Object> caps = new HashMap<>();
                    caps.put("tools", Map.of("listChanged", true));
                    caps.put("resources", Map.of("subscribe", false, "listChanged", true));
                    caps.put("prompts", Map.of("listChanged", true));
                    initResult.put("capabilities", caps);
                    initResult.put("serverInfo", new Implementation("EvoMcpServer", "1.0.0"));
                    return McpResponse.success(id, initResult);

                case "tools/list":
                    // Authorization check for tools discovery
                    if (authorizationService != null && !authorizationService.authorize(principal, "MCP_TOOL", "*", "READ")) {
                        logAudit(principal, "TOOLS_LIST", "MCP_TOOL", "*", "DENIED", "Unauthorized tools discovery");
                        return McpResponse.error(id, -32001, "Unauthorized: User lacks permission to list MCP tools");
                    }
                    Map<String, Object> toolsResult = new HashMap<>();
                    toolsResult.put("tools", toolRegistry.listTools());
                    return McpResponse.success(id, toolsResult);

                case "tools/call":
                    if (request.getParams() == null || !request.getParams().has("name")) {
                        return McpResponse.error(id, -32602, "Invalid params: missing tool name");
                    }
                    String toolName = request.getParams().get("name").asText();

                    // Security check: must have execute permission on MCP_TOOL
                    if (authorizationService != null) {
                        boolean allowed = authorizationService.authorize(principal, "MCP_TOOL", toolName, "EXECUTE");
                        if (!allowed) {
                            logAudit(principal, "CALL_TOOL", "MCP_TOOL", toolName, "DENIED", "Unauthorized tool call or explicit DENY");
                            return McpResponse.error(id, -32001, "Forbidden: User '" + principal + "' denied execution of tool: " + toolName);
                        }
                    }

                    // Check for cancellation before calling
                    context.checkCancelledOrTimedOut();

                    Object toolResult = toolRegistry.callTool(request.getParams(), context);
                    logAudit(principal, "CALL_TOOL", "MCP_TOOL", toolName, "SUCCESS", "Executed tool " + toolName);
                    return McpResponse.success(id, toolResult);

                case "resources/list":
                    if (authorizationService != null && !authorizationService.authorize(principal, "RESOURCE", "*", "READ")) {
                        logAudit(principal, "RESOURCES_LIST", "RESOURCE", "*", "DENIED", "Unauthorized resource listing");
                        return McpResponse.error(id, -32001, "Unauthorized: User lacks permission to list resources");
                    }
                    Map<String, Object> resourcesResult = new HashMap<>();
                    resourcesResult.put("resources", resourceRegistry.listResources());
                    return McpResponse.success(id, resourcesResult);

                case "resources/read":
                    if (authorizationService != null && !authorizationService.authorize(principal, "RESOURCE", "*", "READ")) {
                        logAudit(principal, "RESOURCE_READ", "RESOURCE", "*", "DENIED", "Unauthorized resource read");
                        return McpResponse.error(id, -32001, "Unauthorized: User lacks permission to read resource");
                    }
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
                    return McpResponse.success(id, new HashMap<>());

                default:
                    return McpResponse.error(id, -32601, "Method not found: " + method);
            }
        } catch (IllegalArgumentException e) {
            String target = (request.getParams() != null && request.getParams().has("name")) ? request.getParams().get("name").asText() : method;
            logAudit(principal, "CALL_TOOL", "MCP_TOOL", target, "INVALID_ARGUMENTS", e.getMessage());
            return McpResponse.error(id, -32602, "Invalid params: " + e.getMessage());
        } catch (NoSuchElementException e) {
            String target = (request.getParams() != null && request.getParams().has("name")) ? request.getParams().get("name").asText() : method;
            logAudit(principal, "CALL_TOOL", "MCP_TOOL", target, "NOT_FOUND", e.getMessage());
            return McpResponse.error(id, -32601, e.getMessage());
        } catch (InterruptedException e) {
            String target = (request.getParams() != null && request.getParams().has("name")) ? request.getParams().get("name").asText() : method;
            logAudit(principal, "CALL_TOOL", "MCP_TOOL", target, "CANCELLED_OR_TIMEOUT", e.getMessage());
            return McpResponse.error(id, -32000, "Operation cancelled: " + e.getMessage());
        } catch (Exception e) {
            String target = (request.getParams() != null && request.getParams().has("name")) ? request.getParams().get("name").asText() : method;
            logAudit(principal, "CALL_TOOL", "MCP_TOOL", target, "ERROR", e.getMessage());
            return McpResponse.error(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    private void logAudit(String principal, String action, String resourceType, String resourceKey, String outcome, String details) {
        if (authorizationService != null) {
            authorizationService.logAudit(null, principal, action, resourceType, resourceKey, outcome, details);
        }
    }
}
