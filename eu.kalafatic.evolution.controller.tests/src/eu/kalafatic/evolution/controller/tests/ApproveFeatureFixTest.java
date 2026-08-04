package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class ApproveFeatureFixTest {

    @Test
    public void testAutoApproveResumesWaitingProcess() throws Exception {
        String sessionId = "test-approve-session-" + System.currentTimeMillis();

        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId(sessionId);
        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        orchestrator.getAiChat().setPromptInstructions(OrchestrationFactory.eINSTANCE.createPromptInstructions());

        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext context = new TaskContext(orchestrator, null);
        context.setSessionId(sessionId);
        OrchestratorServiceImpl.getInstance().registerContext(sessionId, context);

        assertFalse("Initially, autoApprove should be false", context.isAutoApprove());

        // Simulate engine waiting for variant selection via requestInput on a background thread
        CompletableFuture<String> waitFuture = CompletableFuture.supplyAsync(() -> {
            try {
                // Wait for approval / selection
                return context.requestInput("Please select a variant").get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        });

        // Give the background thread a moment to start and enter requestInput
        Thread.sleep(200);

        assertTrue("Context should be waiting for input", context.isWaitingForInput());

        // Now simulate the user toggling / selecting "Auto-Approve" on the UI
        Map<String, Object> settings = new HashMap<>();
        settings.put("autoApprove", true);
        OrchestratorServiceImpl.getInstance().updateConfiguration(sessionId, settings);

        // Verify that the autoApprove property is updated
        assertTrue("autoApprove should now be true", context.isAutoApprove());

        // Verify that the process resumes with "Approved"
        String result = waitFuture.get(2, TimeUnit.SECONDS);
        assertEquals("Resumed process should receive 'Approved'", "Approved", result);
        assertFalse("Context should no longer be waiting for input", context.isWaitingForInput());

        // Cleanup
        SessionManager.getInstance().shutdownSession(sessionId);
    }
}
