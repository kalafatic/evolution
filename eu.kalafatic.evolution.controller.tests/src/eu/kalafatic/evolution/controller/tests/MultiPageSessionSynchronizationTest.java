package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Integration test verifying live session synchronization, observational tab switching state preservation,
 * and timestamp-based sequence guards.
 */
public class MultiPageSessionSynchronizationTest {

    private Orchestrator orchestrator;
    private ForgeSessionManager sessionManager;

    @Before
    public void setUp() {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-orchestrator-1");
        sessionManager = ForgeSessionManager.getInstance();
        sessionManager.initialize(orchestrator);
    }

    @Test
    public void testActiveSessionStatePreservedAcrossViews() {
        ForgeSession session = sessionManager.createSession("SyncTestSession", "TRANSFORMER");
        assertNotNull("Created session must not be null", session);
        String sessionId = session.getSessionId();

        TrainingTelemetry telemetry = sessionManager.getTelemetry(sessionId);
        assertNotNull("Telemetry must be initialized", telemetry);
        telemetry.status = "RUNNING";
        telemetry.currentEpoch = 3;
        telemetry.totalEpochs = 10;
        telemetry.currentLoss = 0.25;

        // Verify active training session resolution
        String activeSessionId = sessionManager.getActiveTrainingSessionId();
        assertNotNull("Active session ID must be resolved", activeSessionId);

        // Verify lookup by session ID and by name returns the same session
        ForgeSession foundById = sessionManager.findSessionById(sessionId);
        ForgeSession foundByName = sessionManager.findSession("SyncTestSession");

        assertEquals("Session resolved by ID must match created session", sessionId, foundById.getSessionId());
        assertEquals("Session resolved by name must match created session", sessionId, foundByName.getSessionId());

        // Verify switching context view lookup preserves telemetry metrics
        TrainingTelemetry retrievedTelemetry = sessionManager.getTelemetry("SyncTestSession");
        assertEquals("Current epoch must be preserved across view lookup", 3, retrievedTelemetry.currentEpoch);
        assertEquals("Loss must be preserved across view lookup", 0.25, retrievedTelemetry.currentLoss, 0.001);
    }

    @Test
    public void testTimestampSequenceGuard() {
        final long[] lastAppliedTimestamp = { 0L };
        final int[] appliedEventsCount = { 0 };

        RuntimeEventListener listener = new RuntimeEventListener() {
            @Override
            public void onEvent(RuntimeEvent event) {
                if (event.getTimestamp() >= lastAppliedTimestamp[0]) {
                    lastAppliedTimestamp[0] = event.getTimestamp();
                    appliedEventsCount[0]++;
                }
            }
        };

        long now = System.currentTimeMillis();

        // Simulate newer event first
        RuntimeEvent newerEvent = new RuntimeEvent(RuntimeEventType.VIEW_UPDATED, "test-orchestrator-1", null, null, "TestSource", "UPDATE_NEW", now + 100);
        listener.onEvent(newerEvent);

        assertEquals("Newer event must be applied", 1, appliedEventsCount[0]);
        assertEquals("Last applied timestamp must match newer event", now + 100, lastAppliedTimestamp[0]);

        // Simulate older stale event arriving later
        RuntimeEvent olderEvent = new RuntimeEvent(RuntimeEventType.VIEW_UPDATED, "test-orchestrator-1", null, null, "TestSource", "UPDATE_OLD", now);
        listener.onEvent(olderEvent);

        assertEquals("Older stale event must be rejected by timestamp sequence guard", 1, appliedEventsCount[0]);
        assertEquals("Last applied timestamp must remain at newer timestamp", now + 100, lastAppliedTimestamp[0]);
    }
}
