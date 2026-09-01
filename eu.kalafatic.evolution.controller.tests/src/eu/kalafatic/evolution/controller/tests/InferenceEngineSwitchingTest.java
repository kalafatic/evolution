package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class InferenceEngineSwitchingTest {

    @Test
    public void testRule1GgufModelsExceptEvoDefaultToOllama() {
        // Any .gguf model except evo/evo.gguf must default to ollama
        assertEquals("ollama", LlamaService.detectInferenceEngine("mistral.gguf"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("llama-3.2-1b.gguf"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("demo/custom.gguf"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("gemma.GGUF"));
    }

    @Test
    public void testRule2EvoLlmDefaultsToLlamaCpp() {
        // evo or evo.gguf must default to llama-cpp
        assertEquals("llama-cpp", LlamaService.detectInferenceEngine("evo"));
        assertEquals("llama-cpp", LlamaService.detectInferenceEngine("evo.gguf"));
        assertEquals("llama-cpp", LlamaService.detectInferenceEngine("EVO"));
        assertEquals("llama-cpp", LlamaService.detectInferenceEngine("EVO.GGUF"));
    }

    @Test
    public void testRule3OtherEvoBasedModelsDefaultToEvoNative() {
        // Other evo based models (with timestamps or candidate names) must default to evo native
        assertEquals("evo native", LlamaService.detectInferenceEngine("evo-generic-v128-e4-l4-h4-260826_062700"));
        assertEquals("evo native", LlamaService.detectInferenceEngine("evo-llm-001"));
        assertEquals("evo native", LlamaService.detectInferenceEngine("forging-1724653200"));
        assertEquals("evo native", LlamaService.detectInferenceEngine("my-evo-candidate"));
    }

    @Test
    public void testRule4StandardModelsDefaultToOllama() {
        assertEquals("ollama", LlamaService.detectInferenceEngine("llama3.2:3b"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("gemma:2b"));
        assertEquals("ollama", LlamaService.detectInferenceEngine("deepseek-r1"));
        assertEquals("ollama", LlamaService.detectInferenceEngine(null));
        assertEquals("ollama", LlamaService.detectInferenceEngine(""));
    }

    @Test
    public void testManualEngineOverridePersistence() {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        TaskContext context = new TaskContext(orchestrator, new File(System.getProperty("user.dir")));

        // User manually sets engine choice for session
        context.getMetadata().put("inferenceEngine", "llama-cpp");
        assertEquals("llama-cpp", context.getMetadata().get("inferenceEngine"));

        context.getMetadata().put("inferenceEngine", "evo native");
        assertEquals("evo native", context.getMetadata().get("inferenceEngine"));

        context.getMetadata().put("inferenceEngine", "ollama");
        assertEquals("ollama", context.getMetadata().get("inferenceEngine"));
    }

    @Test
    public void testFilterModelsByInferenceEngine() {
        List<String> inputModels = Arrays.asList(
                "evo-generic-v128",
                "forging-1724653200",
                "evo",
                "evo.gguf",
                "evo-model.gguf",
                "llama3.2:3b",
                "gemma:2b",
                "demo/custom.gguf"
        );

        ProjectModelManager pmm = ProjectModelManager.getInstance();

        // 1. evo native -> all evo models (evo only, non-gguf)
        List<String> nativeModels = pmm.filterModelsByEngine(inputModels, "evo native");
        assertEquals(Arrays.asList("evo-generic-v128", "forging-1724653200"), nativeModels);

        // 2. llama-cpp -> all evo gguf models (evo only)
        List<String> llamaCppModels = pmm.filterModelsByEngine(inputModels, "llama-cpp");
        assertEquals(Arrays.asList("evo", "evo.gguf", "evo-model.gguf"), llamaCppModels);

        // 3. ollama -> all ollama compatible models
        List<String> ollamaModels = pmm.filterModelsByEngine(inputModels, "ollama");
        assertEquals(Arrays.asList("evo", "evo.gguf", "evo-model.gguf", "llama3.2:3b", "gemma:2b", "demo/custom.gguf"), ollamaModels);
    }
}
