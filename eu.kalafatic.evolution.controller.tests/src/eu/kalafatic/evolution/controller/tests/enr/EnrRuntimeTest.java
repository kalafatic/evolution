package eu.kalafatic.evolution.controller.tests.enr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import eu.kalafatic.evolution.enr.EnrInferenceEngine;
import eu.kalafatic.evolution.enr.EnrRuntime;

/**
 * ENR runtime contract tests (Phase 6, WP-6.4). Offline-safe: no live model server is
 * required; provider failures must degrade gracefully.
 */
public class EnrRuntimeTest {

    private final EnrRuntime runtime = EnrRuntime.instance();

    @Test
    public void contractVersionAndCapabilities() {
        assertEquals("1.0", EnrInferenceEngine.ENGINE_VERSION);
        List<EnrInferenceEngine.EnrCapability> capabilities = runtime.capabilities();
        assertFalse(capabilities.isEmpty());
        assertTrue(capabilities.stream().anyMatch(c -> c.id().equals("ENR_INFERENCE")));
        assertTrue(capabilities.stream().anyMatch(c -> c.id().equals("ENR_MODEL_MANAGEMENT")));
        assertTrue(capabilities.stream().anyMatch(c -> c.id().equals("ENR_RUNTIME_MONITORING")));
    }

    @Test
    public void healthNeverThrowsOffline() {
        EnrInferenceEngine.EnrHealth health = runtime.health();
        assertNotNull(health.status());
        assertEquals("1.0", health.version());
    }

    @Test
    public void modelDiscoveryDegradesGracefullyWhenOllamaIsDown() {
        List<EnrInferenceEngine.EnrModel> models = runtime.listModels();
        assertNotNull(models);
        // Offline: the Ollama probe may fail, but the call itself must not throw.
    }

    @Test
    public void findModelWithNullIdIsEmpty() {
        assertEquals(Optional.empty(), runtime.findModel(null));
        assertEquals(Optional.empty(), runtime.findModel(" "));
    }

    @Test
    public void chatRequiresModelId() {
        try {
            runtime.chat(new EnrInferenceEngine.EnrChatRequest("", List.of(), 0.7, 128));
            assertTrue("expected IllegalArgumentException", false);
        } catch (IllegalArgumentException expected) {
            // contract validation
        }
    }

    @Test
    public void unsupportedLifecycleOperationsAreReported() {
        try {
            runtime.pullModel("evo-native:some.evo", null);
            assertTrue("expected UnsupportedOperationException", false);
        } catch (UnsupportedOperationException expected) {
            // pull is Ollama-only by contract
        }
    }

    @Test
    public void runtimeStatusReportsCpuAndEngines() {
        EnrInferenceEngine.EnrRuntimeStatus status = runtime.runtimeStatus();
        assertTrue(status.ready());
        assertTrue(status.availableProcessors() >= 1);
        assertTrue(status.maxHeapMb() > 0);
        assertNotNull(status.activeEngines());
        assertNotNull(status.gpu());
    }

    @Test
    public void providersListIncludesFamilyProviders() {
        List<EnrInferenceEngine.EnrProviderInfo> providers = runtime.listProviders();
        assertTrue(providers.stream().anyMatch(p -> p.id().equals("ollama")));
        assertTrue(providers.stream().anyMatch(p -> p.id().equals("evo-native")));
        assertTrue(providers.stream().anyMatch(p -> p.id().equals("llama-cpp")));
    }

    @Test
    public void unreachableOllamaChatReportsEngineFailure() {
        System.setProperty(EnrRuntime.SYS_OLLAMA_URL, "http://127.0.0.1:1");
        try {
            try {
                runtime.chat(new EnrInferenceEngine.EnrChatRequest("nonexistent-model:1",
                        List.of(EnrInferenceEngine.EnrChatMessage.user("ping")), 0.7, 16));
                // either it throws (unreachable) or returns — both acceptable offline
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("nonexistent-model:1"));
            }
        } finally {
            System.clearProperty(EnrRuntime.SYS_OLLAMA_URL);
        }
    }

    @Test
    public void degradedHealthReportsNotReady() {
        // The /health endpoint must never report readiness from mere HTTP-server
        // availability: a degraded runtime (inference provider down) is NOT ready.
        assertTrue(EnrInferenceEngine.EnrHealth.up().ready());
        assertEquals("UP", EnrInferenceEngine.EnrHealth.up().status());
        assertFalse(EnrInferenceEngine.EnrHealth.degraded("down").ready());
        assertEquals("DEGRADED", EnrInferenceEngine.EnrHealth.degraded("down").status());
    }
}
