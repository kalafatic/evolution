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
}
