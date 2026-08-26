package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class InferenceEngineSwitchingTest {

    @Test
    public void testDetectInferenceEngineForStandardModels() {
        assertEquals("ollama", LlamaService.detectInferenceEngine("llama3.2:3b"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("gemma:2b"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("mistral:7b"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("deepseek-r1"));
        assertEquals("ollama", LlamaService.detectInferenceEngine(null));
        assertEquals("ollama", LlamaService.detectInferenceEngine(""));
    }

    @Test
    public void testDetectInferenceEngineForGgufFiles() {
        assertEquals("llama-cpp", LlamaService.detectInferenceEngine("model.gguf"));
        assertEquals("llama-cpp", LlamaService.detectInferenceEngine("custom-model.GGUF"));
    }

    @Test
    public void testDetectInferenceEngineForNativeEvoFiles() {
        assertEquals("evo native", LlamaService.detectInferenceEngine("model.evo"));
        assertEquals("evo native", LlamaService.detectInferenceEngine("custom-model.EVO"));
    }

    @Test
    public void testDetectInferenceEngineForEvoModel() {
        String engine = LlamaService.detectInferenceEngine("evo");
        // Must be either 'llama-cpp' (if evo.gguf exists on disk) or 'evo native' (if native evo artifact)
        assertTrue("evo model engine must be llama-cpp or evo native",
                "llama-cpp".equals(engine) || "evo native".equals(engine));
    }

    @Test
    public void testTaskContextConfigurationForInferenceEngine() {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, new File(System.getProperty("user.dir")));

        context.getMetadata().put("inferenceEngine", "llama-cpp");
        assertEquals("llama-cpp", context.getMetadata().get("inferenceEngine"));

        context.getMetadata().put("inferenceEngine", "evo native");
        assertEquals("evo native", context.getMetadata().get("inferenceEngine"));

        context.getMetadata().put("inferenceEngine", "ollama");
        assertEquals("ollama", context.getMetadata().get("inferenceEngine"));
    }
}
