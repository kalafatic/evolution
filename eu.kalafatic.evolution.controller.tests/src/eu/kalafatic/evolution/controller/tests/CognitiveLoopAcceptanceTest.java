package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eu.kalafatic.evolution.controller.kernel.AuthorityEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveBudget;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveDecision;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveDecisionType;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveFailureType;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveGoal;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveObservation;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveResult;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveState;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.GoalEvaluation;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.GoalEvaluator;
import eu.kalafatic.evolution.controller.orchestration.cognitive.loop.WorldState;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.supervision.EvolutionDecision;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.controller.tools.ToolFactory;

/**
 * Acceptance test suite demonstrating:
 * 1. Simple non-Darwin operation (file inspect / search)
 * 2. Task requiring tool execution and concrete strategy adaptation
 * 3. Task requiring Darwin escalation
 * 4. Distinguishing action success vs goal completion
 * 5. Safe termination on no-progress condition
 */
public class CognitiveLoopAcceptanceTest {

    @Test
    public void testGoal1_SimpleNonDarwinOperation() throws Exception {
        String sessionId = "Acceptance_Goal1_NonDarwin";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        File tempFile = File.createTempFile("acceptance_test_", ".txt");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "Hello Evo Cognitive Loop");

        TaskContext context = new TaskContext(null, tempFile.getParentFile());
        context.setSessionId(sessionId);

        // Tool registered to read/inspect files
        ITool testFileTool = new ITool() {
            @Override
            public String getName() {
                return "test_inspector";
            }

            @Override
            public String execute(String command, File workingDir, TaskContext ctx) throws Exception {
                return "{\"status\": \"OK\", \"content\": \"Hello Evo Cognitive Loop\"}";
            }
        };
        ToolFactory.registerTool("test_inspector", testFileTool);

        Map<String, Object> params = new HashMap<>();
        params.put("targetFile", tempFile.getAbsolutePath());
        params.put("targetFileContains", "Hello Evo Cognitive Loop");
        params.put("requiredCapabilityVerification", "test_inspector");

        CognitiveGoal goal = new CognitiveGoal("Inspect file and verify text content", "FILE", params);
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(5, 60000L, 1, 3), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals("Simple non-Darwin operation must achieve SUCCESS", CognitiveState.SUCCESS, result.getFinalState());
        assertEquals("Darwin should NOT be invoked for simple non-Darwin task", 0, result.getDarwinInvocations());
        assertTrue("Result summary should confirm goal success", result.getSummary().contains("successfully verified"));
    }

    @Test
    public void testGoal2_ToolExecutionWithAdaptation() throws Exception {
        String sessionId = "Acceptance_Goal2_Adaptation";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        final int[] attempts = new int[]{0};

        // Capability that fails on attempt 1 (transient timeout) and succeeds on attempt 2 (adapted timeout/split)
        ITool adaptingTool = new ITool() {
            @Override
            public String getName() {
                return "adaptive_tool";
            }

            @Override
            public String execute(String command, File workingDir, TaskContext ctx) throws Exception {
                attempts[0]++;
                if (attempts[0] == 1) {
                    return "{\"failureType\": \"NETWORK_FAILURE\", \"error\": \"Connection timeout\"}";
                }
                return "{\"quantity\": 1000.0, \"status\": \"SUCCESS\", \"result\": \"Loaded dataset\"}";
            }
        };
        ToolFactory.registerTool("adaptive_tool", adaptingTool);

        Map<String, Object> params = new HashMap<>();
        params.put("targetMetric", 1000.0);
        params.put("currentMetricKey", "quantity");

        CognitiveGoal goal = new CognitiveGoal("Acquire 1000 records with adaptive recovery", "DATA", params);
        // Budget allowing retries
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(5, 60000L, 1, 3, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals("Adaptive task must succeed after retry/adaptation", CognitiveState.SUCCESS, result.getFinalState());
        assertTrue("At least 2 attempts should have been executed", attempts[0] >= 2);
        assertTrue("Observations must record initial failure followed by success",
                result.getObservations().stream().anyMatch(o -> !o.isSuccess()) &&
                result.getObservations().stream().anyMatch(o -> o.isSuccess()));
    }

    @Test
    public void testGoal3_DarwinEscalation() throws Exception {
        String sessionId = "Acceptance_Goal3_DarwinEscalation";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        // Synthesis goal where no standard tool matches, triggering Darwin escalation
        CognitiveGoal goal = new CognitiveGoal("Synthesize evolutionary architecture variant", "SYNTHESIZE", Collections.emptyMap());

        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(3, 60000L, 2, 2), null);
        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        // Darwin was invoked or attempted
        assertTrue("Darwin escalation path must have been initiated",
                result.getDarwinInvocations() > 0 ||
                result.getDecisions().stream().anyMatch(d -> d.getType() == CognitiveDecisionType.START_DARWIN) ||
                result.getObservations().stream().anyMatch(o -> "DARWIN_SEARCH".equals(o.getActionName())));
    }

    @Test
    public void testDistinguishActionSuccessVsGoalSuccess() throws Exception {
        String sessionId = "Acceptance_ActionSuccessVsGoalSuccess";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        // Tool executes successfully (exit code 0), but returns only 200 items when 1000 are required
        ITool partialTool = new ITool() {
            @Override
            public String getName() {
                return "partial_tool";
            }

            @Override
            public String execute(String command, File workingDir, TaskContext ctx) throws Exception {
                return "{\"quantity\": 200.0, \"status\": \"SUCCESS\"}";
            }
        };
        ToolFactory.registerTool("partial_tool", partialTool);

        Map<String, Object> params = new HashMap<>();
        params.put("targetMetric", 1000.0);
        params.put("currentMetricKey", "quantity");

        CognitiveGoal goal = new CognitiveGoal("Acquire 1000 records", "DATA", params);
        // Tight budget so loop terminates when target not reached
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(2, 60000L, 1, 1), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        // Action was successful, but goal was NOT satisfied
        assertTrue("Observations contain successful tool executions",
                result.getObservations().stream().anyMatch(CognitiveObservation::isSuccess));
        assertEquals("Goal must NOT be marked SUCCESS when criteria are unmet", CognitiveState.FAILED, result.getFinalState());
    }

    @Test
    public void testNoProgressSafeTermination() throws Exception {
        String sessionId = "Acceptance_NoProgressTermination";
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(null, new File("."));
        context.setSessionId(sessionId);

        // Tool executes but makes 0 progress toward goal
        ITool stagnantTool = new ITool() {
            @Override
            public String getName() {
                return "stagnant_tool";
            }

            @Override
            public String execute(String command, File workingDir, TaskContext ctx) throws Exception {
                return "{\"quantity\": 0.0, \"status\": \"NO_DATA\"}";
            }
        };
        ToolFactory.registerTool("stagnant_tool", stagnantTool);

        Map<String, Object> params = new HashMap<>();
        params.put("targetMetric", 500.0);

        CognitiveGoal goal = new CognitiveGoal("Reach 500 data points", "GENERAL", params);
        // Consecutive no progress threshold = 2
        CognitiveLoopEngine engine = new CognitiveLoopEngine(new CognitiveBudget(10, 60000L, 1, 2), null);

        CognitiveResult result = engine.solve(session, context, goal);

        assertNotNull(result);
        assertEquals("Must safely terminate with FAILED on consecutive no-progress limit", CognitiveState.FAILED, result.getFinalState());
        assertTrue("Iterations must be bounded well below max iterations", result.getAttempts() <= 4);
    }
}
