package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;
import eu.kalafatic.evolution.controller.orchestration.llm.ProviderLifecycle;
import eu.kalafatic.evolution.controller.orchestration.llm.ProviderLifecycleRegistry;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Verifies the provider availability/startup registry contract: an available provider is
 * never started, an unavailable provider is started exactly once (even under concurrent
 * requests), and failures/timeouts propagate instead of silently continuing.
 */
public class ProviderLifecycleRegistryTest {

    private static final class FakeLifecycle implements ProviderLifecycle {
        final String key;
        final String name;
        final AtomicBoolean available = new AtomicBoolean(false);
        final AtomicInteger startCalls = new AtomicInteger();
        final AtomicBoolean healthy = new AtomicBoolean(true);
        volatile CountDownLatch startGate;
        volatile RuntimeException startError;

        FakeLifecycle(String key, String name) {
            this.key = key;
            this.name = name;
        }

        @Override
        public String providerKey() {
            return key;
        }

        @Override
        public String displayName() {
            return name;
        }

        @Override
        public boolean isAvailable(Orchestrator orchestrator) {
            return available.get();
        }

        @Override
        public CompletableFuture<Void> start(Orchestrator orchestrator, Consumer<String> progress) {
            startCalls.incrementAndGet();
            return CompletableFuture.runAsync(() -> {
                if (startError != null) {
                    throw startError;
                }
                if (startGate != null) {
                    try {
                        startGate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                available.set(true);
            });
        }

        @Override
        public CompletableFuture<Boolean> waitHealthy(Orchestrator orchestrator, long timeoutMillis) {
            return CompletableFuture.completedFuture(healthy.get());
        }

        @Override
        public List<LocalModelDescriptor> discoverModels(Orchestrator orchestrator) {
            return List.of();
        }
    }

    @Test
    public void testAvailableProviderIsNeverStarted() throws Exception {
        ProviderLifecycleRegistry registry = newRegistry();
        FakeLifecycle lifecycle = new FakeLifecycle("test-a", "Test A");
        lifecycle.available.set(true);
        registry.register(lifecycle);

        ProviderLifecycle result = registry.ensureAvailable("test-a", null, null)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(0, lifecycle.startCalls.get());
    }

    @Test
    public void testUnavailableProviderStartsExactlyOnce() throws Exception {
        ProviderLifecycleRegistry registry = newRegistry();
        FakeLifecycle lifecycle = new FakeLifecycle("test-b", "Test B");
        registry.register(lifecycle);

        registry.ensureAvailable("test-b", null, null).get(5, TimeUnit.SECONDS);
        registry.ensureAvailable("test-b", null, null).get(5, TimeUnit.SECONDS);

        assertEquals(1, lifecycle.startCalls.get());
        assertTrue(lifecycle.available.get());
    }

    @Test
    public void testConcurrentEnsureStartsOnlyOnce() throws Exception {
        ProviderLifecycleRegistry registry = newRegistry();
        FakeLifecycle lifecycle = new FakeLifecycle("test-c", "Test C");
        lifecycle.startGate = new CountDownLatch(1);
        registry.register(lifecycle);

        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<ProviderLifecycle>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    start.await();
                } catch (InterruptedException ignored) {
                }
                return registry.ensureAvailable("test-c", null, null);
            }).thenCompose(f -> f));
        }
        start.countDown();
        // Wait for all callers to reach start() before releasing the gate.
        Thread.sleep(300);
        lifecycle.startGate.countDown();

        for (CompletableFuture<ProviderLifecycle> f : futures) {
            assertNotNull(f.get(10, TimeUnit.SECONDS));
        }
        assertEquals("Concurrent requests must start the provider exactly once",
                1, lifecycle.startCalls.get());
    }

    @Test
    public void testStartupFailurePropagates() throws Exception {
        ProviderLifecycleRegistry registry = newRegistry();
        FakeLifecycle lifecycle = new FakeLifecycle("test-d", "Test D");
        lifecycle.startError = new RuntimeException("boom");
        registry.register(lifecycle);

        try {
            registry.ensureAvailable("test-d", null, null).get(5, TimeUnit.SECONDS);
            fail("Expected startup failure to propagate");
        } catch (Exception expected) {
            assertTrue(String.valueOf(expected),
                    String.valueOf(expected).contains("boom"));
        }
    }

    @Test
    public void testUnhealthyProviderFailsAfterStartup() throws Exception {
        ProviderLifecycleRegistry registry = newRegistry();
        FakeLifecycle lifecycle = new FakeLifecycle("test-e", "Test E");
        lifecycle.healthy.set(false); // waitHealthy returns false
        registry.register(lifecycle);

        try {
            registry.ensureAvailable("test-e", null, null).get(5, TimeUnit.SECONDS);
            fail("Expected unhealthy timeout to fail");
        } catch (Exception expected) {
            assertTrue(String.valueOf(expected).contains("did not become healthy"));
        }
    }

    @Test
    public void testForKeyNormalizesProviderLabels() {
        ProviderLifecycleRegistry registry = newRegistry();
        // Ollama is registered eagerly by the registry; verify lookup by label works.
        ProviderLifecycle lifecycle = registry.forKey("Ollama");
        assertNotNull(lifecycle);
        assertEquals("ollama", lifecycle.providerKey());
        // llama.cpp has no lifecycle (no startup mechanism).
        assertNull(registry.forKey("llama.cpp"));
    }

    @Test
    public void testRetryAfterFailedStartup() throws Exception {
        ProviderLifecycleRegistry registry = newRegistry();
        FakeLifecycle lifecycle = new FakeLifecycle("test-f", "Test F");
        lifecycle.startError = new RuntimeException("boom");
        registry.register(lifecycle);

        try {
            registry.ensureAvailable("test-f", null, null).get(5, TimeUnit.SECONDS);
            fail("Expected startup failure to propagate");
        } catch (Exception expected) {
            // expected
        }
        waitUntilNotStarting(registry, "test-f");

        // A subsequent request retries the startup (the failed future was cleaned up).
        lifecycle.startError = null;
        lifecycle.available.set(false);
        ProviderLifecycle result = registry.ensureAvailable("test-f", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(2, lifecycle.startCalls.get());
    }

    @Test
    public void testNoStaleStartupEntriesAfterCompletion() throws Exception {
        ProviderLifecycleRegistry registry = newRegistry();
        FakeLifecycle lifecycle = new FakeLifecycle("test-g", "Test G");
        registry.register(lifecycle);

        registry.ensureAvailable("test-g", null, null).get(5, TimeUnit.SECONDS);
        waitUntilNotStarting(registry, "test-g");
        assertFalse("Completed startup future must be removed", registry.isStarting("test-g"));
    }

    private static void waitUntilNotStarting(ProviderLifecycleRegistry registry, String key)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (registry.isStarting(key) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    private static ProviderLifecycleRegistry newRegistry() {
        // The singleton already contains Ollama; isolate per-test with a fresh registry via
        // a reflective-free approach: reuse the singleton but only assert on our fake keys.
        return ProviderLifecycleRegistry.getInstance();
    }
}
