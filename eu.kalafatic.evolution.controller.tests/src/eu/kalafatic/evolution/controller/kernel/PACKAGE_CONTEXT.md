# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller.tests/src/eu/kalafatic/evolution/controller/kernel/

## Domain: general

## Components
* `InvariantEnforcementTest.java`: package eu.kalafatic.evolution.controller.kernel; import static org.junit.Assert.*; import org.junit.Test; import eu.kalafatic.evolution.controller.orchestration.*; import eu.kalafatic.evolution.controller.workflow.*; import eu.kalafatic.evolution.controller.execution.BackpressureController; public class InvariantEnforcementTest { @Test public void testSessionBoundaryEnforcement() { String sessionId = "test-session-1"; SessionBoundaryGuard.enterSession(sessionId); try { assertEquals(sessionId, SessionBoundaryGuard.getCurrentSessionId()); assertTrue(SessionBoundaryGuard.isInSession()); RuntimeInvariant.checkSession(sessionId, "Test"); try { RuntimeInvariant.checkSession("wrong-session", "Test"); fail("Should have thrown InvariantViolationException for session mismatch"); } catch (InvariantViolationException e) { assertTrue(e.getMessage().contains("Cross-session access detected"));
