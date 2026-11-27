package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Provider-specific availability/startup contract shared by ENR, Ollama and (where a
 * startup mechanism exists) llama.cpp.
 *
 * <p>Each provider supplies its own availability check, startup operation, health
 * verification and model discovery. This is the only abstraction the AI Commander UI
 * uses to decide whether a selected provider must be started before inference; it never
 * hardcodes provider behavior.</p>
 *
 * <p>Implementations must be asynchronous and must never block the caller (SWT) thread.
 * {@link #start(Orchestrator, Consumer)} must be idempotent in effect: calling it while
 * the provider is already starting/running must not launch a second process.</p>
 */
public interface ProviderLifecycle {

    /** Normalized provider key ({@code "enr"}, {@code "ollama"}, {@code "llama-cpp"}). */
    String providerKey();

    /** Human-readable provider name for dialogs ("ENR", "Ollama", …). */
    String displayName();

    /** Synchronous, non-blocking availability check (e.g. an HTTP health ping). */
    boolean isAvailable(Orchestrator orchestrator);

    /** Asynchronously starts the provider runtime; reports progress via {@code progress}. */
    CompletableFuture<Void> start(Orchestrator orchestrator, Consumer<String> progress);

    /** Asynchronously waits until the provider reports healthy (or the timeout elapses). */
    CompletableFuture<Boolean> waitHealthy(Orchestrator orchestrator, long timeoutMillis);

    /** Discovers the provider's models using its own authoritative registry/discovery. */
    List<LocalModelDescriptor> discoverModels(Orchestrator orchestrator) throws Exception;
}
