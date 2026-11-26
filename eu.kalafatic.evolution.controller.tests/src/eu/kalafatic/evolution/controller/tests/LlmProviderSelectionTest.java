package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Verifies that the local LLM provider is configurable (not hardcoded) and that
 * Ollama, ENR and native EVO are independent, selectable providers. The default
 * ({@code auto}) preserves the established inference path; Ollama remains the
 * fallback, and ENR becomes the default only via explicit configuration.
 */
public class LlmProviderSelectionTest {

    @After
    public void tearDown() {
        System.clearProperty(LlmRouter.SYS_DEFAULT_LOCAL_PROVIDER);
    }

    private static class RecordingProvider implements ILlmProvider {
        final String name;
        int calls = 0;

        RecordingProvider(String name) {
            this.name = name;
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
                String proxyUrl, TaskContext context) {
            calls++;
            return name;
        }
    }

    private static LlmRouter newRouter(RecordingProvider ollama, RecordingProvider nativeEvo,
            RecordingProvider enr) {
        LlmRouter router = new LlmRouter();
        router.setLocalProvider(ollama);
        router.setNativeEvoProvider(nativeEvo);
        router.setEnrProvider(enr);
        return router;
    }

    @Test
    public void testAutoDefaultRoutesNonNativeModelToOllama() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        RecordingProvider enr = new RecordingProvider("enr");
        LlmRouter router = newRouter(ollama, nativeEvo, enr);

        String out = router.sendLocalRequest(null, "hello", 0.7f, null, null);

        assertEquals("ollama", out);
        assertEquals(1, ollama.calls);
        assertEquals(0, nativeEvo.calls);
        assertEquals(0, enr.calls);
    }

    @Test
    public void testOllamaExplicitlySelected() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        RecordingProvider enr = new RecordingProvider("enr");
        LlmRouter router = newRouter(ollama, nativeEvo, enr);
        System.setProperty(LlmRouter.SYS_DEFAULT_LOCAL_PROVIDER, "ollama");

        String out = router.sendLocalRequest(null, "hello", 0.7f, null, null);

        assertEquals("ollama", out);
        assertEquals(1, ollama.calls);
        assertEquals(0, nativeEvo.calls);
        assertEquals(0, enr.calls);
    }

    @Test
    public void testEnrExplicitlySelected() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        RecordingProvider enr = new RecordingProvider("enr");
        LlmRouter router = newRouter(ollama, nativeEvo, enr);
        System.setProperty(LlmRouter.SYS_DEFAULT_LOCAL_PROVIDER, "enr");

        String out = router.sendLocalRequest(null, "hello", 0.7f, null, null);

        assertEquals("enr", out);
        assertEquals(1, enr.calls);
        assertEquals(0, ollama.calls);
        assertEquals(0, nativeEvo.calls);
    }

    @Test
    public void testNativeExplicitlySelected() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        RecordingProvider enr = new RecordingProvider("enr");
        LlmRouter router = newRouter(ollama, nativeEvo, enr);
        System.setProperty(LlmRouter.SYS_DEFAULT_LOCAL_PROVIDER, "native");

        String out = router.sendLocalRequest(null, "hello", 0.7f, null, null);

        assertEquals("native", out);
        assertEquals(1, nativeEvo.calls);
        assertEquals(0, ollama.calls);
        assertEquals(0, enr.calls);
    }

    @Test
    public void testEnrSelectedButNotRegisteredFailsExplicitly() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        // ENR provider intentionally NOT registered.
        LlmRouter router = new LlmRouter();
        router.setLocalProvider(ollama);
        router.setNativeEvoProvider(nativeEvo);
        System.setProperty(LlmRouter.SYS_DEFAULT_LOCAL_PROVIDER, "enr");

        try {
            router.sendLocalRequest(null, "hello", 0.7f, null, null);
            fail("Expected IllegalStateException when ENR selected but not registered");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("ENR"));
        }
    }

    @Test
    public void testEnrSelectedViaContextMetadata() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        RecordingProvider enr = new RecordingProvider("enr");
        LlmRouter router = newRouter(ollama, nativeEvo, enr);

        // The AI Commander provider selection is propagated as session metadata
        // ("provider=enr") and must route the request through the ENR provider.
        TaskContext context = new TaskContext(null, new java.io.File(System.getProperty("user.dir")));
        context.getMetadata().put("provider", "enr");
        context.getMetadata().put("inferenceEngine", "ENR");

        String out = router.sendLocalRequest(null, "hello", 0.7f, null, context);

        assertEquals("enr", out);
        assertEquals(1, enr.calls);
        assertEquals(0, ollama.calls);
        assertEquals(0, nativeEvo.calls);
    }

    @Test
    public void testEnrMetadataWithoutProviderFailsExplicitly() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        // ENR provider intentionally NOT registered.
        LlmRouter router = new LlmRouter();
        router.setLocalProvider(ollama);
        router.setNativeEvoProvider(nativeEvo);

        TaskContext context = new TaskContext(null, new java.io.File(System.getProperty("user.dir")));
        context.getMetadata().put("provider", "enr");

        try {
            router.sendLocalRequest(null, "hello", 0.7f, null, context);
            fail("Expected IllegalStateException when ENR selected via metadata but not registered");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("ENR"));
        }
        assertEquals(0, ollama.calls);
    }

    @Test
    public void testLegacyEnrEvoNativeLabelRecognizedAsEnr() {
        assertTrue(LlmRouter.isEnrEngine("ENR"));
        assertTrue(LlmRouter.isEnrEngine("enr"));
        assertTrue(LlmRouter.isEnrEngine("ENR-EVO Native"));
        assertTrue(LlmRouter.isEnrEngine("enr-evo native"));
        assertTrue(LlmRouter.isEnrEngine("EVO Native ENR"));
        assertFalse(LlmRouter.isEnrEngine("Ollama"));
        assertFalse(LlmRouter.isEnrEngine("EVO Native"));
        assertFalse(LlmRouter.isEnrEngine("LLAMA-CPP"));
        assertFalse(LlmRouter.isEnrEngine("llama.cpp"));
        assertFalse(LlmRouter.isEnrEngine(null));
        assertFalse(LlmRouter.isEnrEngine(""));
    }

    @Test
    public void testProviderKeyNormalization() {
        assertEquals("ollama", LlmRouter.normalizeProviderKey("Ollama"));
        assertEquals("ollama", LlmRouter.normalizeProviderKey("ollama"));
        assertEquals("llama-cpp", LlmRouter.normalizeProviderKey("llama.cpp"));
        assertEquals("llama-cpp", LlmRouter.normalizeProviderKey("LLAMA-CPP"));
        assertEquals("llama-cpp", LlmRouter.normalizeProviderKey("llama_cpp"));
        assertEquals("enr", LlmRouter.normalizeProviderKey("EVO Native ENR"));
        assertEquals("enr", LlmRouter.normalizeProviderKey("ENR-EVO Native"));
        assertEquals("enr", LlmRouter.normalizeProviderKey("ENR"));
        assertEquals("", LlmRouter.normalizeProviderKey("unknown-provider"));
        assertEquals("", LlmRouter.normalizeProviderKey(null));
        assertEquals("", LlmRouter.normalizeProviderKey(""));
    }

    @Test
    public void testEnrSelectionTakesPrecedenceOverNativeEvoPolicy() throws Exception {
        RecordingProvider ollama = new RecordingProvider("ollama");
        RecordingProvider nativeEvo = new RecordingProvider("native");
        RecordingProvider enr = new RecordingProvider("enr");
        LlmRouter router = newRouter(ollama, nativeEvo, enr);

        // A native evo model would normally match the NATIVE EVO routing policy;
        // an explicit ENR selection must take precedence and route through ENR.
        Orchestrator orchestrator = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setLocalModel("my-candidate.evo");

        TaskContext context = new TaskContext(orchestrator, new java.io.File(System.getProperty("user.dir")));
        context.getMetadata().put("provider", "enr");
        context.getMetadata().put("inferenceEngine", "ENR");

        router.sendLlmRequest(orchestrator, "hello", 0.7f, null, context);

        assertEquals(1, enr.calls);
        assertEquals(0, nativeEvo.calls);
        assertEquals(0, ollama.calls);
    }
}
