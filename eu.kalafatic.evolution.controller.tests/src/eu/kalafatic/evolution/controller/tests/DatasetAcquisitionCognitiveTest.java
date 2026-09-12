package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveResult;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveState;

/**
 * Experiment A — Generic Headless Cognitive Loop driving Training Data Acquisition.
 */
public class DatasetAcquisitionCognitiveTest {

    @Test
    public void testDatasetAcquisitionWithAdaptiveRecovery() throws Exception {
        String sessionId = "ExperimentA_DatasetSession";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        Map<String, Object> params = new HashMap<>();
        params.put("targetUsableBytes", 1024L * 1024L * 10L); // 10MB
        params.put("requiredLanguage", "en");

        CognitiveGoal goal = new CognitiveGoal(
                "Acquire at least 10MB of valid English conversational training data",
                "DATASET_ACQUISITION",
                params
        );

        CognitiveLoopEngine engine = new CognitiveLoopEngine(10, null);
        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals(CognitiveState.SUCCESS, result.getFinalState());
        assertTrue("Expected observations recorded", result.getObservations().size() > 0);
        assertTrue("Expected decisions recorded", result.getDecisions().size() > 0);

        System.out.println("--- EXPERIMENT A RESULT ---");
        System.out.println("Session: " + result.getSessionId());
        System.out.println("Final State: " + result.getFinalState());
        System.out.println("Attempts: " + result.getAttempts());
        System.out.println("Darwin Invocations: " + result.getDarwinInvocations());
        System.out.println("Observations count: " + result.getObservations().size());
        System.out.println("Summary: " + result.getSummary());
    }
}
