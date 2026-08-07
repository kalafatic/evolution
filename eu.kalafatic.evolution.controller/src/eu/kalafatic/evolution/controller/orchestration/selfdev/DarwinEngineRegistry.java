package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registry for Darwin engine instances.
 * Allows UI to access the engine without modifying Orchestrator.
 */
public class DarwinEngineRegistry {
    private static final Map<String, ADarwinEngine> engines = new ConcurrentHashMap<>();
    
    public static void register(String sessionId, ADarwinEngine engine) {
        engines.put(sessionId, engine);
    }
    
    public static ADarwinEngine get(String sessionId) {
        return engines.get(sessionId);
    }
    
    public static void unregister(String sessionId) {
        engines.remove(sessionId);
    }
    
    public static boolean hasEngine(String sessionId) {
        return engines.containsKey(sessionId);
    }
}