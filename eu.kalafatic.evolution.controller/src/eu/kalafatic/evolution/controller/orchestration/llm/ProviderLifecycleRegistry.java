package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Registry + availability controller for local LLM providers. Provider bundles (ENR)
 * and the controller (Ollama) register their {@link ProviderLifecycle} here; the AI
 * Commander UI resolves a lifecycle by normalized provider key and asks it to
 * {@link #ensureAvailable(String, Orchestrator, Consumer)} before sending a message.
 *
 * <p>Concurrency: a single shared in-flight startup future per provider guarantees that
 * concurrent requests start the provider exactly once and all attach to the same
 * startup; {@link #ensureAvailable} never blocks the caller thread.</p>
 */
public final class ProviderLifecycleRegistry {

    /** Default health-wait window after a provider is started. */
    public static final long DEFAULT_HEALTH_TIMEOUT_MS = 60_000L;

    private static final ProviderLifecycleRegistry INSTANCE = new ProviderLifecycleRegistry();

    private final Map<String, ProviderLifecycle> lifecycles = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> startups = new ConcurrentHashMap<>();

    private ProviderLifecycleRegistry() {
        // Ollama ships with the controller bundle: register its lifecycle eagerly.
        register(new OllamaProviderLifecycle());
    }

    public static ProviderLifecycleRegistry getInstance() {
        return INSTANCE;
    }

    public void register(ProviderLifecycle lifecycle) {
        if (lifecycle == null || lifecycle.providerKey() == null
                || lifecycle.providerKey().isBlank()) {
            return;
        }
        lifecycles.put(lifecycle.providerKey(), lifecycle);
    }

    public ProviderLifecycle forKey(String providerKey) {
        if (providerKey == null) {
            return null;
        }
        String normalized = LlmRouter.normalizeProviderKey(providerKey);
        return normalized.isEmpty() ? lifecycles.get(providerKey) : lifecycles.get(normalized);
    }

    /**
     * Ensures the provider for {@code providerKey} is available, starting it (once) and
     * waiting for health if necessary. Returns the lifecycle after it is available.
     */
    public CompletableFuture<ProviderLifecycle> ensureAvailable(String providerKey,
            Orchestrator orchestrator, Consumer<String> progress) {
        ProviderLifecycle lifecycle = forKey(providerKey);
        if (lifecycle == null) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "No provider lifecycle registered for: " + providerKey));
        }
        if (lifecycle.isAvailable(orchestrator)) {
            return CompletableFuture.completedFuture(lifecycle);
        }
        return ensureStarted(lifecycle, orchestrator, progress).thenApply(v -> lifecycle);
    }

    private CompletableFuture<Void> ensureStarted(ProviderLifecycle lifecycle,
            Orchestrator orchestrator, Consumer<String> progress) {
        // computeIfAbsent guarantees the mapping function (and therefore start()) runs
        // at most once for concurrent callers.
        CompletableFuture<Void> startup = startups.computeIfAbsent(
                lifecycle.providerKey(),
                key -> lifecycle.start(orchestrator, progress)
                        .thenCompose(v -> lifecycle.waitHealthy(orchestrator,
                                DEFAULT_HEALTH_TIMEOUT_MS))
                        .thenAccept(healthy -> {
                            if (healthy == null || !healthy) {
                                throw new IllegalStateException(lifecycle.displayName()
                                        + " did not become healthy within the timeout");
                            }
                        }));
        startup.whenComplete((v, t) -> startups.remove(lifecycle.providerKey(), startup));
        return startup;
    }

    /** Exposed for tests/diagnostics: whether a startup is currently in flight. */
    public boolean isStarting(String providerKey) {
        return startups.containsKey(providerKey);
    }

    /** Lists the currently registered lifecycle keys (diagnostics). */
    public List<String> registeredKeys() {
        return List.copyOf(lifecycles.keySet());
    }
}
