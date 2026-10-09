package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.manager.removal.FileDeletionUtil;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalResult;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalService;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalStatus;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class ModelRemovalReliabilityTest {

    private Path tempDir;
    private Orchestrator orchestrator;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("evo-removal-test-");
        orchestrator = ProjectModelManager.getInstance().createOrchestrator("test-orch", "Test Orchestrator");
    }

    @After
    public void tearDown() {
        if (tempDir != null && Files.exists(tempDir)) {
            FileDeletionUtil.deleteDirectoryRecursive(tempDir);
        }
    }

    @Test
    public void testFileDeletionUtilSingleFile() throws IOException {
        Path fileToTest = tempDir.resolve("sample-model.gguf");
        Files.writeString(fileToTest, "dummy gguf bytes");
        assertTrue(Files.exists(fileToTest));

        FileDeletionUtil.DeletionStepResult res = FileDeletionUtil.deleteFile(fileToTest);
        assertTrue(res.deleted());
        assertTrue(res.existed());
        assertFalse(Files.exists(fileToTest));

        // Delete already absent file
        FileDeletionUtil.DeletionStepResult resAbsent = FileDeletionUtil.deleteFile(fileToTest);
        assertTrue(resAbsent.deleted());
        assertFalse(resAbsent.existed());
    }

    @Test
    public void testFileDeletionUtilDirectoryRecursive() throws IOException {
        Path dirToTest = tempDir.resolve("forging-test-model");
        Files.createDirectories(dirToTest);
        Path child1 = dirToTest.resolve("weights.bin");
        Path child2 = dirToTest.resolve("config.json");
        Files.writeString(child1, "weights");
        Files.writeString(child2, "config");

        FileDeletionUtil.DirectoryDeletionResult dirRes = FileDeletionUtil.deleteDirectoryRecursive(dirToTest);
        assertTrue(dirRes.success());
        assertFalse(Files.exists(dirToTest));
        assertFalse(Files.exists(child1));
        assertFalse(Files.exists(child2));
    }

    @Test
    public void testRemoteProviderRemoval() {
        AIProvider remoteProvider = OrchestrationFactory.eINSTANCE.createAIProvider();
        remoteProvider.setName("openai-gpt4");
        remoteProvider.setLocal(false);
        remoteProvider.setUrl("https://api.openai.com/v1");

        orchestrator.getAiProviders().add(remoteProvider);

        ModelRemovalResult result = ModelRemovalService.getInstance().removeSingleModel("rem-001", orchestrator, remoteProvider);
        assertNotNull(result);
        assertEquals(ModelRemovalStatus.SUCCESS, result.getStatus());
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }

    @Test
    public void testEvoNativeArtifactRemoval() throws IOException {
        Path evoFile = tempDir.resolve("model-v1.evo");
        Files.writeString(evoFile, "dummy evo binary");

        AIProvider localEvo = OrchestrationFactory.eINSTANCE.createAIProvider();
        localEvo.setName("model-v1.evo");
        localEvo.setLocal(true);
        localEvo.setUrl(evoFile.toString());
        localEvo.setFormat("evo_native");

        orchestrator.getAiProviders().add(localEvo);

        ModelRemovalResult result = ModelRemovalService.getInstance().removeSingleModel("evo-001", orchestrator, localEvo);
        assertNotNull(result);
        assertEquals(ModelRemovalStatus.SUCCESS, result.getStatus());
        assertFalse(Files.exists(evoFile));
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }

    @Test
    public void testSharedArtifactProtection() throws IOException {
        Path sharedModelsDir = tempDir.resolve(".ollama").resolve("models");
        Files.createDirectories(sharedModelsDir);

        AIProvider sharedProvider = OrchestrationFactory.eINSTANCE.createAIProvider();
        sharedProvider.setName("llama3");
        sharedProvider.setLocal(true);
        sharedProvider.setUrl(sharedModelsDir.toString());

        orchestrator.getAiProviders().add(sharedProvider);

        ModelRemovalResult result = ModelRemovalService.getInstance().removeSingleModel("shared-001", orchestrator, sharedProvider);
        assertNotNull(result);
        assertTrue(Files.exists(sharedModelsDir));
    }

    @Test
    public void testBatchRemovalWithMixedTypes() throws IOException {
        Path localGguf = tempDir.resolve("evo-test-model.gguf");
        Files.writeString(localGguf, "gguf contents");

        AIProvider p1 = OrchestrationFactory.eINSTANCE.createAIProvider();
        p1.setName("evo-test-model.gguf");
        p1.setLocal(true);
        p1.setUrl(localGguf.toString());

        AIProvider p2 = OrchestrationFactory.eINSTANCE.createAIProvider();
        p2.setName("claude-3-5");
        p2.setLocal(false);
        p2.setUrl("https://api.anthropic.com");

        orchestrator.getAiProviders().add(p1);
        orchestrator.getAiProviders().add(p2);

        List<ModelRemovalResult> batchResults = ModelRemovalService.getInstance().removeModels(orchestrator, List.of(p1, p2));
        assertEquals(2, batchResults.size());
        assertFalse(Files.exists(localGguf));
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }
}
