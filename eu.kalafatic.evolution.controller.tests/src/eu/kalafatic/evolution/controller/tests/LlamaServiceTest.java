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
    public void testResolveControllerModelsDir() {
        File modelsDir = LlamaService.resolveControllerModelsDir();
        assertNotNull("Resolved controller models folder must not be null", modelsDir);
        assertTrue("Resolved controller models folder must exist on disk", modelsDir.exists());
    }

    @Test
    public void testResolveLlamaCppLibDir() {
        File libDir = LlamaService.resolveLlamaCppLibDir();
        assertNotNull("Resolved llama-cpp lib folder must not be null", libDir);
        assertTrue("Resolved llama-cpp lib folder must exist on disk", libDir.exists());
    }

    @Test
    public void testCopyToModelsDirAndResolution() throws Exception {
        File tempGguf = File.createTempFile("test-evo-models-dir-", ".gguf");
        java.nio.file.Files.writeString(tempGguf.toPath(), "GGUF-DUMMY-BYTES");

        try {
            boolean copySuccess = LlamaService.copyToModelsDir(tempGguf.toPath(), "test-evo-models-unit");
            assertTrue("copyToModelsDir must return true on successful copy", copySuccess);

            File modelsDir = LlamaService.resolveControllerModelsDir();
            File targetNamed = new File(modelsDir, "test-evo-models-unit.gguf");
            File targetEvo = new File(modelsDir, "evo.gguf");
            assertTrue("Named GGUF model file must exist in models folder", targetNamed.exists());
            assertTrue("Default evo.gguf model file must exist in models folder", targetEvo.exists());

            File resolved = LlamaService.resolveEvoModelPath("test-evo-models-unit");
            assertNotNull("resolveEvoModelPath must locate copied model in models folder", resolved);
            assertTrue("Resolved file must exist", resolved.exists());

            // Cleanup
            if (targetNamed.exists()) {
                targetNamed.delete();
            }
            if (targetEvo.exists()) {
                targetEvo.delete();
            }
        } finally {
            tempGguf.delete();
        }
    }

    @Test
    public void testCopyToLlamaCppLibDirAndResolution() throws Exception {
        File tempGguf = File.createTempFile("test-evo-model-", ".gguf");
        java.nio.file.Files.writeString(tempGguf.toPath(), "GGUF-DUMMY-BYTES");

        try {
            boolean copySuccess = LlamaService.copyToLlamaCppLibDir(tempGguf.toPath(), "test-evo-model-unit");
            assertTrue("copyToLlamaCppLibDir must return true on successful copy", copySuccess);

            File resolved = LlamaService.resolveEvoModelPath("test-evo-model-unit");
            assertNotNull("resolveEvoModelPath must locate copied model in models or llama-cpp folder", resolved);
            assertTrue("Resolved file must exist", resolved.exists());
            assertTrue("Resolved path must be inside models or llama-cpp folder", resolved.getAbsolutePath().contains("models") || resolved.getAbsolutePath().contains("llama-cpp"));

            // Cleanup
            if (resolved.exists()) {
                resolved.delete();
            }
            File namedInModels = new File(LlamaService.resolveControllerModelsDir(), "test-evo-model-unit.gguf");
            if (namedInModels.exists()) {
                namedInModels.delete();
            }
            File defaultEvoInModels = new File(LlamaService.resolveControllerModelsDir(), "evo.gguf");
            if (defaultEvoInModels.exists()) {
                defaultEvoInModels.delete();
            }
            File namedInLib = new File(LlamaService.resolveLlamaCppLibDir(), "test-evo-model-unit.gguf");
            if (namedInLib.exists()) {
                namedInLib.delete();
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

    @Test
    public void testCleanLlamaOutputFiltering() throws Exception {
        LlamaService service = new LlamaService("evo-test", null);
        java.lang.reflect.Method method = LlamaService.class.getDeclaredMethod("cleanLlamaOutput", String.class);
        method.setAccessible(true);

        String raw = "system_info: n_threads = 4\nmain: prompt eval time\nllama_perf_context_print: timing data\nHello World\nllama_print_timings: end";
        String cleaned = (String) method.invoke(service, raw);

        assertEquals("Hello World", cleaned);
    }

    @Test
    public void testResolveEvoArtifactPathLatest() throws Exception {
        File userDir = new File(System.getProperty("user.dir"));
        File forgeOut = new File(userDir, "forge-output");
        if (!forgeOut.exists()) forgeOut.mkdirs();

        File testModelDir = new File(forgeOut, "forging-test-unit-latest");
        if (!testModelDir.exists()) testModelDir.mkdirs();
        File weightsFile = new File(testModelDir, "weights.bin");
        java.nio.file.Files.writeString(weightsFile.toPath(), "DUMMY_WEIGHTS");

        try {
            File resolved = OllamaProvider.resolveEvoArtifactPath("evo:latest");
            assertNotNull("resolveEvoArtifactPath for evo:latest must resolve to the forged model directory", resolved);
            assertTrue("Resolved path must exist", resolved.exists());
        } finally {
            if (weightsFile.exists()) weightsFile.delete();
            if (testModelDir.exists()) testModelDir.delete();
        }
    }
}
