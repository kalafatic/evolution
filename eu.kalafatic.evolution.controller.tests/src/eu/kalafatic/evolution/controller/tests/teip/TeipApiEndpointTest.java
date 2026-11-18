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
 * Full HTTP endpoint integration test for the TEIP 1.1 API in EvolutionServer.
 * The server is started with the explicit dev-mode opt-out (evo.teip.devMode=true)
 * which admits token-less loopback callers; a dedicated test verifies that the
 * server rejects requests when no token is configured and dev mode is off.
 */
public class TeipApiEndpointTest {

    private static EvolutionServer server;
    private static final int PORT = 28989;
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TeipExecutionManager.getInstance().clearAll();
        TeipExecutionManager.getInstance().setTeipEnabled(true);
        System.setProperty("evo.teip.devMode", "true");
        System.clearProperty("evo.teip.auth.token");
        server = new EvolutionServer(PORT);
        server.startServer();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (server != null) {
            server.stopServer();
        }
        TeipExecutionManager.getInstance().clearAll();
        System.clearProperty("evo.teip.devMode");
        System.clearProperty("evo.teip.auth.token");
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
        assertEquals("1.1", json.getString("version"));
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
        assertEquals("1.1", json.getString("version"));
        assertTrue(json.getJSONArray("supportedModes").length() > 0);
        // TEIP 1.1 §4.2: canonical capabilities[] and availableModels[] arrays
        assertTrue(json.getJSONArray("capabilities").length() > 0);
        assertTrue(json.getJSONArray("availableModels").length() > 0);
    }

    @Test
    public void testCreateExecutionAndLifecycle() throws Exception {
        JSONObject accepted = createExecution("idem-e2e-1", 202);
        String execId = accepted.getString("executionId");
        assertNotNull(execId);
        assertEquals("corr-e2e-1", accepted.getString("correlationId"));

        // Idempotent duplicate: same key returns the same execution with HTTP 200
        JSONObject duplicate = createExecution("idem-e2e-1", 200);
        assertEquals(execId, duplicate.getString("executionId"));

        // Query status endpoint
        URL statusUrl = new URL(BASE_URL + "/teip/v1/executions/" + execId);
        HttpURLConnection statusConn = (HttpURLConnection) statusUrl.openConnection();
        statusConn.setRequestMethod("GET");
        assertEquals(200, statusConn.getResponseCode());
        String statusBody = new BufferedReader(new InputStreamReader(statusConn.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject statusJson = new JSONObject(statusBody);
        assertEquals(execId, statusJson.getString("executionId"));

        // Query events endpoint: canonical nextSequence cursor
        URL eventsUrl = new URL(BASE_URL + "/teip/v1/executions/" + execId + "/events");
        HttpURLConnection eventsConn = (HttpURLConnection) eventsUrl.openConnection();
        eventsConn.setRequestMethod("GET");
        assertEquals(200, eventsConn.getResponseCode());
        String eventsBody = new BufferedReader(new InputStreamReader(eventsConn.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject eventsJson = new JSONObject(eventsBody);
        assertTrue(eventsJson.has("nextSequence"));

        // Result endpoint for a non-terminal execution: 409 CONFLICT with currentStatus
        URL resultUrl = new URL(BASE_URL + "/teip/v1/executions/" + execId + "/result");
        HttpURLConnection resultConn = (HttpURLConnection) resultUrl.openConnection();
        resultConn.setRequestMethod("GET");
        assertEquals(409, resultConn.getResponseCode());
        String errBody = new BufferedReader(new InputStreamReader(resultConn.getErrorStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject errJson = new JSONObject(errBody);
        assertEquals("CONFLICT", errJson.getString("code"));
        assertFalse(errJson.getBoolean("retryable"));
        assertNotNull(errJson.getJSONObject("details").optString("currentStatus"));

        // Cancel execution (idempotent)
        URL cancelUrl = new URL(BASE_URL + "/teip/v1/executions/" + execId + "/cancel");
        HttpURLConnection cancelConn = (HttpURLConnection) cancelUrl.openConnection();
        cancelConn.setRequestMethod("POST");
        cancelConn.setRequestProperty("Content-Type", "application/json");
        cancelConn.setDoOutput(true);
        try (OutputStream os = cancelConn.getOutputStream()) {
            os.write("{\"reason\":\"Test cancellation\"}".getBytes());
        }
        assertEquals(200, cancelConn.getResponseCode());

        // Second cancel is idempotent and still succeeds
        HttpURLConnection cancelConn2 = (HttpURLConnection) cancelUrl.openConnection();
        cancelConn2.setRequestMethod("POST");
        cancelConn2.setRequestProperty("Content-Type", "application/json");
        cancelConn2.setDoOutput(true);
        try (OutputStream os = cancelConn2.getOutputStream()) {
            os.write("{}".getBytes());
        }
        assertEquals(200, cancelConn2.getResponseCode());
    }

    @Test
    public void testDeprecatedAliasesAccepted() throws Exception {
        // aiMode / mcpToolScopes / caller.app / bare-string contextReferences must be accepted
        URL url = new URL(BASE_URL + "/teip/v1/executions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = """
        {
          "protocol": "TEIP",
          "version": "1.0",
          "aiMode": "DARWIN_MODE",
          "mcpToolScopes": ["read:workspace"],
          "caller": {"app": "TRYST", "taskId": "task-1"},
          "instruction": "Alias compatibility test",
          "contextReferences": ["src/Auth.java"]
        }
        """;
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
        }
        assertEquals(202, conn.getResponseCode());
    }

    @Test
    public void testVersionMismatchRejected() throws Exception {
        URL url = new URL(BASE_URL + "/teip/v1/executions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = """
        {
          "protocol": "TEIP",
          "version": "2.0",
          "instruction": "Unsupported version test"
        }
        """;
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
        }
        assertEquals(400, conn.getResponseCode());
        String errBody = new BufferedReader(new InputStreamReader(conn.getErrorStream()))
                .lines().collect(Collectors.joining("\n"));
        JSONObject errJson = new JSONObject(errBody);
        assertEquals("VERSION_MISMATCH", errJson.getString("code"));
    }

    @Test
    public void testAuthenticationRequiredWhenNotConfigured() throws Exception {
        // Temporarily remove the explicit dev-mode opt-out: requests must be rejected.
        System.clearProperty("evo.teip.devMode");
        try {
            URL url = new URL(BASE_URL + "/teip/v1/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            assertEquals(401, conn.getResponseCode());
            String errBody = new BufferedReader(new InputStreamReader(conn.getErrorStream()))
                    .lines().collect(Collectors.joining("\n"));
            JSONObject errJson = new JSONObject(errBody);
            assertEquals("AUTHENTICATION_FAILED", errJson.getString("code"));
        } finally {
            System.setProperty("evo.teip.devMode", "true");
        }
    }

    @Test
    public void testBearerTokenEnforcedWhenConfigured() throws Exception {
        System.setProperty("evo.teip.auth.token", "teip-test-token");
        try {
            // Without the token: rejected
            URL url = new URL(BASE_URL + "/teip/v1/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            assertEquals(401, conn.getResponseCode());

            // With the token: accepted
            HttpURLConnection okConn = (HttpURLConnection) url.openConnection();
            okConn.setRequestMethod("GET");
            okConn.setRequestProperty("Authorization", "Bearer teip-test-token");
            assertEquals(200, okConn.getResponseCode());
        } finally {
            System.clearProperty("evo.teip.auth.token");
        }
    }

    private static JSONObject createExecution(String idempotencyKey, int expectedStatus) throws Exception {
        URL url = new URL(BASE_URL + "/teip/v1/executions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = """
        {
          "protocol": "TEIP",
          "version": "1.1",
          "requestId": "req-e2e-1",
          "correlationId": "corr-e2e-1",
          "idempotencyKey": "%s",
          "capability": "CODE_IMPLEMENTATION",
          "mode": "DARWIN_MODE",
          "instruction": "Test execution instruction"
        }
        """.formatted(idempotencyKey);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
        }
        assertEquals(expectedStatus, conn.getResponseCode());
        String body = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        return new JSONObject(body);
    }
}
