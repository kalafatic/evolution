package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.llm.NativeEvoProvider;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.forge.model.inference.InferenceProvenance;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmArchitecture;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class NativeEvoProviderIndependenceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testNativeEvoProviderRegisteredInAiProviders() {
        assertTrue("AiProviders must contain 'native_evo'", AiProviders.PROVIDERS.containsKey("native_evo"));
        assertEquals("evo://native", AiProviders.PROVIDERS.get("native_evo").getUrl());
    }

    @Test
    public void testNativeEvoInferenceWorksWithoutOllama() throws Exception {
        // 1. Create a minimal valid native .evo model artifact
        Path modelsDir = tempFolder.newFolder("models").toPath();
        Path evoFile = modelsDir.resolve("test-nano.evo");

        Map<String, Integer> vocab = new HashMap<>();
        vocab.put("<pad>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put("hello", 3);
        vocab.put("world", 4);

        EvoLlmArchitecture arch = new EvoLlmArchitecture(vocab.size(), 16, 1, 1, 32, 16);
        EvoLlmModel model = new EvoLlmModel(arch);

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("test-nano", model, vocab);
        artifact.save(evoFile);

        assertTrue("Generated .evo file must exist", Files.exists(evoFile));

        // 2. Set orchestrator with NO Ollama configuration (null Ollama settings, local model pointing to .evo)
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setLocalModel(evoFile.toAbsolutePath().toString());
        orchestrator.setOllama(null); // Explicitly nullify Ollama

        TaskContext context = new TaskContext(orchestrator, modelsDir.toFile());
        InferenceProvenance prov = new InferenceProvenance();
        context.getMetadata().put("currentInferenceProvenance", prov);

        // 3. Invoke NativeEvoProvider directly
        NativeEvoProvider provider = new NativeEvoProvider();
        String response = provider.sendRequest(orchestrator, "hello", 0.0f, null, context);

        assertNotNull("Response must not be null", response);
        assertEquals("ReferenceEvoInferenceEngine", prov.getActualEngine());
        assertEquals("NativeEvoProvider", prov.getRuntimeProvider());
        assertTrue("Must be marked native", prov.isNative());
        assertFalse("Ollama fallback must not be activated", prov.isFallbackActivated());

        // 4. Invoke via LlmRouter to ensure routing policy reaches NativeEvoProvider without Ollama
        LlmRouter router = LlmRouter.getInstance();
        String routedResponse = router.sendRequest(orchestrator, "hello", 0.0f, null, context);
        assertNotNull("Routed response must not be null", routedResponse);
    }
}
