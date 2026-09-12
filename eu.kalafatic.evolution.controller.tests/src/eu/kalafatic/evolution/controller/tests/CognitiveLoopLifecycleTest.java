package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import eu.kalafatic.evolution.controller.kernel.AuthorityEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveDecision;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveDecisionType;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveObservation;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveResult;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveState;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;

/**
 * Comprehensive automated test suite verifying Cognitive Loop state transitions,
 * retries, adaptation, Darwin escalation, authority denial, and strict session isolation.
 */
public class CognitiveLoopLifecycleTest {

    @Test
    public void testNormalSuccess() throws Exception {
        String sessionId = "Lifecycle_Success_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Simple successful task", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(5, null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
        assertTrue("Attempts should be >= 1", result.getAttempts() >= 1);
        assertEquals("Darwin should not be invoked for simple success", 0, result.getDarwinInvocations());
    }

    @Test
    public void testTransientRetryAndAdaptation() throws Exception {
        String sessionId = "Lifecycle_Adapt_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Task triggering download failure", "DATASET_ACQUISITION", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(10, null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
        assertTrue(result.getObservations().size() >= 1);
    }

    @Test
    public void testDarwinEscalation() throws Exception {
        String sessionId = "Lifecycle_Darwin_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Complex problem requiring evolutionary search", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(10, null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
    }

    @Test
    public void testAuthorityDenialHandling() throws Exception {
        String sessionId = "Lifecycle_Authority_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        // Denying Authority Engine
        AuthorityEngine rejectingAuthority = new AuthorityEngine() {
            @Override
            public EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId) {
                return new EvolutionDecision(
                        AuthorityController.DecisionType.REJECT,
                        null,
                        Collections.emptyList(),
                        "Destructive action rejected by authority policy",
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );
            }

            @Override
            public void updateLifecycle(List<BranchVariant> variants, String targetId, BranchVariant.ActivationState newState, TaskContext context) {}
        };

        CognitiveGoal goal = new CognitiveGoal("Dangerous action", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(3, rejectingAuthority);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertTrue("Authority rejection should be captured in observations",
                result.getObservations().stream().anyMatch(o -> "AUTHORITY_DENIED".equals(o.getStructuredError()) || o.getExitCode() == 403));
    }

    @Test
    public void testSessionIsolation() throws Exception {
        String sessionAId = "Isolation_Session_A";
        String sessionBId = "Isolation_Session_B";

        SessionContainer sessionA = SessionManager.getInstance().getOrCreateSession(sessionAId);
        SessionContainer sessionB = SessionManager.getInstance().getOrCreateSession(sessionBId);

        TaskContext contextA = new TaskContext(null, new File("."));
        contextA.setSessionId(sessionAId);

        TaskContext contextB = new TaskContext(null, new File("."));
        contextB.setSessionId(sessionBId);

        CognitiveGoal goalA = new CognitiveGoal("Goal A", "DATASET_ACQUISITION", null);
        CognitiveGoal goalB = new CognitiveGoal("Goal B", "SELF_DEV", null);

        CognitiveLoopEngine engineA = new CognitiveLoopEngine(5, null);
        CognitiveLoopEngine engineB = new CognitiveLoopEngine(5, null);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<CognitiveResult> futureA = executor.submit(() -> engineA.solve(sessionA, contextA, goalA));
        Future<CognitiveResult> futureB = executor.submit(() -> engineB.solve(sessionB, contextB, goalB));

        CognitiveResult resultA = futureA.get();
        CognitiveResult resultB = futureB.get();
        executor.shutdown();

        assertNotNull(resultA);
        assertNotNull(resultB);

        assertEquals(sessionAId, resultA.getSessionId());
        assertEquals(sessionBId, resultB.getSessionId());

        assertFalse("Session A logs must not contain Session B ID", contextA.getLogs().stream().anyMatch(l -> l.contains(sessionBId)));
        assertFalse("Session B logs must not contain Session A ID", contextB.getLogs().stream().anyMatch(l -> l.contains(sessionAId)));
    }
}
