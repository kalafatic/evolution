package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.util.List;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SessionSnapshot;

public class ForgeSessionManagerIncrementalTest {

    private ForgeSessionManager manager;
    private Orchestrator orchestrator;

    @Before
    public void setUp() {
        manager = ForgeSessionManager.getInstance();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-orch-fsm");
        manager.initialize(orchestrator);
    }

    @Test
    public void testStrictFindSessionByIdAndFallbackLogging() {
        ForgeSession s1 = manager.createSession("Session Alpha", "TRANSFORMER");
        assertNotNull(s1);

        // Strict find by ID must return exact match
        ForgeSession foundById = manager.findSessionById(s1.getSessionId());
        assertNotNull(foundById);
        assertEquals(s1.getSessionId(), foundById.getSessionId());

        // Strict find by ID for unknown ID must return null
        assertNull(manager.findSessionById("unknown-uuid-12345"));

        // findSession with name must match exact name
        ForgeSession foundByName = manager.findSession("Session Alpha");
        assertNotNull(foundByName);
        assertEquals(s1.getSessionId(), foundByName.getSessionId());

        // findSession with unknown ID falls back to s1 (first session)
        ForgeSession fallback = manager.findSession("unknown-uuid-12345");
        assertNotNull(fallback);
        assertEquals(s1.getSessionId(), fallback.getSessionId());
    }

    @Test
    public void testTrainerFinishRequestScopeIsolation() {
        ForgeSession sA = manager.createSession("Session A", "TRANSFORMER");
        ForgeSession sB = manager.createSession("Session B", "LLM");

        EvoLlmModel dummyModelA = new EvoLlmModel(100, 32, 2, 1, 64, 2);
        EvoLlmModel dummyModelB = new EvoLlmModel(100, 32, 2, 1, 64, 2);

        EvoLlmTrainer trainerA = new EvoLlmTrainer(dummyModelA);
        EvoLlmTrainer trainerB = new EvoLlmTrainer(dummyModelB);

        manager.registerActiveTrainer(sA.getSessionId(), trainerA);
        manager.registerActiveTrainer(sB.getSessionId(), trainerB);

        // Request finish specifically for Session A
        boolean result = manager.requestFinish(sA.getSessionId());

        assertTrue("requestFinish for Session A should return true", result);
        assertTrue("Trainer A stop flag must be true", trainerA.isStopRequested());
        assertFalse("Trainer B stop flag must remain false (Session B must NOT be stopped)", trainerB.isStopRequested());

        // Clean up
        manager.unregisterActiveTrainer(sA.getSessionId());
        manager.unregisterActiveTrainer(sB.getSessionId());
    }

    @Test
    public void testDeleteSessionCleansEventBufferAndActiveTrainer() {
        ForgeSession session = manager.createSession("Session To Delete", "CNN");
        String sid = session.getSessionId();

        EvoLlmModel dummyModel = new EvoLlmModel(100, 32, 2, 1, 64, 2);
        EvoLlmTrainer trainer = new EvoLlmTrainer(dummyModel);
        manager.registerActiveTrainer(sid, trainer);

        // Populate event buffer
        manager.updateUiState(sid, "testKey", "testValue");
        List<RuntimeEvent> eventsBefore = manager.getRecentEvents(sid);
        assertFalse("Event buffer should contain events before deletion", eventsBefore.isEmpty());

        // Delete session
        boolean deleted = manager.deleteSession(sid);
        assertTrue("deleteSession should return true", deleted);

        // Session must no longer be found by ID
        assertNull(manager.findSessionById(sid));

        // Event buffer must be cleaned up
        List<RuntimeEvent> eventsAfter = manager.getRecentEvents(sid);
        assertTrue("Event buffer should be empty after deletion", eventsAfter.isEmpty());

        // Active trainer stop must be requested upon session deletion
        assertTrue("Trainer stop should be requested on session deletion", trainer.isStopRequested());
    }

    @Test
    public void testCloneAndSnapshotSemantics() {
        ForgeSession original = manager.createSession("Original Session", "LLM");
        String origId = original.getSessionId();

        manager.updateUiState(origId, "layers", 4);
        manager.updateUiState(origId, "heads", 8);

        // Clone session
        ForgeSession clone = manager.cloneSession(origId, "Cloned Session");
        assertNotNull(clone);
        assertNotEquals(origId, clone.getSessionId());
        assertEquals("Cloned Session", clone.getName());
        assertEquals("LLM", clone.getSelectedModelType());
        assertEquals(original.getModelState().getModelGraph(), clone.getModelState().getModelGraph());
        assertEquals(original.getModelState().getHyperparameters(), clone.getModelState().getHyperparameters());

        // Create snapshot
        SessionSnapshot snapshot = manager.createSnapshot(origId, "milestone-1");
        assertNotNull(snapshot);
        assertEquals(origId, snapshot.getSessionId());

        String serialized = snapshot.getFullSerializedState();
        assertNotNull(serialized);
        JSONObject json = new JSONObject(serialized);

        assertTrue(json.has("modelGraph"));
        assertTrue(json.has("hyperparameters"));
        assertTrue(json.has("selectedModelType"));
        assertEquals("LLM", json.getString("selectedModelType"));
    }
}
