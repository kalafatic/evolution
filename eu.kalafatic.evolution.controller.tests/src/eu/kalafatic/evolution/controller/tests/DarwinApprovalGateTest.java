package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinApprovalResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class DarwinApprovalGateTest {

    private static class TestDarwinEngine extends ADarwinEngine {
        public TestDarwinEngine(TaskContext context) {
            super(context, null, null, eu.kalafatic.evolution.controller.orchestration.PlatformType.DARWIN_MODE);
        }
    }

    @Test
    public void testApprovalGateAndUserDecisions() throws Exception {
        String sessionId = "test-approval-gate-" + System.currentTimeMillis();

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId(sessionId);
        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(orchestrator, null);
        context.setSessionId(sessionId);
        context.getMetadata().put("testMode", true);
        OrchestratorServiceImpl.getInstance().registerContext(sessionId, context);

        TestDarwinEngine engine = new TestDarwinEngine(context);

        List<BranchVariant> candidates = new ArrayList<>();
        BranchVariant v1 = new BranchVariant();
        v1.setId("v1");
        v1.setStrategy("Strategy 1");
        v1.setScore(0.8);
        v1.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        candidates.add(v1);

        BranchVariant v2 = new BranchVariant();
        v2.setId("v2");
        v2.setStrategy("Strategy 2");
        v2.setScore(0.9);
        v2.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        candidates.add(v2);

        // Test Manual Mode (autoApprove is false)
        DarwinApprovalResult result = engine.requestApproval(candidates, v2, null);
        assertEquals(DarwinApprovalResult.Action.WAIT, result.getAction());
        assertEquals(SystemState.WAITING_FOR_USER_DECISION, context.getStateHolder().getState());

        List<BranchVariant> storedCandidates = (List<BranchVariant>) context.getMetadata().get("pending_candidates");
        assertNotNull(storedCandidates);
        assertEquals(2, storedCandidates.size());

        // Test User select decision
        ADarwinEngine.handleUserDecision(context, "SELECT v2", session);
        assertEquals(SystemState.EXECUTING, context.getStateHolder().getState());
        assertEquals("v2", context.getMetadata().get("resume_manual_id"));

        // Put pending candidates back for retry test
        context.getMetadata().put("pending_candidates", candidates);
        ADarwinEngine.handleUserDecision(context, "RETRY", session);
        assertEquals(SystemState.INIT, context.getStateHolder().getState());

        // Test Cancel decision
        context.getMetadata().put("pending_candidates", candidates);
        ADarwinEngine.handleUserDecision(context, "CANCEL", session);
        assertEquals(SystemState.FAILED, context.getStateHolder().getState());

        // Reset to WAITING_FOR_USER_DECISION to test auto-approve toggle resume
        IterationManager.forceTransition(SystemState.WAITING_FOR_USER_DECISION, context);
        context.getMetadata().put("pending_candidates", candidates);
        context.getMetadata().put("recommended_candidate", v2);

        Map<String, Object> settings = new HashMap<>();
        settings.put("autoApprove", true);
        OrchestratorServiceImpl.getInstance().updateConfiguration(sessionId, settings);

        // State must transition to EXECUTING and choose v2
        assertEquals(SystemState.EXECUTING, context.getStateHolder().getState());
        assertEquals("v2", context.getMetadata().get("resume_manual_id"));

        SessionManager.getInstance().shutdownSession(sessionId);
    }
}
