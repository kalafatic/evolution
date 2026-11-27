package eu.kalafatic.evolution.controller.orchestration.llm;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Ollama availability/startup lifecycle. Availability is a live {@code /api/tags} ping of
 * the configured endpoint; startup launches the {@code ollama serve} process (never a
 * hardcoded endpoint); health is verified by repeated pings; model discovery delegates to
 * the existing {@link LlmRouter} Ollama discovery.
 */
public class OllamaProviderLifecycle implements ProviderLifecycle {

    public static final String KEY = "ollama";
    public static final String SYS_OLLAMA_URL = "evo.ollama.url";
    public static final String DEFAULT_URL = "http://localhost:11434";

    private volatile Process activeProcess;

    @Override
    public String providerKey() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Ollama";
    }

    @Override
    public boolean isAvailable(Orchestrator orchestrator) {
        return service(orchestrator).ping();
    }

    @Override
    public CompletableFuture<Void> start(Orchestrator orchestrator, Consumer<String> progress) {
        return CompletableFuture.runAsync(() -> {
            String endpoint = resolveEndpoint(orchestrator);
            // 1. Already running on the default endpoint while the configured endpoint is
            //    unreachable? The endpoint configuration is wrong — never launch a
            //    duplicate Ollama process on top of an already-running one.
            if (!DEFAULT_URL.equals(endpoint)
                    && OllamaManager.getInstance().getService(DEFAULT_URL).ping()) {
                throw new IllegalStateException(
                        "Ollama is already running on " + DEFAULT_URL
                                + " but the configured endpoint (" + endpoint
                                + ") is unreachable. Verify the endpoint configuration.");
            }
            // 2. Already started by EVO? (tracked ownership; never relaunch)
            Process existing = activeProcess;
            if (existing != null && existing.isAlive()) {
                return;
            }
            // 3. Executable present?
            if (!ollamaExecutableAvailable()) {
                throw new IllegalStateException(
                        "Ollama executable not found. Install Ollama or start it manually.");
            }
            // 4. Launch (EVO now owns this process).
            if (progress != null) {
                progress.accept("Starting Ollama service...");
            }
            try {
                Process process = new ProcessBuilder("ollama", "serve")
                        .redirectErrorStream(true)
                        .start();
                activeProcess = process;
            } catch (IOException e) {
                throw new IllegalStateException("Ollama startup failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Whether the {@code ollama} executable is present on the system PATH. This
     * distinguishes "executable missing" from a runtime startup failure (the process
     * launches but exits, detected by {@link #waitHealthy}).
     */
    private static boolean ollamaExecutableAvailable() {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            if (new java.io.File(dir, "ollama").isFile()
                    || new java.io.File(dir, "ollama.exe").isFile()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CompletableFuture<Boolean> waitHealthy(Orchestrator orchestrator, long timeoutMillis) {
        return CompletableFuture.supplyAsync(() -> {
            long deadline = System.currentTimeMillis() + Math.max(0, timeoutMillis);
            while (System.currentTimeMillis() < deadline) {
                if (isAvailable(orchestrator)) {
                    return true;
                }
                Process process = activeProcess;
                if (process != null && !process.isAlive()) {
                    // The launched process exited before becoming healthy.
                    return false;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return isAvailable(orchestrator);
        });
    }

    @Override
    public List<LocalModelDescriptor> discoverModels(Orchestrator orchestrator) throws Exception {
        return LlmRouter.getInstance().discoverModels(KEY, orchestrator);
    }

    private OllamaService service(Orchestrator orchestrator) {
        return OllamaManager.getInstance().getService(resolveEndpoint(orchestrator));
    }

    /** Resolves the configured Ollama endpoint (never a hardcoded value when one exists). */
    public static String resolveEndpoint(Orchestrator orchestrator) {
        if (orchestrator != null && orchestrator.getOllama() != null
                && orchestrator.getOllama().getUrl() != null
                && !orchestrator.getOllama().getUrl().isEmpty()) {
            return orchestrator.getOllama().getUrl();
        }
        String property = System.getProperty(SYS_OLLAMA_URL);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        return DEFAULT_URL;
    }
}
