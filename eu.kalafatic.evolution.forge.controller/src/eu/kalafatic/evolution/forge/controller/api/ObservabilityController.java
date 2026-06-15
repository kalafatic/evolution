package eu.kalafatic.evolution.forge.controller.api;

public interface ObservabilityController {
    void trackEvent(String sessionId, String eventType, String data);
    void triggerEvolution(String sessionId, String modelId);
}
