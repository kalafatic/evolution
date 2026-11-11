package eu.kalafatic.evolution.controller.orchestration;

import java.util.List;

/**
 * Listener interface for EVO server lifecycle and web application state transitions.
 */
public interface ServerLifecycleListener {

    /**
     * Called whenever a server's runtime state changes (e.g. STARTING, RUNNING, STOPPED, FAILED).
     */
    void onServerStateChanged(ServerEntry serverEntry);

    /**
     * Called after auto-start completes across all configured servers.
     */
    default void onAllServersStarted(List<ServerEntry> servers) {}

    /**
     * Called when a web application becomes ready and accessible in browser.
     */
    default void onWebApplicationReady(WebApplicationDescriptor webApp) {}

    /**
     * Called when a web application becomes unavailable (e.g. server stopped).
     */
    default void onWebApplicationUnavailable(WebApplicationDescriptor webApp) {}
}
