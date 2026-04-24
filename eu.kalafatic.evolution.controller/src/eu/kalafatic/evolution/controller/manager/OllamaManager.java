package eu.kalafatic.evolution.controller.manager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Singleton manager for Ollama services.
 * Centralizes service instances and provides caching to prevent multiple
 * instantiations of the same service endpoint.
 *
 * @evo:1:1 reason=centralize-ollama-management
 */
public class OllamaManager {

    private static final OllamaManager INSTANCE = new OllamaManager();
    private final Map<String, OllamaService> services = new ConcurrentHashMap<>();

    private OllamaManager() {}

    public static OllamaManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets or creates an OllamaService for the given URL.
     * @param url The Ollama server URL.
     * @return The OllamaService instance.
     */
    public OllamaService getService(String url) {
        String key = (url != null && !url.isEmpty()) ? url : "http://localhost:11434";
        // Normalize: remove trailing slash for consistent cache keys
        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }
        return services.computeIfAbsent(key, k -> new OllamaService(k, null));
    }

    /**
     * Removes a service from the cache, forcing a recreate on next access.
     * @param url The URL of the service to remove.
     */
    public void removeService(String url) {
        if (url != null) {
            services.remove(url);
        }
    }

    /**
     * Clears all cached services.
     */
    public void clear() {
        services.clear();
    }
}
