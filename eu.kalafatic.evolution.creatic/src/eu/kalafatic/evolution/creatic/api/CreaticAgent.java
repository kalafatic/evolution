package eu.kalafatic.evolution.creatic.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import eu.kalafatic.evolution.creatic.model.ContextGraph;
import eu.kalafatic.evolution.creatic.model.GuidanceResponse;
import eu.kalafatic.evolution.creatic.engine.GuidanceEngine;

public class CreaticAgent {
    private static final CreaticAgent INSTANCE = new CreaticAgent();

    private final ContextCollector collector = new ContextCollector();
    private final GuidanceEngine engine = new GuidanceEngine();
    private final Map<String, GuidanceResponse> cache = new ConcurrentHashMap<>();
    private boolean enabled = true;

    private CreaticAgent() {}

    public static CreaticAgent getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GuidanceResponse analyze(String pageId) {
        if (!enabled) return null;

        try {
            ContextGraph context = collector.collect(pageId);
            GuidanceResponse response = engine.evaluate(context);
            cache.put(pageId, response);
            return response;
        } catch (Exception e) {
            System.err.println("Creatic Agent failure: " + e.getMessage());
            return cache.getOrDefault(pageId, new GuidanceResponse());
        }
    }
}
