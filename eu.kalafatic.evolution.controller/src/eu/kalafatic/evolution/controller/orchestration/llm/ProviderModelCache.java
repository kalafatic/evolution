package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Shared provider-model cache: a deterministic, idempotent cache of the models
 * discovered through the existing LLM provider discovery mechanism, keyed by the
 * normalized provider identifier.
 *
 * <p>The cache is initialized lazily and exactly once: every provider is discovered a
 * single time during initialization, the results are stored in
 * {@code Map<String, List<LocalModelDescriptor>>}, and all consumers reuse that data.
 * A normal UI refresh, page recreation, provider switch or session change never
 * re-triggers discovery — only an explicit {@link #refresh(Orchestrator)} does.</p>
 *
 * <p>Concurrency: initialization is guarded by a double-checked shared future, so
 * concurrent callers never trigger duplicate discovery. Discovery runs on a dedicated
 * daemon executor, never on a caller (UI) thread. A failure for one provider never
 * discards another provider's result, and a failed refresh preserves the previous
 * valid list for that provider.</p>
 *
 * <p>This class performs no logging and no UI work: it is a pure, unit-testable cache
 * layer over the existing discovery function, so it introduces no parallel discovery
 * subsystem.</p>
 */
public final class ProviderModelCache {

    /**
     * Discovery function: resolves the model list for a single normalized provider key.
     * Implementations must throw (rather than return an empty list) when discovery
     * fails, so a failure is never silently converted into a successful empty result.
     */
    @FunctionalInterface
    public interface DiscoveryFunction {
        List<LocalModelDescriptor> discover(String providerKey, Orchestrator orchestrator)
                throws Exception;
    }

    private static final Executor DISCOVERY_EXECUTOR = Executors.newCachedThreadPool(
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "evo-provider-model-cache-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });

    private final List<String> providerKeys;
    private final DiscoveryFunction discovery;
    private final Executor executor;
    private final Consumer<String> logger;

    private final Object lock = new Object();
    private final Map<String, List<LocalModelDescriptor>> providerModels =
            new ConcurrentHashMap<>();
    private final Set<String> failedProviders = ConcurrentHashMap.newKeySet();
    private volatile CompletableFuture<Void> initFuture;
    private volatile boolean refreshing;

    public ProviderModelCache(List<String> providerKeys, DiscoveryFunction discovery) {
        this(providerKeys, discovery, DISCOVERY_EXECUTOR, null);
    }

    public ProviderModelCache(List<String> providerKeys, DiscoveryFunction discovery,
            Executor executor) {
        this(providerKeys, discovery, executor, null);
    }

    public ProviderModelCache(List<String> providerKeys, DiscoveryFunction discovery,
            Executor executor, Consumer<String> logger) {
        this.providerKeys = List.copyOf(providerKeys);
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.executor = executor == null ? DISCOVERY_EXECUTOR : executor;
        this.logger = logger;
    }

    /** The normalized provider keys this cache is scoped to (immutable). */
    public List<String> providerKeys() {
        return providerKeys;
    }

    /** Immutable snapshot of the cached model lists per provider. */
    public Map<String, List<LocalModelDescriptor>> snapshot() {
        Map<String, List<LocalModelDescriptor>> copy = new LinkedHashMap<>();
        providerModels.forEach((key, models) -> copy.put(key, List.copyOf(models)));
        return Map.copyOf(copy);
    }

    /** The cached model list for a provider key (empty when unknown/not yet loaded). */
    public List<LocalModelDescriptor> models(String providerKey) {
        if (providerKey == null) {
            return List.of();
        }
        List<LocalModelDescriptor> models = providerModels.get(providerKey);
        return models == null ? List.of() : models;
    }

    /** Whether the most recent discovery for this provider key failed. */
    public boolean isFailed(String providerKey) {
        return providerKey != null && failedProviders.contains(providerKey);
    }

    /** Whether the cache has completed its initial discovery run. */
    public boolean isInitialized() {
        CompletableFuture<Void> f = initFuture;
        return f != null && f.isDone() && !f.isCompletedExceptionally() && !f.isCancelled();
    }

    /** Whether a discovery/refresh run is currently in progress. */
    public boolean isRefreshing() {
        return refreshing;
    }

    /**
     * Idempotently initializes the cache: the first caller triggers a single discovery
     * run for every provider; every subsequent caller reuses the same shared future and
     * never triggers additional discovery.
     */
    public CompletableFuture<Void> ensureInitialized(Orchestrator orchestrator) {
        CompletableFuture<Void> f = initFuture;
        if (f != null) {
            return f;
        }
        synchronized (lock) {
            if (initFuture == null) {
                initFuture = discoverAll(orchestrator);
            }
            return initFuture;
        }
    }

    /**
     * Explicitly re-discovers every provider and updates the cache atomically per
     * provider. On a provider failure the previous valid list is preserved.
     */
    public CompletableFuture<Void> refresh(Orchestrator orchestrator) {
        synchronized (lock) {
            CompletableFuture<Void> f = discoverAll(orchestrator);
            initFuture = f;
            return f;
        }
    }

    /** Clears all cached state (test seam / hard reset). */
    public void invalidate() {
        synchronized (lock) {
            initFuture = null;
            providerModels.clear();
            failedProviders.clear();
            refreshing = false;
        }
    }

    private CompletableFuture<Void> discoverAll(Orchestrator orchestrator) {
        log("cache initialization started for providers " + providerKeys);
        return CompletableFuture.runAsync(() -> {
            refreshing = true;
            try {
                for (String key : providerKeys) {
                    discoverOne(key, orchestrator);
                }
            } finally {
                refreshing = false;
            }
        }, executor).whenComplete((v, t) -> log("cache initialization completed"));
    }

    private void discoverOne(String key, Orchestrator orchestrator) {
        try {
            log("discovery started for provider " + key);
            List<LocalModelDescriptor> discovered = discovery.discover(key, orchestrator);
            List<LocalModelDescriptor> copy = immutableCopy(discovered);
            providerModels.put(key, copy);
            failedProviders.remove(key);
            log("discovery completed for provider " + key + ": " + copy.size() + " models");
        } catch (Throwable t) {
            // A failure for one provider never discards the others, and never
            // overwrites a previously valid list: keep the old entry and record the
            // failure explicitly so consumers can distinguish "failed" from "empty".
            failedProviders.add(key);
            log("discovery FAILED for provider " + key + ": "
                    + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
        }
    }

    private void log(String message) {
        if (logger != null) {
            try {
                logger.accept(message);
            } catch (Throwable ignored) {
                // logging must never affect cache correctness
            }
        }
    }

    private static List<LocalModelDescriptor> immutableCopy(List<LocalModelDescriptor> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        List<LocalModelDescriptor> copy = new ArrayList<>(models.size());
        for (LocalModelDescriptor model : models) {
            if (model != null) {
                copy.add(model);
            }
        }
        return List.copyOf(copy);
    }
}
