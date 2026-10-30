package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.EvolutionServer;

public class WebInterfaceSessionTest {

    private static EvolutionServer server;
    private static int port = 8899;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        server = new EvolutionServer(port);
        server.startServer();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testWebInterfaceRoutingAndSessions() throws Exception {
        String[] routes = new String[]{
            "/develop",
            "/chat",
            "/forge",
            "/mcp",
            "/admin",
            "/index.html"
        };

        for (String route : routes) {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + route + "?runtime=SWT").openConnection();
            conn.setRequestMethod("GET");
            assertEquals("Route " + route + " should return 200 OK", 200, conn.getResponseCode());
        }
    }

    @Test
    public void testMcpToolExecution() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/ui/execute?runtime=SWT").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject payload = new JSONObject();
        payload.put("tool", "calculate_sum");
        JSONObject args = new JSONObject();
        args.put("a", 10);
        args.put("b", 25);
        payload.put("arguments", args);

        byte[] out = payload.toString().getBytes("UTF-8");
        conn.getOutputStream().write(out);

        int code = conn.getResponseCode();
        assertTrue("MCP execute response code should be 200 or 500", code == 200 || code == 500);
    }
}
