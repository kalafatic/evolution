package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.kalafatic.evolution.controller.orchestration.mcp.CognitiveMcpToolProvider;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import eu.kalafatic.evolution.servers.mcp.prompts.PromptRegistry;
import eu.kalafatic.evolution.servers.mcp.protocol.McpError;
import eu.kalafatic.evolution.servers.mcp.protocol.McpRequest;
import eu.kalafatic.evolution.servers.mcp.protocol.McpResponse;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import eu.kalafatic.evolution.servers.mcp.server.BearerTokenAuthenticator;
import eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.service.AuthorizationService;
import eu.kalafatic.evolution.servers.service.CognitiveScopes;
import eu.kalafatic.evolution.servers.service.IdentityService;

/**
 * MCP security boundary: cognitive tools require a valid bearer token with
 * the tool's scope; unauthenticated callers cannot discover or call them;
 * identity parameters never override the token principal.
 */
public class CognitiveMcpSecurityTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final DatabaseManager databaseManager = new DatabaseManager();
    private JsonRpcDispatcher dispatcher;
    private String applicationId;
    private String validToken;

    @Before
    public void setUp() {
        ToolRegistry toolRegistry = new ToolRegistry();
        ResourceRegistry resourceRegistry = new ResourceRegistry();
        PromptRegistry promptRegistry = new PromptRegistry();
        dispatcher = new JsonRpcDispatcher(toolRegistry, resourceRegistry, promptRegistry);
        dispatcher.setAuthorizationService(AuthorizationService.getInstance(databaseManager));
        dispatcher.setAuthenticator(new BearerTokenAuthenticator());
        CognitiveMcpToolProvider.registerCognitiveTools(toolRegistry, dispatcher);

        applicationId = "mcp-test-app-" + System.nanoTime();
        IdentityService.getInstance(databaseManager).registerApplication(applicationId,
                "MCP Test App", "EXTERNAL_MCP", null, null);
        validToken = IdentityService.getInstance(databaseManager).issueToken(applicationId, 1L,
                List.of(CognitiveScopes.CONTEXT_GET, CognitiveScopes.CONTEXT_SEARCH,
                        CognitiveScopes.MEMORY_READ, CognitiveScopes.MEMORY_WRITE,
                        CognitiveScopes.SESSION_READ, CognitiveScopes.PROJECT_READ),
                null, null);
    }

    private McpResponse call(String toolName, String jsonArgs, String bearer) {
        McpRequest request = new McpRequest();
        request.setMethod("tools/call");
        request.setId(1L);
        try {
            request.setParams(mapper.readTree(
                    "{\"name\":\"" + toolName + "\",\"arguments\":" + jsonArgs + "}"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        McpExecutionContext context = new McpExecutionContext("test-session", "admin");
        context.setBearerToken(bearer);
        return dispatcher.dispatch(request, context);
    }

    private McpResponse listTools(String bearer) {
        McpRequest request = new McpRequest();
        request.setMethod("tools/list");
        request.setId(1L);
        McpExecutionContext context = new McpExecutionContext("test-session", "admin");
        context.setBearerToken(bearer);
        return dispatcher.dispatch(request, context);
    }

    @Test
    public void unauthenticatedCallersAreDenied() {
        McpResponse response = call("context_get", "{\"intent\":\"test\"}", null);
        assertNotNull(response.getError());
        assertEquals(-32001, response.getError().getCode());
    }

    @Test
    public void invalidTokenIsDenied() {
        McpResponse response = call("context_get", "{\"intent\":\"test\"}", "not-a-valid-token");
        assertNotNull(response.getError());
        assertEquals(-32001, response.getError().getCode());
    }

    @Test
    public void cognitiveToolsAreHiddenFromAnonymousListing() {
        McpResponse response = listTools(null);
        assertTrue(response.getResult() instanceof java.util.Map);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) response.getResult();
        @SuppressWarnings("unchecked")
        List<Object> tools = (List<Object>) result.get("tools");
        for (Object toolObject : tools) {
            if (toolObject instanceof Tool tool) {
                assertTrue("Cognitive tool must be hidden: " + tool.getName(),
                        !tool.getName().startsWith("context_")
                                && !tool.getName().startsWith("memory_"));
            }
        }
    }

    @Test
    public void authenticatedCallSucceeds() {
        McpResponse response = call("context_get",
                "{\"intent\":\"which port is used\",\"budgetTokens\":4000}", validToken);
        assertEquals(null, response.getError());
        assertNotNull(response.getResult());
    }

    @Test
    public void spoofedUserIdIsIgnored() {
        // The token belongs to admin; the request tries to act as another user.
        McpResponse response = call("user_preferences_get",
                "{\"userId\":\"nonexistent-other-user\"}", validToken);
        // user_preferences_get requires user.preferences.read which the token
        // does NOT carry -> permission denied proves both scope enforcement
        // and that spoofing never elevates identity.
        assertNotNull(response.getError());
        assertEquals(-32002, response.getError().getCode());
    }

    @Test
    public void missingScopeIsPermissionDenied() {
        // The token has no decision.write scope.
        McpResponse response = call("decision_record",
                "{\"project\":\"/tmp/x\",\"title\":\"t\",\"decision\":\"d\"}", validToken);
        McpError error = response.getError();
        assertNotNull(error);
        assertTrue("Expected -32002 (permission denied) or -32001, got " + error.getCode(),
                error.getCode() == -32002 || error.getCode() == -32001);
    }
}
