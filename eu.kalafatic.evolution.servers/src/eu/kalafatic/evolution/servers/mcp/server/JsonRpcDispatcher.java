package eu.kalafatic.evolution.servers.mcp.server;

import eu.kalafatic.evolution.servers.mcp.protocol.McpRequest;
import eu.kalafatic.evolution.servers.mcp.protocol.McpResponse;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import eu.kalafatic.evolution.servers.mcp.prompts.PromptRegistry;
import eu.kalafatic.evolution.servers.mcp.model.Implementation;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import eu.kalafatic.evolution.servers.service.AuthorizationService;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonRpcDispatcher {
    private final ToolRegistry toolRegistry;
    private final ResourceRegistry resourceRegistry;
    private final PromptRegistry promptRegistry;
    private AuthorizationService authorizationService;
    private McpAuthenticator authenticator;
    private boolean enforcePromptAuthorization = false;

    /** toolName -> required cognitive scope for authenticated tool calls. */
    private final Map<String, String> toolScopeRequirements = new HashMap<>();

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

    public void setAuthenticator(McpAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    public McpAuthenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * When enabled, prompts/list and prompts/get require READ authorization on
     * the PROMPT resource type. Opt-in to preserve legacy demo behavior.
     */
    public void setEnforcePromptAuthorization(boolean enforce) {
        this.enforcePromptAuthorization = enforce;
    }

    public boolean isEnforcePromptAuthorization() {
        return enforcePromptAuthorization;
    }

    /**
     * Marks a tool as protected: calls require a valid bearer token whose
     * principal holds the given scope. Protected tools are hidden from
     * tools/list for unauthenticated callers.
     */
    public void registerToolScope(String toolName, String requiredScope) {
        toolScopeRequirements.put(toolName, requiredScope);
    }

    public Map<String, String> getToolScopeRequirements() {
        return new HashMap<>(toolScopeRequirements);
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
                    initResult.put("serverInfo", new Implementation("EvoMcpServer", "1.1.0"));
                    return McpResponse.success(id, initResult);

                case "tools/list":
                    // Authorization check for tools discovery
                    if (authorizationService != null && !authorizationService.authorize(principal, "MCP_TOOL", "*", "READ")) {
                        logAudit(principal, "TOOLS_LIST", "MCP_TOOL", "*", "DENIED", "Unauthorized tools discovery");
                        return McpResponse.error(id, -32001, "Unauthorized: User lacks permission to list MCP tools");
                    }
                    Map<String, Object> toolsResult = new HashMap<>();
                    toolsResult.put("tools", visibleTools(context));
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

                    // Cognitive scope enforcement for protected tools
                    String requiredScope = toolScopeRequirements.get(toolName);
                    if (requiredScope != null) {
                        ValidatedPrincipal vp = authenticateContext(context);
                        if (vp == null) {
                            logAudit(principal, "CALL_TOOL", "MCP_TOOL", toolName, "DENIED", "Missing or invalid bearer token for scoped tool");
                            return McpResponse.error(id, -32001, "Unauthorized: valid bearer token required for tool: " + toolName);
                        }
                        if (!vp.hasScope(requiredScope)) {
                            logAudit(principal, "CALL_TOOL", "MCP_TOOL", toolName, "DENIED", "Missing scope '" + requiredScope + "'");
                            return McpResponse.error(id, -32002, "Permission denied: missing scope '" + requiredScope + "' for tool: " + toolName);
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
                    if (enforcePromptAuthorization && authorizationService != null
                            && !authorizationService.authorize(principal, "PROMPT", "*", "READ")) {
                        logAudit(principal, "PROMPTS_LIST", "PROMPT", "*", "DENIED", "Unauthorized prompt listing");
                        return McpResponse.error(id, -32001, "Unauthorized: User lacks permission to list prompts");
                    }
                    Map<String, Object> promptsResult = new HashMap<>();
                    promptsResult.put("prompts", promptRegistry.listPrompts());
                    return McpResponse.success(id, promptsResult);

                case "prompts/get":
                    if (enforcePromptAuthorization && authorizationService != null) {
                        String promptName = request.getParams() != null && request.getParams().has("name")
                                ? request.getParams().get("name").asText() : "*";
                        if (!authorizationService.authorize(principal, "PROMPT", promptName, "READ")) {
                            logAudit(principal, "PROMPTS_GET", "PROMPT", promptName, "DENIED", "Unauthorized prompt read");
                            return McpResponse.error(id, -32001, "Unauthorized: User lacks permission to read prompt");
                        }
                    }
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
        } catch (SecurityException e) {
            String target = (request.getParams() != null && request.getParams().has("name")) ? request.getParams().get("name").asText() : method;
            logAudit(principal, "CALL_TOOL", "MCP_TOOL", target, "PERMISSION_DENIED", e.getMessage());
            return McpResponse.error(id, -32002, "Permission denied: " + e.getMessage());
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

    /**
     * Authenticates the context against the configured authenticator, caching
     * the result on the context. Returns null when no valid bearer is present.
     */
    private ValidatedPrincipal authenticateContext(McpExecutionContext context) {
        if (context == null) return null;
        if (context.getAuthenticatedPrincipal() != null) return context.getAuthenticatedPrincipal();
        if (authenticator == null) return null;
        String bearer = context.getBearerToken();
        if (bearer == null || bearer.trim().isEmpty()) return null;
        Optional<ValidatedPrincipal> principal = authenticator.authenticate(bearer.trim());
        if (principal.isPresent()) {
            context.setAuthenticatedPrincipal(principal.get());
            return principal.get();
        }
        return null;
    }

    /**
     * Filters scoped (protected) tools out of the listing for callers without
     * a valid bearer token. Legacy servers without an authenticator are
     * unaffected.
     */
    private List<Tool> visibleTools(McpExecutionContext context) {
        List<Tool> tools = new ArrayList<>(toolRegistry.listTools());
        if (authenticator == null || toolScopeRequirements.isEmpty()) {
            return tools;
        }
        ValidatedPrincipal vp = authenticateContext(context);
        if (vp != null) {
            return tools;
        }
        return tools.stream()
                .filter(t -> !toolScopeRequirements.containsKey(t.getName()))
                .collect(Collectors.toList());
    }

    private void logAudit(String principal, String action, String resourceType, String resourceKey, String outcome, String details) {
        if (authorizationService != null) {
            authorizationService.logAudit(null, principal, action, resourceType, resourceKey, outcome, details);
        }
    }
}
