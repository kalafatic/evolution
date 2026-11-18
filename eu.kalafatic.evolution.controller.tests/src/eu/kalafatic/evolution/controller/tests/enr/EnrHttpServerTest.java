package eu.kalafatic.evolution.controller.tests.enr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.enr.EnrHttpServer;
import eu.kalafatic.evolution.enr.EnrInferenceEngine;

/**
 * ENR HTTP contract tests (Phase 6, WP-6.4). Wire-level tests against a fake runtime;
 * the SSE streaming path is exercised with a chunk-emitting fake engine.
 */
public class EnrHttpServerTest {

    private static class FakeEngine implements EnrInferenceEngine {

        private final List<EnrModel> models = new ArrayList<>(List.of(
                new EnrModel("ollama:demo:1", "demo:1", "ollama", "ollama", 1024, true, 4096, false),
                new EnrModel("evo-native:demo.evo", "demo.evo", "evo-native", "evo native",
                        2048, true, 0, false)));

        @Override
        public EnrHealth health() {
            return EnrHealth.up();
        }

        @Override
        public List<EnrModel> listModels() {
            return models;
        }

        @Override
        public Optional<EnrModel> findModel(String modelId) {
            return models.stream().filter(m -> m.id().equals(modelId)).findFirst();
        }

        @Override
        public boolean loadModel(String modelId) {
            return findModel(modelId).isPresent();
        }

        @Override
        public boolean unloadModel(String modelId) {
            return findModel(modelId).isPresent();
        }

        @Override
        public boolean deleteModel(String modelId) {
            return findModel(modelId).isPresent();
        }

        @Override
        public boolean pullModel(String modelId, Consumer<EnrProgress> progressConsumer) {
            return true;
        }

        @Override
        public EnrChatResponse chat(EnrChatRequest request) {
            if (request.model().equals("missing:1")) {
                throw new IllegalStateException("unknown model missing:1");
            }
            return new EnrChatResponse("fake answer for " + request.model(), "stop",
                    "ollama", request.model(), 12, 21, 42);
        }

        @Override
        public void streamChat(EnrChatRequest request, Consumer<EnrChatChunk> chunkConsumer,
                BooleanSupplier cancellationSupplier) {
            chunkConsumer.accept(new EnrChatChunk("fake answer", false, "ollama"));
            chunkConsumer.accept(EnrChatChunk.done("ollama"));
        }

        @Override
        public List<EnrProviderInfo> listProviders() {
            return List.of(new EnrProviderInfo("ollama", "Ollama", "OLLAMA",
                    "http://localhost:11434", true, true));
        }

        @Override
        public EnrRuntimeStatus runtimeStatus() {
            return new EnrRuntimeStatus(true, 4, 0.5, 1024, 256,
                    EnrGpuStatus.undetected("test"), List.of("ollama"));
        }

        @Override
        public List<EnrCapability> capabilities() {
            return List.of(new EnrCapability("ENR_INFERENCE", "Local LLM Inference", "test",
                    List.of("chat")));
        }
    }

    private EnrHttpServer server;
    private int port;

    @Before
    public void setUp() throws Exception {
        server = new EnrHttpServer(0, new FakeEngine());
        server.start(60_000, false);
        port = server.getListeningPort();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    private HttpResponse<String> send(String method, String path, String body,
            String authToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        if (authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void healthEndpoint() throws Exception {
        HttpResponse<String> response = send("GET", "/health", null, null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"application\":\"ENR\""));
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    public void modelListing() throws Exception {
        HttpResponse<String> response = send("GET", "/enr/v1/models", null, null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("ollama:demo:1"));
        assertTrue(response.body().contains("evo-native:demo.evo"));
        // Cognitive metadata extension: context window per model with honesty flag
        assertTrue("contextLength must be serialized", response.body().contains("contextLength"));
        assertTrue("contextLengthKnown must be serialized", response.body().contains("contextLengthKnown"));
    }

    @Test
    public void modelLookupAndNotFound() throws Exception {
        HttpResponse<String> found = send("GET", "/enr/v1/models/ollama:demo:1", null, null);
        assertEquals(200, found.statusCode());
        assertTrue(found.body().contains("\"contextLength\":4096"));
        assertTrue(found.body().contains("\"contextLengthKnown\":false"));
        HttpResponse<String> missing = send("GET", "/enr/v1/models/nope:1", null, null);
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("NOT_FOUND"));
    }

    @Test
    public void modelLoadUnload() throws Exception {
        HttpResponse<String> loaded = send("POST", "/enr/v1/models/ollama:demo:1/load",
                null, null);
        assertEquals(200, loaded.statusCode());
        assertTrue(loaded.body().contains("\"success\":true"));
    }

    @Test
    public void chatEndpoint() throws Exception {
        HttpResponse<String> response = send("POST", "/enr/v1/chat",
                "{\"model\":\"ollama:demo:1\",\"messages\":[{\"role\":\"user\","
                        + "\"content\":\"hi\"}]}", null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("fake answer for ollama:demo:1"));
        // Cognitive metadata extension: token usage counters on the wire
        assertTrue(response.body().contains("\"promptTokens\":21"));
        assertTrue(response.body().contains("\"completionTokens\":42"));
    }

    @Test
    public void chatValidationErrorEnvelope() throws Exception {
        HttpResponse<String> response = send("POST", "/enr/v1/chat",
                "{\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}", null);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("VALIDATION_FAILED"));
    }

    @Test
    public void chatEngineFailureIsInternalError() throws Exception {
        HttpResponse<String> response = send("POST", "/enr/v1/chat",
                "{\"model\":\"missing:1\"}", null);
        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("INTERNAL_ERROR"));
    }

    @Test
    public void runtimeStatusEndpoint() throws Exception {
        HttpResponse<String> response = send("GET", "/enr/v1/runtime", null, null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"ready\":true"));
        assertTrue(response.body().contains("\"gpu\""));
    }

    @Test
    public void providersAndCapabilitiesEndpoints() throws Exception {
        HttpResponse<String> providers = send("GET", "/enr/v1/providers", null, null);
        assertEquals(200, providers.statusCode());
        assertTrue(providers.body().contains("http://localhost:11434"));
        HttpResponse<String> capabilities = send("GET", "/enr/v1/capabilities", null, null);
        assertEquals(200, capabilities.statusCode());
        assertTrue(capabilities.body().contains("ENR_INFERENCE"));
    }

    @Test
    public void streamingChatEmitsSseChunks() throws Exception {
        HttpResponse<String> response = send("POST", "/enr/v1/chat/stream",
                "{\"model\":\"ollama:demo:1\",\"messages\":[]}", null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data:"));
        assertTrue(response.body().contains("fake answer"));
        assertTrue(response.body().contains("\"done\":true"));
    }

    @Test
    public void authTokenEnforcedWhenConfigured() throws Exception {
        System.setProperty(EnrHttpServer.SYS_ENR_AUTH_TOKEN, "test-secret");
        EnrHttpServer guarded = new EnrHttpServer(0, new FakeEngine());
        guarded.start(60_000, false);
        int guardedPort = guarded.getListeningPort();
        try {
            HttpResponse<String> denied = send("GET", "/enr/v1/models", null, null);
            HttpResponse<String> wrong = send("GET", "/enr/v1/models", null, "wrong");
            assertEquals(401, wrong.statusCode());
            assertTrue(wrong.body().contains("AUTHENTICATION_FAILED"));
            HttpResponse<String> ok = send("GET", "/enr/v1/models", null, "test-secret");
            assertEquals(200, ok.statusCode());
        } finally {
            guarded.stop();
            System.clearProperty(EnrHttpServer.SYS_ENR_AUTH_TOKEN);
        }
    }

    @Test
    public void unknownRouteIsNotFound() throws Exception {
        HttpResponse<String> response = send("GET", "/enr/v1/unknown", null, null);
        assertEquals(404, response.statusCode());
        assertFalse(response.body().contains("stacktrace"));
    }
}
