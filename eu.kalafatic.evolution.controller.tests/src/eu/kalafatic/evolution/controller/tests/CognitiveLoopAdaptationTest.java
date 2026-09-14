package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CapabilityDiscovery;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveBudget;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveDecision;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveFailureType;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveObservation;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveResult;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveState;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveStrategy;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.GoalEvaluation;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.GoalEvaluator;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.WorldState;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.impl.discovery.HuggingFaceSourceDiscovery;

/**
 * Test suite verifying all 10 adaptive cognitive loop requirements.
 */
public class CognitiveLoopAdaptationTest {

    @Test
    public void test1_SourceExhaustion() throws Exception {
        WorldState worldState = new WorldState("Test1_Session");
        CognitiveStrategy stratA = CognitiveStrategy.acquisitionStrategy("STRAT_A", "HUGGING_FACE", "wikitext", "validation", null);
        CognitiveStrategy stratB = CognitiveStrategy.acquisitionStrategy("STRAT_B", "HUGGING_FACE", "fineweb", "train", null);

        Map<String, Object> metaA = Map.of(
                "sourceType", "HUGGING_FACE",
                "repository", "wikitext",
                "split", "validation",
                "usableContentBytes", 0L,
                "isSourceExhausted", true,
                "failureType", "SOURCE_EXHAUSTED"
        );
        CognitiveObservation obsA = new CognitiveObservation("dataset_acquisition", false, 1, "", "", "SOURCE_EXHAUSTED", 100, metaA);
        worldState.recordStrategyAttempt(stratA, obsA, 1);

        assertTrue("Strategy A must be recorded as exhausted", worldState.isStrategyExhausted(stratA));
        assertFalse("Strategy B must NOT be recorded as exhausted", worldState.isStrategyExhausted(stratB));
    }

    @Test
    public void test2_TransientRetry() throws Exception {
        WorldState worldState = new WorldState("Test2_Session");
        CognitiveStrategy stratA = CognitiveStrategy.acquisitionStrategy("STRAT_A", "HUGGING_FACE", "wikitext", "train", null);

        Map<String, Object> metaNet = Map.of("failureType", "NETWORK_FAILURE");
        CognitiveObservation obsNet1 = new CognitiveObservation("dataset_acquisition", false, 1, "", "", "NETWORK_FAILURE", 50, metaNet);
        worldState.recordStrategyAttempt(stratA, obsNet1, 1);

        assertEquals("First network failure increments retry count to 1", 1, worldState.getRetryCount(stratA));
        assertFalse("First transient failure should NOT exhaust strategy", worldState.isStrategyExhausted(stratA));

        CognitiveObservation obsNet2 = new CognitiveObservation("dataset_acquisition", false, 1, "", "", "NETWORK_FAILURE", 50, metaNet);
        worldState.recordStrategyAttempt(stratA, obsNet2, 2);

        assertEquals("Second network failure increments retry count to 2", 2, worldState.getRetryCount(stratA));
    }

    @Test
    public void test3_TargetReached() throws Exception {
        CognitiveGoal goal = new CognitiveGoal("Acquire 500 MB data", "DATASET_ACQUISITION", Map.of("targetUsableBytes", 500L * 1024L * 1024L));
        WorldState state = new WorldState("Test3_Session");
        GoalEvaluator evaluator = new GoalEvaluator();

        Map<String, Object> meta = Map.of("usableContentBytes", 500L * 1024L * 1024L, "quantity", 500L * 1024L * 1024L);
        CognitiveObservation obs = new CognitiveObservation("dataset_acquisition", true, 0, "SUCCESS", "", null, 200, meta);

        GoalEvaluation eval = evaluator.evaluate(goal, state, List.of(obs));
        assertTrue("Evaluator must return achieved when target reached", eval.isAchieved());
        assertEquals(1.0, eval.getProgress(), 0.001);
    }

    @Test
    public void test4_PartialResult() throws Exception {
        long target = 500L * 1024L * 1024L;
        long acquired = 200L * 1024L * 1024L;
        CognitiveGoal goal = new CognitiveGoal("Acquire 500 MB data", "DATASET_ACQUISITION", Map.of("targetUsableBytes", target));
        WorldState state = new WorldState("Test4_Session");
        GoalEvaluator evaluator = new GoalEvaluator();

        Map<String, Object> metaA = Map.of(
                "usableContentBytes", acquired,
                "remainingBytes", target - acquired,
                "isSourceExhausted", true,
                "failureType", "SOURCE_EXHAUSTED"
        );
        CognitiveObservation obsA = new CognitiveObservation("dataset_acquisition", false, 1, "Partial output", "", "SOURCE_EXHAUSTED", 150, metaA);

        GoalEvaluation eval = evaluator.evaluate(goal, state, List.of(obsA));
        assertFalse("Goal is partially completed, not fully achieved", eval.isAchieved());
        assertEquals(0.40, eval.getProgress(), 0.01);
    }

    @Test
    public void test5_NoAvailableStrategies() throws Exception {
        String sessionId = "Test5_NoStrategies_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        // Goal with explicit unknown source and tiny iteration budget to verify clean failure termination
        CognitiveGoal goal = new CognitiveGoal("Acquire 500 MB data", "DATASET_ACQUISITION", Map.of("targetUsableBytes", 500L * 1024L * 1024L, "sourceType", "NON_EXISTENT_SOURCE"));

        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), null);
        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals("Engine must terminate cleanly with FAILED when strategies are exhausted", CognitiveState.FAILED, result.getFinalState());
        assertTrue("Result summary should explain failure", result.getSummary() != null && !result.getSummary().isEmpty());
    }

    @Test
    public void test6_SameStrategyIsNeverRepeated() throws Exception {
        WorldState worldState = new WorldState("Test6_Session");
        CognitiveStrategy strat = CognitiveStrategy.acquisitionStrategy("TEST_STRAT", "HUGGING_FACE", "wikitext", "validation", null);

        Map<String, Object> metaEmpty = Map.of(
                "sourceType", "HUGGING_FACE",
                "repository", "wikitext",
                "split", "validation",
                "usableContentBytes", 0L,
                "isSourceExhausted", true,
                "failureType", "SOURCE_EMPTY"
        );
        CognitiveObservation obs = new CognitiveObservation("dataset_acquisition", false, 1, "", "", "SOURCE_EMPTY", 100, metaEmpty);
        worldState.recordStrategyAttempt(strat, obs, 1);

        assertTrue("Strategy marked empty must be exhausted", worldState.isStrategyExhausted(strat));
        assertTrue("Signature lookup must confirm strategy is exhausted", worldState.isStrategyExhausted(strat.getSignature()));
    }

    @Test
    public void test7_DuplicateEvent() throws Exception {
        WorldState worldState = new WorldState("Test7_Session");
        String actionKey = "Test7_Session:iter1:dataset_acquisition:dataset_acquisition:hugging_face:wikitext:train";

        boolean firstCall = worldState.isActionAlreadyExecuted(actionKey);
        assertFalse("First call to action key should return false (not previously executed)", firstCall);

        boolean secondCall = worldState.isActionAlreadyExecuted(actionKey);
        assertTrue("Second duplicate call must return true (already executed)", secondCall);
    }

    @Test
    public void test8_SessionIsolation() throws Exception {
        WorldState stateA = new WorldState("SessionA");
        WorldState stateB = new WorldState("SessionB");

        CognitiveStrategy strat = CognitiveStrategy.acquisitionStrategy("SHARED_STRAT", "HUGGING_FACE", "wikitext", "train", null);

        Map<String, Object> metaA = Map.of("failureType", "SOURCE_EXHAUSTED");
        CognitiveObservation obsA = new CognitiveObservation("dataset_acquisition", false, 1, "", "", "SOURCE_EXHAUSTED", 100, metaA);
        stateA.recordStrategyAttempt(strat, obsA, 1);

        assertTrue("Session A strategy must be exhausted", stateA.isStrategyExhausted(strat));
        assertFalse("Session B must NOT be affected by Session A exhaustion", stateB.isStrategyExhausted(strat));
    }

    @Test
    public void test9_SearchExpansion() throws Exception {
        HuggingFaceSourceDiscovery discovery = new HuggingFaceSourceDiscovery();
        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(500L * 1024L * 1024L)
                .addDomain("code")
                .build();

        List<DataSourceCandidate> round0 = discovery.discoverNextBatch(prefs, 0);
        List<DataSourceCandidate> round1 = discovery.discoverNextBatch(prefs, 1);

        assertNotNull(round0);
        assertNotNull(round1);
        assertTrue("Round 0 candidates should be generated", round0.size() > 0);
        assertTrue("Round 1 expansion candidates should be generated", round1.size() > 0);
    }

    @Test
    public void test10_LlmAbort() throws Exception {
        String sessionId = "Test10_LlmAbort_Session";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        CognitiveGoal goal = new CognitiveGoal("Goal triggering abort decision", "GENERAL", null);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 600000L, 1, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertNotNull("Result state must be computed safely", result.getFinalState());
    }
}
