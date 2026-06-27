package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Tracks the active session ID for the current thread to enforce session isolation.
 */
public class SessionBoundaryGuard {
    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

    public static void enterSession(String sessionId) {
        currentSessionId.set(sessionId);
    }

    public static void exitSession() {
        currentSessionId.remove();
    }

    public static String getCurrentSessionId() {
        return currentSessionId.get();
    }

    public static boolean isInSession() {
        return currentSessionId.get() != null;
    }
}
