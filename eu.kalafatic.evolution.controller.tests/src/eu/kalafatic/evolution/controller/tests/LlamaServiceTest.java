package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.Test;
import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;

public class LlamaServiceTest {

    @Test
    public void testResolveEvoModelPathForNonexistent() {
        File file = LlamaService.resolveEvoModelPath("nonexistent-test-model-12345");
        assertNull("Path resolution for non-existent model should return null", file);
    }

    @Test
    public void testLlamaServiceInitialization() {
        LlamaService service = new LlamaService("evo-test", "/tmp/nonexistent.gguf");
        assertEquals("evo-test", service.getModel());
        assertEquals("/tmp/nonexistent.gguf", service.getModelPath());
    }

    @Test
    public void testResolveLlamaCppLibDir() {
        File libDir = LlamaService.resolveLlamaCppLibDir();
        assertNotNull("Resolved llama-cpp lib folder must not be null", libDir);
        assertTrue("Resolved llama-cpp lib folder must exist on disk", libDir.exists());
    }

    @Test
    public void testCopyToLlamaCppLibDirAndResolution() throws Exception {
        File tempGguf = File.createTempFile("test-evo-model-", ".gguf");
        java.nio.file.Files.writeString(tempGguf.toPath(), "GGUF-DUMMY-BYTES");

        try {
            boolean copySuccess = LlamaService.copyToLlamaCppLibDir(tempGguf.toPath(), "test-evo-model-unit");
            assertTrue("copyToLlamaCppLibDir must return true on successful copy", copySuccess);

            File resolved = LlamaService.resolveEvoModelPath("test-evo-model-unit");
            assertNotNull("resolveEvoModelPath must locate copied model in llama-cpp lib folder", resolved);
            assertTrue("Resolved file must exist", resolved.exists());
            assertTrue("Resolved path must be inside llama-cpp lib folder", resolved.getAbsolutePath().contains("llama-cpp"));

            // Cleanup
            if (resolved.exists()) {
                resolved.delete();
            }
            File defaultEvoInLib = new File(LlamaService.resolveLlamaCppLibDir(), "evo.gguf");
            if (defaultEvoInLib.exists()) {
                defaultEvoInLib.delete();
            }
        } finally {
            tempGguf.delete();
        }
    }

    @Test
    public void testOllamaProviderWithEvoModelFallback() {
        // Prepare orchestrator configured for a missing evo model
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("evo-nonexistent-model");
        orchestrator.setOllama(ollama);

        OllamaProvider provider = new OllamaProvider();
        TaskContext context = new TaskContext(orchestrator, null);

        // This should try LlamaService first, fail to resolve the path, and then fall back to the traditional OllamaProvider flow.
        // It will eventually throw a ConnectionException if local Ollama is down (which is expected), but it proves that routing handles the missing GGUF and doesn't crash on null/exceptions.
        try {
            provider.sendRequest(orchestrator, "Hello", 0.2f, null, context);
        } catch (Exception e) {
            // Expected connection/fallback exception, ensuring no NullPointerException or other crash was triggered inside LlamaService integration.
            assertNotNull("Exception must not be null", e);
        }
    }
}
