package eu.kalafatic.evolution.controller.kernel;

import static org.junit.Assert.*;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.workflow.*;
import eu.kalafatic.evolution.controller.execution.BackpressureController;

/**
 * Verifies that runtime invariants are strictly enforced.
 */
public class InvariantEnforcementTest {

    @Test
    public void testSessionBoundaryEnforcement() {
        String sessionId = "test-session-1";
        SessionBoundaryGuard.enterSession(sessionId);
        try {
            assertEquals(sessionId, SessionBoundaryGuard.getCurrentSessionId());
            assertTrue(SessionBoundaryGuard.isInSession());

            // Should pass: matching session
            RuntimeInvariant.checkSession(sessionId, "Test");

            // Should fail: mismatching session
            try {
                RuntimeInvariant.checkSession("wrong-session", "Test");
                fail("Should have thrown InvariantViolationException for session mismatch");
            } catch (InvariantViolationException e) {
                assertTrue(e.getMessage().contains("Cross-session access detected"));
            }
        } finally {
            SessionBoundaryGuard.exitSession();
        }

        assertFalse(SessionBoundaryGuard.isInSession());
        assertNull(SessionBoundaryGuard.getCurrentSessionId());
    }

    @Test
    public void testGlobalAccessEnforcement() {
        String sessionId = "test-session-2";

        // Access without active session should NOT throw (for backward compatibility where needed)
        BackpressureController.getInstance();

        SessionBoundaryGuard.enterSession(sessionId);
        try {
            // Access WITH active session should throw violation
            try {
                BackpressureController.getInstance();
                fail("Should have thrown InvariantViolationException for global singleton access within session");
            } catch (InvariantViolationException e) {
                assertTrue(e.getMessage().contains("Hidden singleton access detected"));
            }
        } finally {
            SessionBoundaryGuard.exitSession();
        }
    }

    @Test
    public void testCrossSessionEventEnforcement() {
        String sessionA = "session-A";
        String sessionB = "session-B";

        RuntimeEventBus busA = new RuntimeEventBus(sessionA);

        SessionBoundaryGuard.enterSession(sessionA);
        try {
            // Should pass
            busA.publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionA, "Test", "Payload"));

            // Should fail: publishing session B event on bus A while thread is in session A
            // Actually, RuntimeEventBus.publish checks if event.sessionId matches bus.sessionId first.
            // But RuntimeInvariant.checkSession(event.getSessionId()) will also catch it if we are in session A.

            try {
                busA.publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionB, "Test", "Payload"));
                fail("Should have thrown exception for cross-session event");
            } catch (Exception e) {
                // RuntimeEventBus or RuntimeInvariant will throw
            }
        } finally {
            SessionBoundaryGuard.exitSession();
        }
    }
}
