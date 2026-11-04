package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.kalafatic.evolution.controller.orchestration.mcp.EvoMcpToolProvider;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import eu.kalafatic.evolution.servers.mcp.protocol.McpRequest;
import eu.kalafatic.evolution.servers.mcp.protocol.McpResponse;
import eu.kalafatic.evolution.servers.mcp.prompts.DemoPrompts;
import eu.kalafatic.evolution.servers.mcp.prompts.PromptRegistry;
import eu.kalafatic.evolution.servers.mcp.resources.DemoResources;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher;
import eu.kalafatic.evolution.servers.mcp.tools.DemoTools;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.model.AuditLogEntry;
import eu.kalafatic.evolution.servers.model.ResourceAccessPolicy;
import eu.kalafatic.evolution.servers.model.User;
import eu.kalafatic.evolution.servers.service.AuthorizationService;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Regression and security verification tests for MCP Platform:
 * - Schema validation & parameters
 * - Security authorization & Explicit DENY
 * - Session isolation and context propagation
 * - Cancellation and timeouts
 * - Audit log integrity
 * - Authoritative EVO capability access
 */
public class McpPlatformSecurityAndExecutionTest {

    private DatabaseManager dbManager;
    private AuthorizationService authService;
    private ToolRegistry toolRegistry;
    private ResourceRegistry resourceRegistry;
    private PromptRegistry promptRegistry;
    private JsonRpcDispatcher dispatcher;
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        dbManager = new DatabaseManager();
        authService = new AuthorizationService(dbManager);

        toolRegistry = new ToolRegistry();
        DemoTools.registerAll(toolRegistry);
        EvoMcpToolProvider.registerAuthoritativeTools(toolRegistry);

        resourceRegistry = new ResourceRegistry();
        DemoResources.registerAll(resourceRegistry);

        promptRegistry = new PromptRegistry();
        DemoPrompts.registerAll(promptRegistry);

        dispatcher = new JsonRpcDispatcher(toolRegistry, resourceRegistry, promptRegistry);
        dispatcher.setAuthorizationService(authService);

        mapper = new ObjectMapper();
    }

    @Test
    public void testToolDiscoveryAndMetadataSeparation() {
        McpRequest req = new McpRequest();
        req.setMethod("tools/list");
        req.setId(1);

        McpExecutionContext adminCtx = new McpExecutionContext("session-1", "admin");
        McpResponse resp = dispatcher.dispatch(req, adminCtx);

        assertNull("Response should not have errors", resp.getError());
        assertNotNull("Result must not be null", resp.getResult());

        @SuppressWarnings("unchecked")
        List<Tool> tools = (List<Tool>) ((Map<String, Object>) resp.getResult()).get("tools");
        assertTrue("Should have registered demo and EVO tools", tools.size() >= 10);

        boolean foundFileRead = false;
        boolean foundProcessList = false;
        for (Tool t : tools) {
            if ("evo_file_read".equals(t.getName())) {
                foundFileRead = true;
                assertNotNull("Schema must be declared", t.getInputSchema());
                assertTrue("Schema must require path", ((List<?>) t.getInputSchema().get("required")).contains("path"));
            }
            if ("evo_process_list".equals(t.getName())) {
                foundProcessList = true;
            }
        }
        assertTrue("Must discover evo_file_read", foundFileRead);
        assertTrue("Must discover evo_process_list", foundProcessList);
    }

    @Test
    public void testSchemaValidationMissingRequiredParameter() {
        McpRequest req = new McpRequest();
        req.setMethod("tools/call");
        req.setId(2);

        ObjectNode params = mapper.createObjectNode();
        params.put("name", "calculator");
        ObjectNode args = mapper.createObjectNode();
        args.put("operation", "add");
        // Missing required 'a' and 'b'
        params.set("arguments", args);
        req.setParams(params);

        McpExecutionContext ctx = new McpExecutionContext("session-1", "admin");
        McpResponse resp = dispatcher.dispatch(req, ctx);

        assertNotNull("Must return an error on missing required params", resp.getError());
        assertEquals(-32602, resp.getError().getCode());
        assertTrue(resp.getError().getMessage().contains("Missing required parameter"));
    }

    @Test
    public void testSchemaValidationInvalidTypeAndEnum() {
        McpRequest req = new McpRequest();
        req.setMethod("tools/call");
        req.setId(3);

        ObjectNode params = mapper.createObjectNode();
        params.put("name", "calculator");
        ObjectNode args = mapper.createObjectNode();
        args.put("operation", "invalid_op"); // Not in enum
        args.put("a", 10);
        args.put("b", 5);
        params.set("arguments", args);
        req.setParams(params);

        McpExecutionContext ctx = new McpExecutionContext("session-1", "admin");
        McpResponse resp = dispatcher.dispatch(req, ctx);

        assertNotNull("Must reject invalid enum value", resp.getError());
        assertEquals(-32602, resp.getError().getCode());
        assertTrue(resp.getError().getMessage().contains("not in allowed enum options"));
    }

    @Test
    public void testSecurityAuthorizationExplicitDeny() throws Exception {
        // Create standard user "developer1"
        User devUser = new User(null, "developer1", "Dev User", "hash", "USER", true, LocalDateTime.now(), LocalDateTime.now(), null, "Test developer");
        authService.getUserRepository().save(devUser);
        User savedUser = authService.getUserRepository().findByUsername("developer1").orElseThrow();

        // Register resource "calculator"
        authService.registerResource("MCP_TOOL", "calculator", "Calculator", "Test tool", true, "{}");
        var resItem = authService.getResourceRepository().findByTypeAndKey("MCP_TOOL", "calculator").orElseThrow();

        // Grant explicit ALLOW to user
        ResourceAccessPolicy allowPolicy = new ResourceAccessPolicy(null, "USER", savedUser.getId(), resItem.getId(), "EXECUTE", "ALLOW", LocalDateTime.now(), 1L);
        authService.getResourceAccessRepository().save(allowPolicy);

        // Verify developer1 is allowed
        assertTrue("developer1 should be allowed with explicit ALLOW",
                authService.authorize("developer1", "MCP_TOOL", "calculator", "EXECUTE"));

        // Now add explicit DENY policy
        ResourceAccessPolicy denyPolicy = new ResourceAccessPolicy(null, "USER", savedUser.getId(), resItem.getId(), "EXECUTE", "DENY", LocalDateTime.now(), 1L);
        authService.getResourceAccessRepository().save(denyPolicy);

        // Verify DENY overrides ALLOW
        assertFalse("Explicit DENY must override ALLOW",
                authService.authorize("developer1", "MCP_TOOL", "calculator", "EXECUTE"));

        // Dispatch call as developer1 -> should fail with forbidden error
        McpRequest req = new McpRequest();
        req.setMethod("tools/call");
        req.setId(4);
        ObjectNode params = mapper.createObjectNode();
        params.put("name", "calculator");
        ObjectNode args = mapper.createObjectNode();
        args.put("operation", "add");
        args.put("a", 1);
        args.put("b", 2);
        params.set("arguments", args);
        req.setParams(params);

        McpExecutionContext devCtx = new McpExecutionContext("session-dev", "developer1");
        McpResponse resp = dispatcher.dispatch(req, devCtx);

        assertNotNull("Call must be forbidden", resp.getError());
        assertEquals(-32001, resp.getError().getCode());
        assertTrue(resp.getError().getMessage().contains("denied execution"));

        // Verify audit log has DENIED entry
        List<AuditLogEntry> auditLogs = authService.getAuditLogRepository().findAll(100);
        boolean foundDenied = auditLogs.stream()
                .anyMatch(a -> "developer1".equals(a.getActorUsername()) && "DENIED".equals(a.getOutcome()));
        assertTrue("Audit log must capture DENIED outcome", foundDenied);
    }

    @Test
    public void testCancellationPropagation() {
        McpRequest req = new McpRequest();
        req.setMethod("tools/call");
        req.setId(5);
        ObjectNode params = mapper.createObjectNode();
        params.put("name", "echo");
        ObjectNode args = mapper.createObjectNode();
        args.put("text", "hello");
        params.set("arguments", args);
        req.setParams(params);

        McpExecutionContext ctx = new McpExecutionContext("session-cxl", "admin");
        ctx.cancel(); // Flag as cancelled

        McpResponse resp = dispatcher.dispatch(req, ctx);
        assertNotNull("Cancelled operation must return error", resp.getError());
        assertEquals(-32000, resp.getError().getCode());
        assertTrue(resp.getError().getMessage().contains("cancelled"));
    }

    @Test
    public void testAuthoritativeCapabilityFileReadAndSecurityBoundary() {
        McpRequest req = new McpRequest();
        req.setMethod("tools/call");
        req.setId(6);

        // Try reading pom.xml (valid)
        ObjectNode params = mapper.createObjectNode();
        params.put("name", "evo_file_read");
        ObjectNode args = mapper.createObjectNode();
        args.put("path", "pom.xml");
        params.set("arguments", args);
        req.setParams(params);

        McpExecutionContext ctx = new McpExecutionContext("session-file", "admin");
        McpResponse resp = dispatcher.dispatch(req, ctx);
        assertNull("Should read pom.xml without error", resp.getError());
        assertNotNull(resp.getResult());

        // Path traversal attack: attempt to read outside project root
        args.put("path", "../../etc/passwd");
        McpResponse traversalResp = dispatcher.dispatch(req, ctx);
        assertNotNull("Path traversal must be rejected", traversalResp.getError());
        assertTrue(traversalResp.getError().getMessage().contains("Path traversal detected"));
    }

    @Test
    public void testAuditLogIntegrityForSuccessfulInvocation() throws Exception {
        McpRequest req = new McpRequest();
        req.setMethod("tools/call");
        req.setId(7);
        ObjectNode params = mapper.createObjectNode();
        params.put("name", "echo");
        ObjectNode args = mapper.createObjectNode();
        args.put("text", "audit_test");
        params.set("arguments", args);
        req.setParams(params);

        McpExecutionContext ctx = new McpExecutionContext("session-audit", "admin");
        McpResponse resp = dispatcher.dispatch(req, ctx);
        assertNull(resp.getError());

        List<AuditLogEntry> auditLogs = authService.getAuditLogRepository().findAll(100);
        boolean foundSuccess = auditLogs.stream()
                .anyMatch(a -> "admin".equals(a.getActorUsername()) && "CALL_TOOL".equals(a.getAction()) && "SUCCESS".equals(a.getOutcome()));
        assertTrue("Audit log must contain SUCCESS record for tool call", foundSuccess);
    }
}
