package eu.kalafatic.evolution.controller.tests;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.kalafatic.evolution.view.ecosystem.EcosystemClient;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the UPM HTTP gateway using an embedded stub UPM server
 * (JDK {@code com.sun.net.httpserver} — no extra dependencies).
 */
public class EcosystemClientTest {

    private HttpServer server;
    private int port;
    private final AtomicInteger opsPosted = new AtomicInteger();
    private final AtomicInteger profileLaunch = new AtomicInteger();

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/health", ex -> {
            JSONObject h = new JSONObject();
            h.put("status", "ok");
            h.put("app", "universal-project-manager");
            h.put("name", "Universal Project Manager");
            h.put("version", "2.6.11");
            h.put("port", port);
            respond(ex, 200, h.toString());
        });
        server.createContext("/api/state", ex -> {
            JSONObject s = new JSONObject();
            s.put("app", new JSONObject().put("port", port));
            respond(ex, 200, s.toString());
        });
        server.createContext("/api/ops", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                respond(ex, 405, "{}");
                return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject parsed = new JSONObject(body);
            opsPosted.incrementAndGet();
            JSONObject out = new JSONObject();
            out.put("ok", true);
            out.put("type", parsed.optString("type"));
            respond(ex, 200, out.toString());
        });
        server.createContext("/api/profiles/dev/launch", ex -> {
            profileLaunch.incrementAndGet();
            respond(ex, 200, new JSONObject().put("ok", true).toString());
        });
        server.createContext("/api/fail", ex -> respond(ex, 500, "{\"error\":\"boom\"}"));
        server.start();
        port = server.getAddress().getPort();
    }

    @After
    public void tearDown() {
        if (server != null) server.stop(0);
    }

    private static void respond(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    @Test
    public void testDiscoveryFindsStubUpm() {
        EcosystemClient c = new EcosystemClient("http://127.0.0.1:" + port);
        assertTrue(c.discover());
        assertEquals("http://127.0.0.1:" + port, c.getBaseUrl());
    }

    @Test
    public void testDiscoveryRejectsForeignServer() throws IOException {
        // A server that is not UPM must not be accepted.
        HttpServer foreign = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        foreign.createContext("/api/health", ex -> respond(ex, 200, "{\"name\":\"nginx\"}"));
        foreign.start();
        try {
            EcosystemClient c = new EcosystemClient("http://127.0.0.1:" + foreign.getAddress().getPort());
            assertTrue(c.discover() == false || c.getBaseUrl() == null);
        } finally {
            foreign.stop(0);
        }
    }

    @Test
    public void testProductOpPostsToUpm() {
        EcosystemClient c = new EcosystemClient("http://127.0.0.1:" + port);
        assertTrue(c.discover());
        JSONObject res = c.productOp("EVO", "run", "headless");
        assertNotNull(res);
        assertTrue(res.optBoolean("ok"));
        assertEquals(1, opsPosted.get());
    }

    @Test
    public void testProfileLaunch() {
        EcosystemClient c = new EcosystemClient("http://127.0.0.1:" + port);
        assertTrue(c.discover());
        JSONObject res = c.profileLaunch("dev");
        assertNotNull(res);
        assertTrue(res.optBoolean("ok"));
        assertEquals(1, profileLaunch.get());
    }

    @Test
    public void testGetFailureIsNull() {
        EcosystemClient c = new EcosystemClient("http://127.0.0.1:" + port);
        assertTrue(c.discover());
        assertNull(c.get("/api/fail"));
        assertTrue(c.getLastError().contains("500"));
    }
}
