package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

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

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Verifies that provider selection drives model discovery through each provider's own
 * model-listing mechanism (Test A — Ollama, Test B — llama.cpp, Test C — ENR dispatch,
 * Test F — repeated provider switching without stale or duplicate results).
 */
public class ProviderModelDiscoveryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private HttpServer server;
    private int port;

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
                m2.put("name", "gemma2:2b");
                m2.put("size", 1638435864L);
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
    }

    @After
    public void tearDown() {
        System.clearProperty("evo.test.modelDirsOverride");
        if (server != null) {
            server.stop(0);
        }
    }

    // ------------------------------------------------------------------
    // Test A — Ollama: discovery via the Ollama registry
    // ------------------------------------------------------------------

    @Test
    public void testOllamaProviderDiscoversRegistryModels() throws Exception {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:" + port);
        orchestrator.setOllama(ollama);

        List<LocalModelDescriptor> models = LlmRouter.getInstance().discoverModels("Ollama", orchestrator);

        assertNotNull(models);
        assertEquals(2, models.size());
        assertTrue(models.stream().anyMatch(m -> m.id().equals("llama3.2:3b")
                && LocalModelDescriptor.ORIGIN_OLLAMA.equals(m.origin())));
        assertTrue(models.stream().anyMatch(m -> m.id().equals("gemma2:2b")
                && "ollama".equals(m.engine()) && m.available()));
    }

    @Test
    public void testOllamaDiscoveryFailureSurfacesError() throws Exception {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://127.0.0.1:1"); // unreachable
        orchestrator.setOllama(ollama);

        try {
            LlmRouter.getInstance().discoverModels("ollama", orchestrator);
            fail("Expected discovery failure for an unreachable Ollama endpoint");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("discovery failed"));
        }
    }

    // ------------------------------------------------------------------
    // Test B — llama.cpp: discovery from the same directories the
    // llama.cpp execution path resolves
    // ------------------------------------------------------------------

    @Test
    public void testLlamaCppDiscoveryListsExecutableGgufArtifacts() throws Exception {
        java.io.File modelsDir = tempFolder.newFolder("llama-cpp-models");
        assertTrue(new java.io.File(modelsDir, "evo.gguf").createNewFile());
        assertTrue(new java.io.File(modelsDir, "evo-12345.gguf").createNewFile());
        System.setProperty("evo.test.modelDirsOverride", modelsDir.getAbsolutePath());

        List<LocalModelDescriptor> models = LlmRouter.getInstance().discoverModels("llama.cpp", null);

        assertNotNull(models);
        assertTrue(models.stream().anyMatch(m -> m.id().equals("evo")
                && LocalModelDescriptor.ORIGIN_LLAMA_CPP.equals(m.origin())));
        assertTrue(models.stream().anyMatch(m -> m.id().equals("evo-12345")
                && "llama-cpp".equals(m.engine())));
        // Discovery and execution must agree: every listed id must resolve to a file.
        for (LocalModelDescriptor m : models) {
            assertNotNull("No artifact resolvable for discovered id " + m.id(),
                    LlamaService.resolveEvoModelPath(m.id()));
        }
    }

    @Test
    public void testLlamaCppEmptyRegistryYieldsEmptyList() throws Exception {
        java.io.File emptyDir = tempFolder.newFolder("llama-cpp-empty");
        System.setProperty("evo.test.modelDirsOverride", emptyDir.getAbsolutePath());

        List<LocalModelDescriptor> models = LlmRouter.getInstance().discoverModels("llama.cpp", null);

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    // ------------------------------------------------------------------
    // Test C — ENR: discovery dispatches to the registered ENR provider
    // ------------------------------------------------------------------

    private static class FakeEnrProvider implements ILlmProvider {
        final List<LocalModelDescriptor> models;
        int discoveryCalls = 0;

        FakeEnrProvider(List<LocalModelDescriptor> models) {
            this.models = models;
        }

        @Override
        public List<LocalModelDescriptor> discoverModels(Orchestrator orchestrator) {
            discoveryCalls++;
            return models;
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
                String proxyUrl, TaskContext context) {
            return "enr";
        }
    }

    @Test
    public void testEnrSelectionDispatchesToEnrProviderDiscovery() throws Exception {
        LlmRouter router = new LlmRouter();
        FakeEnrProvider enr = new FakeEnrProvider(List.of(
                new LocalModelDescriptor("ollama:llama3.2:3b", "llama3.2:3b",
                        LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true),
                new LocalModelDescriptor("evo-native:my-model.evo", "my-model.evo",
                        LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", true)));
        router.setEnrProvider(enr);

        List<LocalModelDescriptor> models = router.discoverModels("EVO Native ENR", null);

        assertEquals(2, models.size());
        assertEquals(1, enr.discoveryCalls);
        assertTrue(models.stream().anyMatch(m -> "ollama:llama3.2:3b".equals(m.id())));
        assertTrue(models.stream().anyMatch(m -> "evo-native:my-model.evo".equals(m.id())));
    }

    @Test
    public void testEnrDiscoveryWithoutRegisteredProviderFailsExplicitly() throws Exception {
        LlmRouter router = new LlmRouter(); // ENR provider NOT registered

        try {
            router.discoverModels("EVO Native ENR", null);
            fail("Expected IllegalStateException when ENR is not registered");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("ENR"));
        }
    }

    @Test
    public void testUnknownProviderSelectionFailsExplicitly() throws Exception {
        try {
            LlmRouter.getInstance().discoverModels("Mystery Provider", null);
            fail("Expected IllegalArgumentException for an unsupported provider");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Mystery Provider"));
        }
    }

    // ------------------------------------------------------------------
    // Test F — repeated provider switching: no stale results, no duplicates
    // ------------------------------------------------------------------

    @Test
    public void testRepeatedProviderSwitchingYieldsFreshDistinctLists() throws Exception {
        LlmRouter router = new LlmRouter();
        FakeEnrProvider enr = new FakeEnrProvider(List.of(
                new LocalModelDescriptor("ollama:a", "a", LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true),
                new LocalModelDescriptor("evo-native:b.evo", "b.evo",
                        LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", true)));
        router.setEnrProvider(enr);

        java.io.File modelsDir = tempFolder.newFolder("switch-models");
        assertTrue(new java.io.File(modelsDir, "evo.gguf").createNewFile());
        System.setProperty("evo.test.modelDirsOverride", modelsDir.getAbsolutePath());

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:" + port);
        orchestrator.setOllama(ollama);

        String[] sequence = { "Ollama", "EVO Native ENR", "llama.cpp", "EVO Native ENR", "Ollama" };
        java.util.Set<String> ollamaIds = null;
        java.util.Set<String> enrIds = null;
        java.util.Set<String> llamaIds = null;

        for (String provider : sequence) {
            List<LocalModelDescriptor> models = router.discoverModels(provider, orchestrator);
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (LocalModelDescriptor m : models) {
                ids.add(m.id());
            }
            // No duplicate identifiers within a single discovery result.
            assertEquals("Duplicate model ids for provider " + provider, models.size(), ids.size());

            switch (LlmRouter.normalizeProviderKey(provider)) {
                case "ollama" -> {
                    if (ollamaIds == null) ollamaIds = ids;
                    else assertEquals(ollamaIds, ids);
                    assertEquals(2, ids.size()); // llama3.2:3b + gemma2:2b
                }
                case "enr" -> {
                    if (enrIds == null) enrIds = ids;
                    else assertEquals(enrIds, ids);
                    assertEquals(2, ids.size());
                }
                default -> {
                    if (llamaIds == null) llamaIds = ids;
                    else assertEquals(llamaIds, ids);
                    assertEquals(1, ids.size()); // evo
                }
            }
        }
        // Each provider returns its own list — a native ENR-only model never leaks
        // into the Ollama list and vice versa.
        assertFalse(ollamaIds.contains("evo-native:b.evo"));
        assertTrue(enrIds.contains("evo-native:b.evo"));
        assertTrue(enrIds.contains("ollama:a"));
        assertFalse(llamaIds.contains("llama3.2:3b"));
    }

    @Test
    public void testEnrDiscoveryReturnsCompleteMixedListWithoutFiltering() throws Exception {
        // The ENR registry is authoritative: every registered model — Ollama-compatible,
        // EVO-native and llama.cpp, loaded or not — must pass through discovery unchanged.
        LlmRouter router = new LlmRouter();
        List<LocalModelDescriptor> enrModels = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            enrModels.add(new LocalModelDescriptor("ollama:model-" + i, "model-" + i,
                    LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true));
        }
        enrModels.add(new LocalModelDescriptor("evo-native:shared.evo", "shared.evo",
                LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", false));
        enrModels.add(new LocalModelDescriptor("llama-cpp:shared.gguf", "shared.gguf",
                LocalModelDescriptor.ORIGIN_LLAMA_CPP, "llama-cpp", false));
        enrModels.add(new LocalModelDescriptor("evo-native:model-1", "model-1",
                LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", true));
        enrModels.add(new LocalModelDescriptor("ollama:unloaded", "unloaded",
                LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", false));
        FakeEnrProvider enr = new FakeEnrProvider(enrModels);
        router.setEnrProvider(enr);

        List<LocalModelDescriptor> models = router.discoverModels("EVO Native ENR", null);

        assertEquals(10, models.size());
        // Exact identity and order preserved — no filtering, no format/name exclusions.
        assertEquals(enrModels, models);
        // Duplicate display name "model-1" survives as two distinct identities.
        assertTrue(models.stream().anyMatch(m -> "ollama:model-1".equals(m.id())));
        assertTrue(models.stream().anyMatch(m -> "evo-native:model-1".equals(m.id())));
        // Unloaded/unavailable models remain visible.
        assertTrue(models.stream().anyMatch(m -> "ollama:unloaded".equals(m.id()) && !m.available()));
    }

    @Test
    public void testOllamaDiscoveryListsEveryRegistryModelWithoutNameFiltering() throws Exception {
        // The Ollama registry is authoritative: every returned name — including names
        // that resemble native/GGUF artifacts — must be listed, never filtered.
        HttpServer srv = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            srv.createContext("/api/tags", exchange -> {
                JSONObject resp = new JSONObject();
                JSONArray models = new JSONArray();
                for (String name : List.of("llama3.2:3b", "evo", "my-model.gguf", "forge-123", "gemma2:2b")) {
                    JSONObject m = new JSONObject();
                    m.put("name", name);
                    m.put("size", 1000L);
                    models.put(m);
                }
                resp.put("models", models);
                byte[] bytes = resp.toString().getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            srv.start();

            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
            ollama.setUrl("http://localhost:" + srv.getAddress().getPort());
            orchestrator.setOllama(ollama);

            List<LocalModelDescriptor> models = LlmRouter.getInstance().discoverModels("Ollama", orchestrator);

            assertEquals(5, models.size());
            for (String name : List.of("llama3.2:3b", "evo", "my-model.gguf", "forge-123", "gemma2:2b")) {
                assertTrue("Expected registry model " + name + " to be visible",
                        models.stream().anyMatch(m -> m.id().equals(name)));
            }
        } finally {
            srv.stop(0);
        }
    }

    // ------------------------------------------------------------------
    // Routing agreement: explicit llama.cpp selection is not redirected
    // ------------------------------------------------------------------

    private static class RecordingOllama implements ILlmProvider {
        int calls = 0;

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
                String proxyUrl, TaskContext context) {
            calls++;
            return "ollama-path";
        }
    }

    private static class RecordingNative implements ILlmProvider {
        int calls = 0;

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
                String proxyUrl, TaskContext context) {
            calls++;
            return "native-path";
        }
    }

    @Test
    public void testExplicitLlamaCppSelectionKeepsModelNameBasedNativeDetectionDisabled() throws Exception {
        RecordingOllama ollama = new RecordingOllama();
        RecordingNative nativeEvo = new RecordingNative();
        LlmRouter router = new LlmRouter();
        router.setLocalProvider(ollama);
        router.setNativeEvoProvider(nativeEvo);

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setLocalModel("evo"); // name that would normally route to native
        TaskContext context = new TaskContext(orchestrator,
                new java.io.File(System.getProperty("user.dir")));
        context.getMetadata().put("inferenceEngine", "llama.cpp");

        String out = router.sendLocalRequest(orchestrator, "hello", 0.7f, null, context);

        assertEquals("ollama-path", out);
        assertEquals(1, ollama.calls);
        assertEquals(0, nativeEvo.calls);
    }
}
