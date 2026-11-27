package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmResponse;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.forge.model.inference.InferenceProvenance;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Verifies that the actual inference provider and model (recorded in
 * {@link InferenceProvenance}) are propagated from the provider execution layer to the
 * {@link LlmResponse} consumed by the presentation layer — never inferred from a model
 * name or a configured/requested provider.
 */
public class ProviderModelMetadataPropagationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final class ProvenanceRecordingProvider implements ILlmProvider {
        final String providerName;
        final String modelName;

        ProvenanceRecordingProvider(String providerName, String modelName) {
            this.providerName = providerName;
            this.modelName = modelName;
        }

        @Override
        public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
                String proxyUrl, TaskContext context) {
            InferenceProvenance prov = new InferenceProvenance();
            prov.setRuntimeProvider(providerName);
            prov.setLoadedModelIdentity(modelName);
            prov.setActualEngine("FakeEngine");
            prov.setVerificationStatus("VERIFIED");
            if (context != null) {
                context.getMetadata().put("currentInferenceProvenance", prov);
            }
            return "ok";
        }
    }

    @Test
    public void testLlmRouterPropagatesProvenanceFromProviderToResponse() throws Exception {
        LlmRouter router = new LlmRouter();
        router.setLocalProvider(new ProvenanceRecordingProvider("Ollama",
                "deepseek-coder-v2:16b-lite-instruct-q4_K_M"));

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setLocalModel("deepseek-coder-v2:16b-lite-instruct-q4_K_M");

        TaskContext context = new TaskContext(orchestrator, tempFolder.newFolder().getAbsoluteFile());

        LlmResponse response = router.sendLlmRequest(orchestrator, "hi", 0.7f, null, context);

        assertNotNull(response);
        assertNotNull("Provenance must be propagated to the response",
                response.getInferenceProvenance());
        assertEquals("Ollama", response.getInferenceProvenance().getRuntimeProvider());
        assertEquals("deepseek-coder-v2:16b-lite-instruct-q4_K_M",
                response.getInferenceProvenance().getLoadedModelIdentity());
    }

    @Test
    public void testLlmRouterPropagatesActualProviderNotConfiguredModel() throws Exception {
        // The configured local model is irrelevant: the provider that actually ran must
        // win — this is the "do not display configured/requested provider" guarantee.
        LlmRouter router = new LlmRouter();
        router.setLocalProvider(new ProvenanceRecordingProvider("llama.cpp", "evo.gguf"));

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setLocalModel("some-configured-model");

        TaskContext context = new TaskContext(orchestrator, tempFolder.newFolder().getAbsoluteFile());

        LlmResponse response = router.sendLlmRequest(orchestrator, "hi", 0.7f, null, context);

        assertNotNull(response);
        assertNotNull(response.getInferenceProvenance());
        assertEquals("llama.cpp", response.getInferenceProvenance().getRuntimeProvider());
        assertEquals("evo.gguf", response.getInferenceProvenance().getLoadedModelIdentity());
    }

    @Test
    public void testLlmResponseWithoutProvenanceIsUnaffected() throws Exception {
        // When no provider records provenance, the response simply has none — never a
        // fabricated value (the UI omits the metadata line in that case).
        LlmRouter router = new LlmRouter();
        router.setLocalProvider(new ILlmProvider() {
            @Override
            public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
                    String proxyUrl, TaskContext context) {
                return "ok";
            }
        });

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setAiMode(AiMode.LOCAL);
        orchestrator.setLocalModel("some-model");

        TaskContext context = new TaskContext(orchestrator, tempFolder.newFolder().getAbsoluteFile());

        LlmResponse response = router.sendLlmRequest(orchestrator, "hi", 0.7f, null, context);

        assertNotNull(response);
        assertEquals(null, response.getInferenceProvenance());
    }
}
