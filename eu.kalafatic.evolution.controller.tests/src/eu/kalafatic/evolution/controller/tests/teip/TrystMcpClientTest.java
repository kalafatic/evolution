package eu.kalafatic.evolution.controller.tests.teip;

import static org.junit.Assert.*;

import java.util.List;
import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.teip.mcp.TrystMcpClient;

/**
 * Unit test verifying TrystMcpClient behavior, URL handling, and fail-closed permission enforcement.
 */
public class TrystMcpClientTest {

    @Test
    public void testGrantedScopesCheckPass() {
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token", List.of("tryst:run_tests", "read_file"));
        assertTrue(client.getGrantedToolScopes().contains("tryst:run_tests"));
        assertTrue(client.getGrantedToolScopes().contains("read_file"));
        assertEquals("http://localhost:3000/mcp", client.getServerUrl());
    }

    @Test(expected = SecurityException.class)
    public void testUnauthorizedToolScopeRejection() throws Exception {
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token", List.of("read_file"));
        // Calling run_tests when only read_file is granted must throw SecurityException fail-closed
        client.callTool("run_tests", new JSONObject(), "exec-1", "task-1", "corr-1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyToolNameRejection() throws Exception {
        TrystMcpClient client = new TrystMcpClient("http://localhost:3000/mcp", "secret-token", List.of("*"));
        client.callTool("", new JSONObject(), "exec-1", "task-1", "corr-1");
    }
}
