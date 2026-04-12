package eu.kalafatic.evolution.controller.tests;

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

        server.createContext("/api/generate", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                int count = generateCallCount.incrementAndGet();
                if (count == 1) {
                    // Return memory error
                    String response = "{\"error\":\"model requires more system memory (6.6 GiB) than is available (4.9 GiB)\"}";
                    exchange.sendResponseHeaders(500, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    // Return success
                    String response = "{\"response\":\"Fallback success\"}";
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
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
}
