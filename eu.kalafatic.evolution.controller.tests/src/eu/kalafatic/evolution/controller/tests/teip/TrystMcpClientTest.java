package eu.kalafatic.evolution.controller.tests.teip;

import static org.junit.Assert.*;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.teip.mcp.TrystMcpClient;

/**
 * Unit test verifying TrystMcpClient behavior, header transmission (TEIP 1.1 §8.2),
 * MCP initialize handshake, and fail-closed permission enforcement.
 */
public class TrystMcpClientTest {

    private HttpServer mockServer;
    private int mockPort;

    @Before
    public void setUp() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();
    }

    @After
    public void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    public void testGrantedScopesCheckPass() {
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token", List.of("tryst:run_tests", "read_file"));
        assertTrue(client.getGrantedToolScopes().contains("tryst:run_tests"));
        assertTrue(client.getGrantedToolScopes().contains("read_file"));
        assertEquals("http://localhost:3000/mcp", client.getServerUrl());
    }

    @Test
    public void testDefaultServerUrl() {
        TrystMcpClient client = new TrystMcpClient(null);
        assertTrue(client.getServerUrl().contains("api/mcp"));
        assertTrue(client.getServerUrl().contains("38080"));
    }

    @Test(expected = SecurityException.class)
    public void testUnauthorizedToolScopeRejection() throws Exception {
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token", List.of("read_file"));
        // Calling run_tests when only read_file is granted must throw SecurityException fail-closed
        client.callTool("run_tests", new JSONObject(), "exec-1", "task-1", "corr-1");
    }

    @Test(expected = SecurityException.class)
    public void testScopeBasedToolRejection() throws Exception {
        // Canonical scope mapping (TEIP 1.1 §8.5): run_build_and_test requires execute:build.
        // With only read scopes granted, the call must fail closed.
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token",
                List.of("read:workspace", "read:task"));
        client.callTool("run_build_and_test", new JSONObject(), "exec-1", "task-1", "corr-1");
    }

    @Test
    public void testScopeBasedToolAuthorized() {
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token",
                List.of("read:workspace", "read:task", "write:workspace", "execute:build"));
        assertTrue(client.getGrantedToolScopes().contains("execute:build"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyToolNameRejection() throws Exception {
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token", List.of("*"));
        client.callTool("", new JSONObject(), "exec-1", "task-1", "corr-1");
    }

    @Test
    public void testRequiredHeadersAndInitialize() throws Exception {
        final String[] captured = new String[3];
        mockServer.createContext("/", exchange -> {
            captured[0] = exchange.getRequestHeaders().getFirst("Authorization");
            captured[1] = exchange.getRequestHeaders().getFirst("X-Execution-Id");
            captured[2] = exchange.getRequestHeaders().getFirst("X-Correlation-Id");
            String resp = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"2024-11-05\"}}";
            byte[] bytes = resp.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        TrystMcpClient client = new TrystMcpClient("http://127.0.0.1:" + mockPort + "/", "mcp-token", List.of("*"));
        JSONObject result = client.initialize("exec-42", "corr-42");

        assertEquals("2024-11-05", result.getString("protocolVersion"));
        assertEquals("Bearer mcp-token", captured[0]);
        assertEquals("exec-42", captured[1]);
        assertEquals("corr-42", captured[2]);
    }
}
