package eu.kalafatic.evolution.forge.observability.api;

public interface EventTracker {
    void track(ForgeEvent event, String sessionId, Object data);
}
