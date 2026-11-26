package eu.kalafatic.evolution.controller.tests.enr;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;
import eu.kalafatic.evolution.enr.EnrInferenceEngine;
import eu.kalafatic.evolution.enr.EnrLlmProvider;
import eu.kalafatic.evolution.enr.EnrRuntime;

/**
 * ENR model registry tests (Test C / Test D): ENR must register every Ollama model of
 * the local Ollama installation plus the native {@code *.evo} models, expose them with
 * their origins in a unified listing, and fail explicitly (never silently redirect)
 * when a visible model cannot be loaded.
 */
public class EnrModelRegistryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private HttpServer server;
    private int port;
    private final EnrRuntime runtime = EnrRuntime.instance();

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/tags", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                JSONObject resp = new JSONObject();
                JSONArray models = new JSONArray();
                JSONObject m1 = new JSONObject();
                m1.put("name", "llama3.2:3b");
                m1.put("size", 2019393189L);
                models.put(m1);
                JSONObject m2 = new JSONObject();
                m2.put("name", "qwen2.5:0.5b");
                m2.put("size", 397829310L);
                models.put(m2);
                resp.put("models", models);
                byte[] bytes = resp.toString().getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });
        server.start();
        System.setProperty(EnrRuntime.SYS_OLLAMA_URL, "http://localhost:" + port);
    }

    @After
    public void tearDown() {
        System.clearProperty(EnrRuntime.SYS_OLLAMA_URL);
        System.clearProperty("evo.test.modelDirsOverride");
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testEnrRegistersAllOllamaModels() {
        List<EnrInferenceEngine.EnrModel> models = runtime.listModels();

        assertTrue(models.stream().anyMatch(m -> m.id().equals("ollama:llama3.2:3b")
                && "llama3.2:3b".equals(m.name()) && "ollama".equals(m.provider())
                && "ollama".equals(m.engine())));
        assertTrue(models.stream().anyMatch(m -> m.id().equals("ollama:qwen2.5:0.5b")
                && "ollama".equals(m.provider())));
        // No predefined whitelist: the full registry is exposed.
        assertEquals(2, models.stream()
                .filter(m -> "ollama".equals(m.provider())
                        && (m.id().equals("ollama:llama3.2:3b") || m.id().equals("ollama:qwen2.5:0.5b")))
                .count());
    }

    @Test
    public void testEnrDiscoversNativeEvoModels() throws IOException {
        java.io.File forgeOutput = tempFolder.newFolder("forge-output");
        java.io.File evoArtifact = new java.io.File(forgeOutput, "my-evo-candidate.evo");
        assertTrue(evoArtifact.createNewFile());
        System.setProperty("evo.test.modelDirsOverride", tempFolder.getRoot().getAbsolutePath());

        List<EnrInferenceEngine.EnrModel> models = runtime.listModels();

        assertTrue(models.stream().anyMatch(m -> m.id().equals("evo-native:my-evo-candidate.evo")
                && "my-evo-candidate.evo".equals(m.name())
                && "evo-native".equals(m.provider())));
    }

    @Test
    public void testEnrUnifiedListingRetainsOriginsWithoutDuplicates() throws Exception {
        java.io.File forgeOutput = tempFolder.newFolder("forge-output");
        assertTrue(new java.io.File(forgeOutput, "my-evo-candidate.evo").createNewFile());
        System.setProperty("evo.test.modelDirsOverride", tempFolder.getRoot().getAbsolutePath());

        List<LocalModelDescriptor> models = new EnrLlmProvider(runtime).discoverModels(null);

        assertNotNull(models);
        Set<String> ids = new HashSet<>();
        for (LocalModelDescriptor m : models) {
            // Unified list, no duplicate identifiers.
            assertTrue("Duplicate model id " + m.id(), ids.add(m.id()));
        }
        assertTrue(models.stream().anyMatch(m -> "ollama:llama3.2:3b".equals(m.id())
                && LocalModelDescriptor.ORIGIN_OLLAMA.equals(m.origin())));
        assertTrue(models.stream().anyMatch(m -> "evo-native:my-evo-candidate.evo".equals(m.id())
                && LocalModelDescriptor.ORIGIN_EVO_NATIVE.equals(m.origin())));
    }

    @Test
    public void testNativeModelNotOnDiskFailsExplicitlyWithoutRedirect() {
        // A native id without a resolvable artifact must fail with a clear error —
        // inference is never silently redirected to another provider/model.
        try {
            runtime.chat(new EnrInferenceEngine.EnrChatRequest("evo-native:does-not-exist.evo",
                    List.of(EnrInferenceEngine.EnrChatMessage.user("ping")), 0.7, 16));
            fail("Expected IllegalStateException for an unresolvable native artifact");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("does-not-exist.evo"));
        }
    }
}
