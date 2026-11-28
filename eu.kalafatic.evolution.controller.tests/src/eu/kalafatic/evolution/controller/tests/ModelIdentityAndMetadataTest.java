package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.manager.ModelEditingService;
import eu.kalafatic.evolution.controller.manager.ModelIdentity;
import eu.kalafatic.evolution.controller.manager.ModelIdentityResolver;
import eu.kalafatic.evolution.controller.manager.ModelSearch;
import eu.kalafatic.evolution.controller.manager.ModelStateStore;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.manager.removal.FileDeletionUtil;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Verifies canonical model identity, search semantics and metadata/rename
 * behaviour without touching a real runtime.
 */
public class ModelIdentityAndMetadataTest {

    private Path tempDir;
    private Path ownedModelsDir;
    private Orchestrator orchestrator;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("evo-model-identity-");
        ownedModelsDir = tempDir.resolve("owned-models");
        Files.createDirectories(ownedModelsDir);
        System.setProperty("evo.test.modelDirsOverride", ownedModelsDir.toString());
        ModelStateStore.setStateFileForTesting(tempDir.resolve("model-state.json"));
        orchestrator = ProjectModelManager.getInstance().createOrchestrator("identity-orch", "Identity Orchestrator");
    }

    @After
    public void tearDown() {
        ModelStateStore.resetStateFile();
        System.clearProperty("evo.test.modelDirsOverride");
        if (tempDir != null && Files.exists(tempDir)) {
            FileDeletionUtil.deleteDirectoryRecursive(tempDir);
        }
    }

    @Test
    public void testImplicitLatestSharesIdentity() {
        AIProvider plain = provider("model-a", true, "http://127.0.0.1:11434", "ollama");
        AIProvider latest = provider("model-a:latest", true, "http://127.0.0.1:11434", "ollama");

        assertEquals(ModelIdentityResolver.resolve(orchestrator, plain).getCanonicalId(),
                ModelIdentityResolver.resolve(orchestrator, latest).getCanonicalId());
        assertEquals("model-a:latest",
                ModelIdentityResolver.resolve(orchestrator, plain).getRuntimeName());
    }

    @Test
    public void testExplicitTagsRemainDistinct() {
        AIProvider plain = provider("model-b", true, "http://127.0.0.1:11434", "ollama");
        AIProvider tagged = provider("model-b:7b", true, "http://127.0.0.1:11434", "ollama");

        assertNotEquals(ModelIdentityResolver.resolve(orchestrator, plain).getCanonicalId(),
                ModelIdentityResolver.resolve(orchestrator, tagged).getCanonicalId());
    }

    @Test
    public void testEvoArtifactIsNotBlindlyEquatedWithOllamaModel() {
        AIProvider ollama = provider("evo", true, "http://127.0.0.1:11434", "ollama");
        AIProvider artifact = provider("evo.evo", true,
                ownedModelsDir.resolve("evo.evo").toString(), "evo_native");

        ModelIdentity ollamaIdentity = ModelIdentityResolver.resolve(orchestrator, ollama);
        ModelIdentity artifactIdentity = ModelIdentityResolver.resolve(orchestrator, artifact);

        assertNotEquals(ollamaIdentity.getCanonicalId(), artifactIdentity.getCanonicalId());
        assertFalse(ProjectModelManager.getInstance().matchesIdentity(orchestrator, ollamaIdentity, artifact));
    }

    @Test
    public void testOllamaProviderPrefixIsNormalized() {
        AIProvider prefixed = provider("ollama:deepseek-coder", true,
                "http://127.0.0.1:11434", "ollama");
        AIProvider plain = provider("deepseek-coder", true,
                "http://127.0.0.1:11434", "ollama");

        ModelIdentity prefixedIdentity = ModelIdentityResolver.resolve(orchestrator, prefixed);
        ModelIdentity plainIdentity = ModelIdentityResolver.resolve(orchestrator, plain);

        assertEquals("deepseek-coder", prefixedIdentity.getTechnicalId());
        assertEquals(plainIdentity.getCanonicalId(), prefixedIdentity.getCanonicalId());
    }

    @Test
    public void testSearchMatchesTechnicalIdAndCombinedFields() {
        assertTrue(ModelSearch.matchesAllTerms("evo native", "Model X", "EVO Native", "Available"));
        assertTrue(ModelSearch.matchesAllTerms("regi", "Registered"));
        assertFalse(ModelSearch.matchesAllTerms("ollama", "EVO Native", "Remote"));
        assertTrue(ModelSearch.matchesSelection("All", "Registered"));
        assertTrue(ModelSearch.matchesSelection("Registered", "Registered"));
        assertFalse(ModelSearch.matchesSelection("Registered", "Discovered"));
    }

    @Test
    public void testMetadataPersistsAndTechnicalIdentityIsPreserved() {
        AIProvider provider = provider("persist-model", true, "http://127.0.0.1:11434", "ollama");
        orchestrator.getAiProviders().add(provider);
        ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);

        ModelEditingService.RenameResult result = ModelEditingService.getInstance().saveMetadata(
                orchestrator, provider,
                new ModelEditingService.MetadataUpdate()
                        .displayName("Friendly Name")
                        .description("User description")
                        .tags(List.of("chat", "local"))
                        .aliases(List.of("alias-one")));
        assertTrue(result.isSuccess());
        assertEquals("persist-model", provider.getName());

        // Simulate a restart: reset in-memory state but keep the same file.
        ModelStateStore.setStateFileForTesting(tempDir.resolve("model-state.json"));
        ModelStateStore.Metadata reloaded = ModelStateStore.getInstance().get(identity.getCanonicalId());
        assertNotNull(reloaded);
        assertEquals("Friendly Name", reloaded.getDisplayName());
        assertEquals("User description", reloaded.getDescription());
        assertTrue(reloaded.getTags().contains("chat"));
    }

    @Test
    public void testDuplicateDisplayNameIsRejected() {
        AIProvider alpha = provider("alpha", false, "https://alpha.invalid", "openai");
        AIProvider beta = provider("beta", false, "https://beta.invalid", "openai");
        orchestrator.getAiProviders().add(alpha);
        orchestrator.getAiProviders().add(beta);

        assertTrue(ModelEditingService.getInstance().saveMetadata(orchestrator, alpha,
                new ModelEditingService.MetadataUpdate().displayName("Alpha")).isSuccess());

        ModelEditingService.RenameResult conflict = ModelEditingService.getInstance().saveMetadata(
                orchestrator, beta, new ModelEditingService.MetadataUpdate().displayName("Alpha"));
        assertFalse(conflict.isSuccess());
        assertEquals("beta", beta.getName());
    }

    @Test
    public void testArtifactRenameUpdatesReferencesAtomically() throws IOException {
        Path artifact = ownedModelsDir.resolve("old-name.evo");
        Files.writeString(artifact, "artifact bytes");

        AIProvider provider = provider("old-name.evo", true, artifact.toString(), "evo_native");
        orchestrator.getAiProviders().add(provider);
        orchestrator.setLocalModel("old-name.evo");

        ModelEditingService.RenameResult result = ModelEditingService.getInstance()
                .renameArtifact(orchestrator, provider, "new-name.evo");

        assertTrue(result.getMessage(), result.isSuccess());
        assertTrue(result.isArtifactRenamed());
        Path renamed = ownedModelsDir.resolve("new-name.evo");
        assertTrue(Files.exists(renamed));
        assertFalse(Files.exists(artifact));
        assertEquals(renamed.toAbsolutePath().normalize().toString(),
                Path.of(provider.getUrl()).toAbsolutePath().normalize().toString());
        assertEquals("new-name.evo", provider.getName());
        assertEquals("new-name.evo", orchestrator.getLocalModel());
    }

    @Test
    public void testOllamaRenameIsReportedAsUnsupported() {
        AIProvider provider = provider("ollama-model", true, "http://127.0.0.1:11434", "ollama");
        orchestrator.getAiProviders().add(provider);

        ModelEditingService.RenameResult result = ModelEditingService.getInstance()
                .renameArtifact(orchestrator, provider, "renamed-model");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("not supported"));
        assertEquals("ollama-model", provider.getName());
    }

    @Test
    public void testSuppressionPreventsSilentRegistrationRestore() {
        AIProvider provider = provider("suppressed-model", true, "http://127.0.0.1:11434", "ollama");
        ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);

        ModelStateStore store = ModelStateStore.getInstance();
        assertTrue(store.suppress(identity.getCanonicalId(), "test"));
        assertTrue(java.nio.file.Files.isRegularFile(store.getStateFile()));
        assertTrue(store.isSuppressed(identity.getCanonicalId()));

        // Simulate an application restart: the marker must survive reload.
        ModelStateStore.setStateFileForTesting(tempDir.resolve("model-state.json"));
        assertTrue(ModelStateStore.getInstance().isSuppressed(identity.getCanonicalId()));

        ModelStateStore.getInstance().unsuppress(identity.getCanonicalId());
        assertFalse(ModelStateStore.getInstance().isSuppressed(identity.getCanonicalId()));
    }

    private AIProvider provider(String name, boolean local, String url, String format) {
        AIProvider provider = OrchestrationFactory.eINSTANCE.createAIProvider();
        provider.setName(name);
        provider.setLocal(local);
        provider.setUrl(url);
        provider.setFormat(format);
        return provider;
    }
}
