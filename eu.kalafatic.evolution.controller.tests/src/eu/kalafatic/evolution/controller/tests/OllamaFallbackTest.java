package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;

public class OllamaFallbackTest {

    private HttpServer server;
    private int port;
    private AtomicInteger generateCallCount = new AtomicInteger(0);
    private AtomicInteger tagsCallCount = new AtomicInteger(0);

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        server.createContext("/api/chat", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] bytes = exchange.getRequestBody().readAllBytes();
                String requestBody = new String(bytes);
                JSONObject reqJson = new JSONObject(requestBody);
                String requestedModel = reqJson.optString("model", "");

                generateCallCount.incrementAndGet();

                if (requestedModel.equalsIgnoreCase("evo-token-test")) {
                    // Return raw token placeholders response
                    JSONObject resp = new JSONObject();
                    JSONObject msg = new JSONObject();
                    msg.put("role", "assistant");
                    msg.put("content", "token_6291token_6231token_6627");
                    resp.put("message", msg);
                    String response = resp.toString();
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else if (requestedModel.toLowerCase().contains("evo")) {
                    // Always fail for 'evo' model with unable to load model error to trigger self-healing and then fallback
                    String response = "{\"error\":\"unable to load model: C:\\\\Users\\\\petrk\\\\.ollama\\\\models\\\\blobs\\\\sha256-f3df8489485a4c2095c1a12d2e035728c1ca6dd8374d64aea25c0e7dcb752b27\"}";
                    exchange.sendResponseHeaders(500, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else if (requestedModel.equals("large-model:latest")) {
                    // Memory error to test memory fallback
                    String response = "{\"error\":\"model requires more system memory (6.6 GiB) than is available (4.9 GiB)\"}";
                    exchange.sendResponseHeaders(500, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    // Success for small-model or other fallback models
                    JSONObject resp = new JSONObject();
                    JSONObject msg = new JSONObject();
                    msg.put("role", "assistant");
                    msg.put("content", "Fallback success");
                    resp.put("message", msg);
                    String response = resp.toString();
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            }
        });

        server.createContext("/api/create", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                JSONObject resp = new JSONObject();
                resp.put("status", "success");
                String response = resp.toString();
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });

        server.createContext("/api/tags", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                tagsCallCount.incrementAndGet();
                JSONObject resp = new JSONObject();
                JSONArray models = new JSONArray();

                JSONObject m1 = new JSONObject();
                m1.put("name", "large-model:latest");
                m1.put("size", 7L * 1024 * 1024 * 1024); // 7 GiB
                models.put(m1);

                JSONObject m2 = new JSONObject();
                m2.put("name", "small-model:latest");
                m2.put("size", 2L * 1024 * 1024 * 1024); // 2 GiB
                models.put(m2);

                resp.put("models", models);
                String response = resp.toString();
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });

        server.start();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testOllamaFallback() throws Exception {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:" + port);
        ollama.setModel("large-model:latest");
        orchestrator.setOllama(ollama);
        orchestrator.setLocalModel("large-model:latest");

        OllamaProvider provider = new OllamaProvider();
        TaskContext context = new TaskContext(orchestrator, null);

        // This should trigger the fallback logic once implemented
        String result = provider.sendRequest(orchestrator, "test prompt", 0.7f, null, context);

        assertEquals("Fallback success", result);
        assertEquals(2, generateCallCount.get());
        assertEquals(1, tagsCallCount.get());
        assertEquals("small-model:latest", orchestrator.getOllama().getModel());
        assertEquals("small-model:latest", orchestrator.getLocalModel());
    }

    @Test
    public void testEvoModelFallback() throws Exception {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:" + port);
        ollama.setModel("evo");
        orchestrator.setOllama(ollama);
        orchestrator.setLocalModel("evo");

        OllamaProvider provider = new OllamaProvider();
        TaskContext context = new TaskContext(orchestrator, null);

        // This should trigger self-healing and fallback to large-model:latest/small-model:latest
        String result = provider.sendRequest(orchestrator, "test prompt", 0.7f, null, context);

        assertEquals("Fallback success", result);
        assertNotEquals("evo", orchestrator.getOllama().getModel());
        assertNotEquals("evo", orchestrator.getLocalModel());
    }

    @Test
    public void testEvoTokenPlaceholderFallback() throws Exception {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:" + port);
        ollama.setModel("evo-token-test");
        orchestrator.setOllama(ollama);
        orchestrator.setLocalModel("evo-token-test");

        OllamaProvider provider = new OllamaProvider();
        TaskContext context = new TaskContext(orchestrator, null);

        // This model returns raw token_XXXX output. The provider must detect this and fallback to a working base model.
        String result = provider.sendRequest(orchestrator, "test prompt", 0.7f, null, context);

        assertEquals("Fallback success", result);
        assertNotEquals("evo-token-test", orchestrator.getOllama().getModel());
        assertNotEquals("evo-token-test", orchestrator.getLocalModel());
    }
}
