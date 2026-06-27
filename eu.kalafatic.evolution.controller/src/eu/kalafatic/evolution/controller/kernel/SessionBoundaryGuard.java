package eu.kalafatic.evolution.controller.kernel;

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
