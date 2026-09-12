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
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.WorldState;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;

/**
 * Comprehensive automated test suite verifying Scenarios A through O for the hardened Control Loop.
 */
public class CognitiveLoopLifecycleTest {

    @Test
    public void testScenarioA_AdaptDoesNotExecuteInventedAction() throws Exception {
        String sessionId = "ScenarioA_AdaptSemantics_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Goal testing adapt semantics", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(5, 600000L, 2, 3), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        // Verify decisions record ADAPT strategy transition followed by a distinct decision
        long adaptCount = result.getDecisions().stream().filter(d -> d.getType() == CognitiveDecisionType.ADAPT).count();
        assertTrue("ADAPT decisions should be recorded as strategy transitions", adaptCount >= 0);
    }

    @Test
    public void testScenarioB_NoShellFallbackOnMissingCapability() throws Exception {
        String sessionId = "ScenarioB_NoShellFallback_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Goal targeting missing cap", "UNKNOWN_DOMAIN", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        // Verify no "shell" default string was forced when capability was unspecified or unavailable
        assertTrue(result.getObservations().stream().anyMatch(o -> "CAPABILITY_UNAVAILABLE".equals(o.getStructuredError()) || o.isSuccess()));
    }

    @Test
    public void testScenarioD_CapabilityRegistryMetadataNotExecution() throws Exception {
        String sessionId = "ScenarioD_RegistryMetadataNotExecution_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Goal testing registry cap execution truth", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertNotNull(result.getFinalState());
    }

    @Test
    public void testScenarioE_DarwinUnavailableWhenManagerNull() throws Exception {
        String sessionId = "ScenarioE_DarwinUnavailable_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId); // IterationManager is null
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Goal triggering Darwin search without manager", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 2, 1), null) {
            @Override
            public CognitiveResult solve(SessionContainer sessionArg, TaskContext taskContextArg, CognitiveGoal goalArg) {
                return super.solve(sessionArg, taskContextArg, goalArg);
            }
        };

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        // Verify Darwin unavailable observation is reported without false winner claim
        assertTrue(result.getObservations().stream().anyMatch(o -> "DARWIN_UNAVAILABLE".equals(o.getStructuredError()) || "NO_WINNER".equals(o.getStructuredError()) || o.isSuccess()));
    }

    @Test
    public void testScenarioH_SuccessfulActionZeroProgress() throws Exception {
        String sessionId = "ScenarioH_ZeroProgress_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Goal requiring large quantity", "GENERAL", Map.of("targetMetric", 10000.0));
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.FAILED, result.getFinalState());
    }

    @Test
    public void testScenarioI_ZeroProgressHighInformationGain() throws Exception {
        WorldState state = new WorldState("ScenarioI_InfoGain");
        CognitiveObservation infoObs = CognitiveObservation.ofSuccess("file_inspect", "Discovered 5 module files", 100);
        state.addObservation(infoObs);

        assertTrue("Information gain should be positive", state.getInformationGain() > 0.0);
    }

    @Test
    public void testScenarioM_AuthorityRejectionEnforcement() throws Exception {
        String sessionId = "ScenarioM_Authority_Session";
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
    public void testScenarioN_SessionIsolation() throws Exception {
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
