package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.manager.ModelIdentityResolver;
import eu.kalafatic.evolution.controller.manager.ModelStateStore;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.manager.removal.FileDeletionUtil;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalResult;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalService;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalStatus;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Verifies the model removal lifecycle: canonical identity, duplicate
 * registrations, runtime 404 idempotency, runtime unavailability, artifact
 * ownership, shared providers and repeated deletion.
 */
public class ModelRemovalReliabilityTest {

    private Path tempDir;
    private Path ownedModelsDir;
    private Orchestrator orchestrator;
    private HttpServer server;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("evo-removal-test-");
        ownedModelsDir = tempDir.resolve("owned-models");
        Files.createDirectories(ownedModelsDir);
        System.setProperty("evo.test.modelDirsOverride", ownedModelsDir.toString());
        ModelStateStore.setStateFileForTesting(tempDir.resolve("model-state.json"));
        orchestrator = ProjectModelManager.getInstance().createOrchestrator("test-orch", "Test Orchestrator");
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        ModelStateStore.resetStateFile();
        System.clearProperty("evo.test.modelDirsOverride");
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
        orchestrator.setRemoteModel("openai-gpt4");

        ModelRemovalResult result = ModelRemovalService.getInstance()
                .removeSingleModel("rem-001", orchestrator, remoteProvider);

        assertNotNull(result);
        assertEquals(ModelRemovalStatus.SUCCESS, result.getStatus());
        assertTrue(orchestrator.getAiProviders().isEmpty());
        assertEquals("", orchestrator.getRemoteModel());
        assertTrue(result.getRegistryOutcome().startsWith("Removed"));
        assertTrue(ModelStateStore.getInstance().isSuppressed(result.getCanonicalIdentity()));
    }

    @Test
    public void testEvoNativeArtifactRemovalWhenRequested() throws IOException {
        Path evoFile = ownedModelsDir.resolve("model-v1.evo");
        Files.writeString(evoFile, "dummy evo binary");

        AIProvider localEvo = OrchestrationFactory.eINSTANCE.createAIProvider();
        localEvo.setName("model-v1.evo");
        localEvo.setLocal(true);
        localEvo.setUrl(evoFile.toString());
        localEvo.setFormat("evo_native");
        orchestrator.getAiProviders().add(localEvo);
        orchestrator.setLocalModel("model-v1.evo");

        ModelRemovalResult result = ModelRemovalService.getInstance().removeSingleModel(
                "evo-001", orchestrator, localEvo,
                ModelRemovalService.RemovalOptions.defaults().deleteOwnedArtifacts(true));

        assertNotNull(result);
        assertEquals(ModelRemovalStatus.SUCCESS, result.getStatus());
        assertFalse(Files.exists(evoFile));
        assertTrue(orchestrator.getAiProviders().isEmpty());
        assertEquals("", orchestrator.getLocalModel());
        assertTrue(result.getArtifactOutcome().startsWith("Deleted"));
        assertTrue(ModelStateStore.getInstance().isSuppressed(result.getCanonicalIdentity()));
    }

    @Test
    public void testEvoNativeArtifactRetainedByDefault() throws IOException {
        Path evoFile = ownedModelsDir.resolve("model-v2.evo");
        Files.writeString(evoFile, "dummy evo binary");

        AIProvider localEvo = OrchestrationFactory.eINSTANCE.createAIProvider();
        localEvo.setName("model-v2.evo");
        localEvo.setLocal(true);
        localEvo.setUrl(evoFile.toString());
        localEvo.setFormat("evo_native");
        orchestrator.getAiProviders().add(localEvo);

        ModelRemovalResult result = ModelRemovalService.getInstance()
                .removeSingleModel("evo-002", orchestrator, localEvo);

        assertEquals(ModelRemovalStatus.SUCCESS, result.getStatus());
        assertTrue(Files.exists(evoFile));
        assertTrue(result.getArtifactOutcome().startsWith("Retained"));
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }

    @Test
    public void testArtifactSetRemovalDeletesEveryRepresentation() throws IOException {
        Path gguf = ownedModelsDir.resolve("evo-family.gguf");
        Path evo = ownedModelsDir.resolve("evo-family.evo");
        Path evoDir = ownedModelsDir.resolve("evo-family.evo");
        Path dirChild = evoDir.resolve("weights.bin");
        Path aiJson = ownedModelsDir.resolve("evo-family.gguf.ai.json");
        Path vocab = ownedModelsDir.resolve("evo-family_vocab.json");
        Path unrelated = ownedModelsDir.resolve("evo-family-other.gguf");

        Files.writeString(gguf, "gguf");
        Files.createDirectories(evoDir);
        Files.writeString(dirChild, "weights");
        Files.writeString(aiJson, "{}");
        Files.writeString(vocab, "{}");
        Files.writeString(unrelated, "keep");

        AIProvider provider = OrchestrationFactory.eINSTANCE.createAIProvider();
        provider.setName("evo-family");
        provider.setLocal(true);
        provider.setUrl("http://127.0.0.1:1");
        provider.setFormat("ollama");
        orchestrator.getAiProviders().add(provider);

        ModelRemovalResult result = ModelRemovalService.getInstance().removeSingleModel(
                "set-001", orchestrator, provider,
                ModelRemovalService.RemovalOptions.defaults().deleteOwnedArtifacts(true));

        assertFalse(Files.exists(gguf));
        assertFalse(Files.exists(evo));
        assertFalse(Files.exists(evoDir));
        assertFalse(Files.exists(aiJson));
        assertFalse(Files.exists(vocab));
        assertTrue("unrelated model artifact must remain", Files.exists(unrelated));
        assertTrue(result.getArtifactOutcome().startsWith("Deleted"));
    }

    @Test
    public void testSharedArtifactProtection() throws IOException {
        Path sharedModelsDir = tempDir.resolve(".ollama").resolve("models");
        Files.createDirectories(sharedModelsDir);
        Path sharedFile = sharedModelsDir.resolve("llama3.gguf");
        Files.writeString(sharedFile, "shared gguf");

        AIProvider sharedProvider = OrchestrationFactory.eINSTANCE.createAIProvider();
        sharedProvider.setName("llama3");
        sharedProvider.setLocal(true);
        sharedProvider.setUrl(sharedFile.toString());
        orchestrator.getAiProviders().add(sharedProvider);

        ModelRemovalResult result = ModelRemovalService.getInstance().removeSingleModel(
                "shared-001", orchestrator, sharedProvider,
                ModelRemovalService.RemovalOptions.defaults().deleteOwnedArtifacts(true));

        assertNotNull(result);
        assertTrue(Files.exists(sharedFile));
        assertFalse("external artifact must never be deleted: " + result.getArtifactOutcome(),
                result.getArtifactOutcome().startsWith("Deleted"));
    }

    @Test
    public void testGenericEvoGgufNotDeletedUnlessSharedDeletionAuthorised() throws IOException {
        Path generic = ownedModelsDir.resolve("evo.gguf");
        Files.writeString(generic, "generic evo model");

        AIProvider provider = OrchestrationFactory.eINSTANCE.createAIProvider();
        provider.setName("evo");
        provider.setLocal(true);
        provider.setUrl(generic.toString());
        provider.setFormat("ollama");
        orchestrator.getAiProviders().add(provider);

        ModelRemovalResult result = ModelRemovalService.getInstance().removeSingleModel(
                "generic-001", orchestrator, provider,
                ModelRemovalService.RemovalOptions.defaults().deleteOwnedArtifacts(true));

        assertTrue(Files.exists(generic));
        assertTrue(result.getArtifactOutcome().contains("shared"));
    }

    @Test
    public void testDuplicateOllamaIdentitiesRemovedTogether() {
        AIProvider untagged = OrchestrationFactory.eINSTANCE.createAIProvider();
        untagged.setName("dup-model");
        untagged.setLocal(true);
        untagged.setUrl("http://127.0.0.1:1");
        untagged.setFormat("ollama");

        AIProvider latest = OrchestrationFactory.eINSTANCE.createAIProvider();
        latest.setName("dup-model:latest");
        latest.setLocal(true);
        latest.setUrl("http://127.0.0.1:1");
        latest.setFormat("ollama");

        orchestrator.getAiProviders().add(untagged);
        orchestrator.getAiProviders().add(latest);

        assertEquals(ModelIdentityResolver.resolve(orchestrator, untagged).getCanonicalId(),
                ModelIdentityResolver.resolve(orchestrator, latest).getCanonicalId());

        ModelRemovalService.getInstance().removeSingleModel("dup-001", orchestrator, untagged);
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }

    @Test
    public void testExplicitTagsAreDistinctModels() {
        AIProvider base = OrchestrationFactory.eINSTANCE.createAIProvider();
        base.setName("qwen");
        base.setLocal(true);
        base.setUrl("http://127.0.0.1:1");
        base.setFormat("ollama");

        AIProvider tagged = OrchestrationFactory.eINSTANCE.createAIProvider();
        tagged.setName("qwen:7b");
        tagged.setLocal(true);
        tagged.setUrl("http://127.0.0.1:1");
        tagged.setFormat("ollama");

        assertFalse(ModelIdentityResolver.resolve(orchestrator, base).getCanonicalId()
                .equals(ModelIdentityResolver.resolve(orchestrator, tagged).getCanonicalId()));

        orchestrator.getAiProviders().add(base);
        orchestrator.getAiProviders().add(tagged);

        ModelRemovalService.getInstance().removeSingleModel("tag-001", orchestrator, base);
        assertEquals(1, orchestrator.getAiProviders().size());
        assertEquals("qwen:7b", orchestrator.getAiProviders().get(0).getName());
    }

    @Test
    public void testSharedProviderLeavesOtherModelsIntact() {
        AIProvider a = OrchestrationFactory.eINSTANCE.createAIProvider();
        a.setName("shared-a");
        a.setLocal(true);
        a.setUrl("http://127.0.0.1:1");
        a.setFormat("ollama");

        AIProvider b = OrchestrationFactory.eINSTANCE.createAIProvider();
        b.setName("shared-b");
        b.setLocal(true);
        b.setUrl("http://127.0.0.1:1");
        b.setFormat("ollama");

        orchestrator.getAiProviders().add(a);
        orchestrator.getAiProviders().add(b);

        ModelRemovalService.getInstance().removeSingleModel("shared-provider-1", orchestrator, a);

        assertEquals(1, orchestrator.getAiProviders().size());
        assertEquals("shared-b", orchestrator.getAiProviders().get(0).getName());
    }

    @Test
    public void testRuntimeUnavailableIsPartialSuccessNotSuccess() {
        AIProvider provider = OrchestrationFactory.eINSTANCE.createAIProvider();
        provider.setName("lost-model");
        provider.setLocal(true);
        provider.setUrl("http://127.0.0.1:1");
        provider.setFormat("ollama");
        orchestrator.getAiProviders().add(provider);

        ModelRemovalResult result = ModelRemovalService.getInstance()
                .removeSingleModel("lost-001", orchestrator, provider);

        assertEquals(ModelRemovalStatus.PARTIAL_SUCCESS, result.getStatus());
        assertTrue(orchestrator.getAiProviders().isEmpty());
        assertTrue(result.getRuntimeOutcome().toLowerCase().contains("unverified")
                || result.getRuntimeOutcome().toLowerCase().contains("retained"));
        assertEquals("Incomplete", result.getVerificationOutcome());
    }

    @Test
    public void testRuntime404IsIdempotentSuccess() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/api/generate", exchange -> {
            respond(exchange, 404, "{\"error\":\"not found\"}");
        });
        server.createContext("/api/delete", exchange -> {
            respond(exchange, 404, "{\"error\":\"model not found\"}");
        });
        server.createContext("/api/tags", exchange -> {
            respond(exchange, 200, "{\"models\":[]}");
        });
        server.start();

        AIProvider provider = OrchestrationFactory.eINSTANCE.createAIProvider();
        provider.setName("ghost-model:latest");
        provider.setLocal(true);
        provider.setUrl("http://127.0.0.1:" + port);
        provider.setFormat("ollama");
        orchestrator.getAiProviders().add(provider);

        ModelRemovalResult result = ModelRemovalService.getInstance()
                .removeSingleModel("ghost-001", orchestrator, provider);

        assertEquals(ModelRemovalStatus.SUCCESS, result.getStatus());
        assertEquals("Already absent", result.getRuntimeOutcome());
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }

    @Test
    public void testRepeatedRemovalIsIdempotent() {
        AIProvider provider = OrchestrationFactory.eINSTANCE.createAIProvider();
        provider.setName("repeat-model");
        provider.setLocal(false);
        provider.setUrl("https://example.invalid");
        orchestrator.getAiProviders().add(provider);

        ModelRemovalResult first = ModelRemovalService.getInstance()
                .removeSingleModel("repeat-1", orchestrator, provider);
        ModelRemovalResult second = ModelRemovalService.getInstance()
                .removeSingleModel("repeat-2", orchestrator, provider);

        assertEquals(ModelRemovalStatus.SUCCESS, first.getStatus());
        assertEquals(ModelRemovalStatus.SUCCESS, second.getStatus());
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }

    @Test
    public void testBatchRemovalWithMixedTypes() throws IOException {
        Path localGguf = ownedModelsDir.resolve("evo-test-model.gguf");
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

        List<ModelRemovalResult> batchResults = ModelRemovalService.getInstance().removeModels(
                orchestrator, List.of(p1, p2),
                ModelRemovalService.RemovalOptions.defaults().deleteOwnedArtifacts(true));

        assertEquals(2, batchResults.size());
        assertFalse(Files.exists(localGguf));
        assertTrue(orchestrator.getAiProviders().isEmpty());
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
