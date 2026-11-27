package eu.kalafatic.evolution.controller.tests.enr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.enr.EnrWebServer;

/**
 * ENR Web Control Center contract tests: the independent listener serves the static UI
 * and its own info/health endpoints, never the API surface, and reports port conflicts
 * clearly instead of falling back to another port.
 */
public class EnrWebServerTest {

    private EnrWebServer server;
    private int port;

    @Before
    public void setUp() throws Exception {
        // Ephemeral port: the test never occupies the authoritative 28880 port.
        server = new EnrWebServer(0, "127.0.0.1", 28780);
        server.start(60_000, false);
        port = server.getListeningPort();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + path))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void servesIndexHtml() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(response.body().contains("Neural Runtime"));
    }

    @Test
    public void servesStaticAssets() throws Exception {
        HttpResponse<String> js = get("/app.js");
        assertEquals(200, js.statusCode());
        assertTrue(js.headers().firstValue("Content-Type").orElse("").contains("javascript"));
        assertTrue(js.body().contains("API_BASE"));

        HttpResponse<String> css = get("/style.css");
        assertEquals(200, css.statusCode());
        assertTrue(css.headers().firstValue("Content-Type").orElse("").contains("text/css"));
    }

    @Test
    public void wccInfoReportsApiBaseUrl() throws Exception {
        HttpResponse<String> response = get("/api/wcc/info");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"api_base_url\":\"http://127.0.0.1:28780\""));
    }

    @Test
    public void webHealthIsIndependent() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"application\":\"ENR Web\""));
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    public void webDoesNotServeApiSurface() throws Exception {
        HttpResponse<String> response = get("/enr/v1/models");
        assertEquals(404, response.statusCode());
    }

    @Test
    public void conflictReportIdentifiesOccupiedPort() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0)) {
            occupied.setReuseAddress(true);
            int busyPort = occupied.getLocalPort();
            String conflict = EnrWebServer.conflictOnPort(busyPort);
            assertNotNull("conflict must be detected on an occupied port", conflict);
            assertTrue(conflict.contains("PORT CONFLICT"));
            assertTrue(conflict.contains(String.valueOf(busyPort)));
        }
    }
}
