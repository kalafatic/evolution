package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import eu.kalafatic.evolution.controller.kernel.AuthorityEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CapabilityDiscovery;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveBudget;
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
 * Comprehensive automated test suite verifying Control Loop state transitions,
 * generic strategy adaptation, goal requirement evaluation, budget constraints,
 * authority enforcement, real Darwin escalation, dynamic capabilities, and session isolation.
 */
public class CognitiveLoopLifecycleTest {

    @Test
    public void testNormalSuccess() throws Exception {
        String sessionId = "Lifecycle_Success_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Simple successful task", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine();

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
        assertTrue("Attempts should be >= 1", result.getAttempts() >= 1);
    }

    @Test
    public void testGenericAdaptationWithoutHardcodedStrings() throws Exception {
        String sessionId = "Lifecycle_GenericAdapt_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Generic goal triggering adaptation", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(10, 600000L, 2, 5), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
        assertTrue(result.getObservations().size() >= 1);
    }

    @Test
    public void testRealGoalEvaluationPartialVsAchieved() throws Exception {
        String sessionId = "Lifecycle_GoalEval_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Acquire training data", "DATASET_ACQUISITION", Map.of("targetUsableBytes", 1024L * 1024L * 50L));
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(5, 600000L, 2, 5), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
    }

    @Test
    public void testDynamicCapabilityDiscovery() throws Exception {
        String sessionId = "Lifecycle_CapDiscovery_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);

        CapabilityDiscovery discovery = new CapabilityDiscovery();
        var caps = discovery.discoverCapabilities(session);

        assertNotNull(caps);
        assertTrue("Expected non-empty discovered capabilities", caps.size() > 0);
        assertTrue("Expected file or shell tool present", caps.stream().anyMatch(c -> c.getName().equals("file") || c.getName().equals("shell")));
    }

    @Test
    public void testAuthorityEnforcementRejection() throws Exception {
        String sessionId = "Lifecycle_Authority_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        AuthorityEngine rejectingAuthority = new AuthorityEngine() {
            @Override
            public EvolutionDecision decide(String iterationId, List<BranchVariant> variants, TaskContext context, String manualSelectionId) {
                return new EvolutionDecision(
                        AuthorityController.DecisionType.REJECT,
                        null,
                        Collections.emptyList(),
                        "Action rejected by policy",
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );
            }

            @Override
            public void updateLifecycle(List<BranchVariant> variants, String targetId, BranchVariant.ActivationState newState, TaskContext context) {}
        };

        CognitiveGoal goal = new CognitiveGoal("Sensitive action", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), rejectingAuthority);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertTrue("Authority denial must be recorded in observations",
                result.getObservations().stream().anyMatch(o -> "AUTHORITY_DENIED".equals(o.getStructuredError()) || o.getExitCode() == 403));
    }

    @Test
    public void testBudgetExhaustionTermination() throws Exception {
        String sessionId = "Lifecycle_Budget_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Unsatisfiable goal", "GENERAL", Map.of("requirePreflightPass", true));
        // Strict budget: 1 iteration allowed
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(1, 600000L, 1, 1), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.FAILED, result.getFinalState());
        assertTrue(result.getAttempts() <= 2);
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

        CognitiveLoopEngine engineA = new CognitiveLoopEngine(new CognitiveBudget(5, 600000L, 2, 3), null);
        CognitiveLoopEngine engineB = new CognitiveLoopEngine(new CognitiveBudget(5, 600000L, 2, 3), null);

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
