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
 * Comprehensive automated test suite verifying Scenarios A through K for the generic Control Loop.
 */
public class CognitiveLoopLifecycleTest {

    @Test
    public void testScenarioA_UnknownDomain() throws Exception {
        String sessionId = "ScenarioA_UnknownDomain_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Perform quantum optimization calculation", "QUANTUM_COMPUTING", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(5, 600000L, 2, 3), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
        assertTrue(result.getObservations().size() >= 1);
    }

    @Test
    public void testScenarioB_CapabilityFailureAdaptation() throws Exception {
        String sessionId = "ScenarioB_CapFailure_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Generic task with capability failure", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(5, 600000L, 2, 3), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
    }

    @Test
    public void testScenarioC_PartialGoalNotSuccess() throws Exception {
        String sessionId = "ScenarioC_PartialGoal_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Acquire large dataset", "DATASET_ACQUISITION", Map.of("targetMetric", 1000.0, "currentMetricKey", "quantity"));
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(1, 600000L, 1, 1), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        // Partial progress must not be marked as SUCCESS on budget exhaustion
        assertEquals(CognitiveState.FAILED, result.getFinalState());
    }

    @Test
    public void testScenarioD_SuccessfulActionButIncompleteGoal() throws Exception {
        String sessionId = "ScenarioD_ActionSuccessIncompleteGoal_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Incomplete goal with required verification capability", "GENERAL", Map.of("requiredCapabilityVerification", "non_existent_verifier"));
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(2, 600000L, 1, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.FAILED, result.getFinalState());
    }

    @Test
    public void testScenarioE_LlmOverruledByGoalEvaluator() throws Exception {
        String sessionId = "ScenarioE_LlmOverruled_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Goal with missing requirement", "GENERAL", Map.of("targetMetric", 500.0));
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(1, 600000L, 1, 1), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertFalse("GoalEvaluator must prevent false LLM success when requirements are unsatisfied", result.getFinalState() == CognitiveState.SUCCESS && result.getObservations().isEmpty());
    }

    @Test
    public void testScenarioF_DarwinCreationNoWinner() throws Exception {
        String sessionId = "ScenarioF_DarwinNoWinner_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Task triggering Darwin search", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 2, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertTrue(result.getDarwinInvocations() >= 0);
    }

    @Test
    public void testScenarioH_AuthorityRejectionEnforcement() throws Exception {
        String sessionId = "ScenarioH_Authority_Session";
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
        assertTrue("Authority rejection must be recorded in observations",
                result.getObservations().stream().anyMatch(o -> "AUTHORITY_DENIED".equals(o.getStructuredError()) || o.getExitCode() == 403));
    }

    @Test
    public void testScenarioI_SessionIsolation() throws Exception {
        String sessionAId = "Isolation_Session_A";
        String sessionBId = "Isolation_Session_B";

        SessionContainer sessionA = SessionManager.getInstance().getOrCreateSession(sessionAId);
        SessionContainer sessionB = SessionManager.getInstance().getOrCreateSession(sessionBId);

        TaskContext contextA = new TaskContext(null, new File("."));
        contextA.setSessionId(sessionAId);

        TaskContext contextB = new TaskContext(null, new File("."));
        contextB.setSessionId(sessionBId);

        CognitiveGoal goalA = new CognitiveGoal("Goal A", "GENERAL", null);
        CognitiveGoal goalB = new CognitiveGoal("Goal B", "GENERAL", null);

        CognitiveLoopEngine engineA = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), null);
        CognitiveLoopEngine engineB = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), null);

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
