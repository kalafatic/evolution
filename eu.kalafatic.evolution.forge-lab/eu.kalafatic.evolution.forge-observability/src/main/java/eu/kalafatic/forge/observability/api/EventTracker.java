package eu.kalafatic.forge.observability.api;

public interface EventTracker {
    void track(ForgeEvent event, String sessionId, Object data);
}
