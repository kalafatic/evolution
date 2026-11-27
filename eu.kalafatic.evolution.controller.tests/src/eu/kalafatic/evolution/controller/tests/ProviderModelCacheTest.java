package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;
import eu.kalafatic.evolution.controller.orchestration.llm.ProviderModelCache;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Deterministic, headless tests for the shared provider-model cache: initialization
 * discovers every provider exactly once, repeated/concurrent initialization never
 * duplicates discovery, a single provider failure never discards the others, a failed
 * refresh preserves the previous valid list, and model descriptors retain their exact
 * inference identifiers.
 */
public class ProviderModelCacheTest {

    private static final List<String> KEYS = List.of("ollama", "llama-cpp", "enr");

    private static LocalModelDescriptor model(String id) {
        return new LocalModelDescriptor(id, LocalModelDescriptor.plainName(id),
                LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true);
    }

    private static final class CountingDiscovery implements ProviderModelCache.DiscoveryFunction {
        final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
        final Map<String, List<LocalModelDescriptor>> results = new ConcurrentHashMap<>();
        final Set<String> failing = ConcurrentHashMap.newKeySet();

        CountingDiscovery() {
            for (String k : KEYS) {
                calls.put(k, new AtomicInteger());
                results.put(k, List.of(model(k + ":m1"), model(k + ":m2")));
            }
        }

        @Override
        public List<LocalModelDescriptor> discover(String providerKey, Orchestrator orchestrator)
                throws Exception {
            calls.get(providerKey).incrementAndGet();
            if (failing.contains(providerKey)) {
                throw new IllegalStateException("boom: " + providerKey);
            }
            return results.get(providerKey);
        }
    }

    @Test
    public void initializationDiscoversEveryProviderExactlyOnce() throws Exception {
        CountingDiscovery discovery = new CountingDiscovery();
        ProviderModelCache cache = new ProviderModelCache(KEYS, discovery);
        cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        assertTrue(cache.isInitialized());
        for (String key : KEYS) {
            assertEquals(1, discovery.calls.get(key).get());
            assertEquals(2, cache.models(key).size());
            assertFalse(cache.isFailed(key));
        }
    }

    @Test
    public void repeatedInitializationReturnsExistingResultWithoutRediscovery() throws Exception {
        CountingDiscovery discovery = new CountingDiscovery();
        ProviderModelCache cache = new ProviderModelCache(KEYS, discovery);
        cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        for (int i = 0; i < 5; i++) {
            cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        }
        for (String key : KEYS) {
            assertEquals(1, discovery.calls.get(key).get());
        }
    }

    @Test
    public void concurrentInitializationDoesNotDuplicateDiscovery() throws Exception {
        CountingDiscovery discovery = new CountingDiscovery();
        ProviderModelCache cache = new ProviderModelCache(KEYS, discovery);
        int threadCount = 16;
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            workers.add(new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException ignored) {
                    // continue
                }
                cache.ensureInitialized(null);
            }));
        }
        workers.forEach(Thread::start);
        start.countDown();
        for (Thread t : workers) {
            t.join();
        }
        cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        for (String key : KEYS) {
            assertEquals("Provider " + key + " discovered more than once",
                    1, discovery.calls.get(key).get());
        }
    }

    @Test
    public void singleProviderFailureDoesNotBlockOthers() throws Exception {
        CountingDiscovery discovery = new CountingDiscovery();
        discovery.failing.add("enr");
        ProviderModelCache cache = new ProviderModelCache(KEYS, discovery);
        cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        assertFalse(cache.isFailed("ollama"));
        assertFalse(cache.isFailed("llama-cpp"));
        assertTrue(cache.isFailed("enr"));
        assertEquals(2, cache.models("ollama").size());
        assertEquals(2, cache.models("llama-cpp").size());
        // A failure is never converted into a successful empty result: the provider is
        // marked failed and exposes no models.
        assertTrue(cache.models("enr").isEmpty());
    }

    @Test
    public void failedRefreshPreservesPreviousValidList() throws Exception {
        CountingDiscovery discovery = new CountingDiscovery();
        ProviderModelCache cache = new ProviderModelCache(KEYS, discovery);
        cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        assertEquals(2, cache.models("ollama").size());

        discovery.failing.add("ollama");
        cache.refresh(null).get(10, TimeUnit.SECONDS);

        assertTrue(cache.isFailed("ollama"));
        assertEquals("Previous valid list must be preserved after a failed refresh",
                2, cache.models("ollama").size());
        assertEquals(2, cache.models("enr").size());
        assertEquals(2, cache.models("llama-cpp").size());
    }

    @Test
    public void explicitRefreshInvokesDiscoveryAgain() throws Exception {
        CountingDiscovery discovery = new CountingDiscovery();
        ProviderModelCache cache = new ProviderModelCache(KEYS, discovery);
        cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        cache.refresh(null).get(10, TimeUnit.SECONDS);
        for (String key : KEYS) {
            assertEquals(2, discovery.calls.get(key).get());
        }
    }

    @Test
    public void modelDescriptorsRetainOriginalInferenceIds() throws Exception {
        CountingDiscovery discovery = new CountingDiscovery();
        discovery.results.put("enr", List.of(
                new LocalModelDescriptor("ollama:llama3.2:3b", "llama3.2:3b",
                        LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true),
                new LocalModelDescriptor("evo-native:x.evo", "x.evo",
                        LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", false)));
        ProviderModelCache cache = new ProviderModelCache(List.of("enr"), discovery);
        cache.ensureInitialized(null).get(10, TimeUnit.SECONDS);
        List<LocalModelDescriptor> models = cache.models("enr");
        assertEquals(2, models.size());
        assertEquals("ollama:llama3.2:3b", models.get(0).id());
        assertEquals("evo-native:x.evo", models.get(1).id());
        assertEquals("x.evo", models.get(1).name());
    }

    @Test
    public void providerKeysAreNormalizedConsistently() {
        assertEquals(List.of("ollama", "llama-cpp", "enr"), LlmRouter.PROVIDER_KEYS);
        assertEquals("ollama", LlmRouter.normalizeProviderKey("Ollama"));
        assertEquals("llama-cpp", LlmRouter.normalizeProviderKey("llama.cpp"));
        assertEquals("enr", LlmRouter.normalizeProviderKey("EVO Native ENR"));
        assertEquals("enr", LlmRouter.normalizeProviderKey("enr"));
    }
}
