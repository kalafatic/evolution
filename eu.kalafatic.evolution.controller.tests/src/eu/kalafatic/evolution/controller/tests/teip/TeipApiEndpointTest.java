package eu.kalafatic.evolution.controller.tests.teip;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.EvolutionServer;
import eu.kalafatic.evolution.controller.teip.service.TeipExecutionManager;

/**
 * Full HTTP endpoint integration test for TEIP 1.0 API in EvolutionServer.
 */
public class TeipApiEndpointTest {

    private static EvolutionServer server;
    private static final int PORT = 28989;
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TeipExecutionManager.getInstance().clearAll();
        TeipExecutionManager.getInstance().setTeipEnabled(true);
        server = new EvolutionServer(PORT);
        server.startServer();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (server != null) {
            server.stopServer();
        }
        TeipExecutionManager.getInstance().clearAll();
    }

    @Test
    public void testHealthEndpoint() throws Exception {
        URL url = new URL(BASE_URL + "/teip/v1/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String body = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject json = new JSONObject(body);
        assertEquals("TEIP", json.getString("protocol"));
        assertEquals("1.0", json.getString("version"));
        assertEquals("UP", json.getString("status"));
        assertTrue(json.getBoolean("ready"));
    }

    @Test
    public void testCapabilitiesEndpoint() throws Exception {
        URL url = new URL(BASE_URL + "/teip/v1/capabilities");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String body = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject json = new JSONObject(body);
        assertEquals("TEIP", json.getString("protocol"));
        assertTrue(json.getJSONArray("supportedModes").length() > 0);
    }

    @Test
    public void testCreateExecutionAndLifecycle() throws Exception {
        URL url = new URL(BASE_URL + "/teip/v1/executions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = """
        {
          "protocol": "TEIP",
          "version": "1.0",
          "requestId": "req-e2e-1",
          "correlationId": "corr-e2e-1",
          "idempotencyKey": "idem-e2e-1",
          "capability": "CODE_IMPLEMENTATION",
          "mode": "DARWIN_MODE",
          "instruction": "Test execution instruction"
        }
        """;

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
        }

        assertEquals(202, conn.getResponseCode());
        String body = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject json = new JSONObject(body);
        String execId = json.getString("executionId");
        assertNotNull(execId);
        assertEquals("corr-e2e-1", json.getString("correlationId"));

        // Query status endpoint
        URL statusUrl = new URL(BASE_URL + "/teip/v1/executions/" + execId);
        HttpURLConnection statusConn = (HttpURLConnection) statusUrl.openConnection();
        statusConn.setRequestMethod("GET");
        assertEquals(200, statusConn.getResponseCode());
        String statusBody = new BufferedReader(new InputStreamReader(statusConn.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject statusJson = new JSONObject(statusBody);
        assertEquals(execId, statusJson.getString("executionId"));

        // Query events endpoint
        URL eventsUrl = new URL(BASE_URL + "/teip/v1/executions/" + execId + "/events");
        HttpURLConnection eventsConn = (HttpURLConnection) eventsUrl.openConnection();
        eventsConn.setRequestMethod("GET");
        assertEquals(200, eventsConn.getResponseCode());

        // Cancel execution
        URL cancelUrl = new URL(BASE_URL + "/teip/v1/executions/" + execId + "/cancel");
        HttpURLConnection cancelConn = (HttpURLConnection) cancelUrl.openConnection();
        cancelConn.setRequestMethod("POST");
        cancelConn.setRequestProperty("Content-Type", "application/json");
        cancelConn.setDoOutput(true);
        try (OutputStream os = cancelConn.getOutputStream()) {
            os.write("{\"reason\":\"Test cancellation\"}".getBytes());
        }
        assertEquals(200, cancelConn.getResponseCode());
    }
}
