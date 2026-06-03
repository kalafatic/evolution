package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;

/**
 * Authoritative engine for enforcing runtime invariants across the EVO kernel.
 */
public class RuntimeInvariant {

    /**
     * Ensures the provided session ID matches the active session context of the current thread.
     */
    public static void checkSession(String sessionId, String source) {
        String activeSession = SessionBoundaryGuard.getCurrentSessionId();
        if (activeSession != null && !activeSession.equals(sessionId)) {
            reportViolation(String.format("[%s] Cross-session access detected. Current thread is bound to session %s, but tried to access session %s.",
                source, activeSession, sessionId), activeSession);
        }
    }

    /**
     * Enforces that no global state (singletons) is accessed when a session context is active.
     */
    public static void checkNoGlobalAccess(String source) {
        if (SessionBoundaryGuard.isInSession()) {
             // In a perfect world, this would always throw.
             // For now, we report it to track drift while we decommission legacy paths.
             reportViolation(String.format("[%s] Hidden singleton access detected within active session context %s.",
                source, SessionBoundaryGuard.getCurrentSessionId()), SessionBoundaryGuard.getCurrentSessionId());
        }
    }

    /**
     * Fail-fast reporting for invariant violations.
     */
    private static void reportViolation(String message, String sessionId) {
        System.err.println("[INVARIANT VIOLATION] " + message);

        // Attempt to publish a policy violation event if possible
        try {
            SessionContainer session = SessionManager.getInstance().getSession(sessionId);
            if (session != null) {
                RuntimeEventBus bus = session.getEventBus();
                if (bus != null) {
                    bus.publish(new RuntimeEvent(
                        RuntimeEventType.POLICY_VIOLATION_DETECTED,
                        sessionId,
                        "InvariantEnforcer",
                        message));
                }
            }
        } catch (Exception e) {
            // Sink event errors to avoid recursive loops during violation reporting
        }

        throw new InvariantViolationException(message);
    }
}
