package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class ForgeWorkflowTest {

    private ForgeSessionManager manager;
    private Orchestrator orchestrator;

    @Before
    public void setUp() {
        manager = ForgeSessionManager.getInstance();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-orch");
        manager.initialize(orchestrator);
    }

    @Test
    public void testZeroToTrainedWorkflow() throws Exception {
        // 1. Create session (Drag from palette)
        ForgeSession session = manager.createSession("Test MLP", "MLP");
        assertNotNull(session);
        assertEquals("MLP", session.getSelectedModelType());

        // 2. Verify Architecture Generated
        String initialGraph = session.getModelState().getModelGraph();
        assertTrue(initialGraph.contains("m_h0")); // At least one hidden layer by default

        JSONObject uiState = manager.getUiState(session.getSessionId());
        assertEquals("ARCH_GENERATED", uiState.getString("forge.workflow.status"));

        // 3. Update structural property (Review Properties)
        manager.updateUiState(session.getSessionId(), "layers", 3);

        // 4. Verify Regeneration
        String updatedGraph = session.getModelState().getModelGraph();
        assertNotEquals(initialGraph, updatedGraph);
        assertTrue(updatedGraph.contains("m_h0"));
        assertTrue(updatedGraph.contains("m_h1"));
        assertTrue(updatedGraph.contains("m_h2"));

        // 5. Confirm Properties
        manager.updateWorkflowStatus(session.getSessionId(), "PROPERTIES_CONFIRMED");
        assertEquals("PROPERTIES_CONFIRMED", manager.getUiState(session.getSessionId()).getString("forge.workflow.status"));

        // 6. Select Dataset
        manager.updateWorkflowStatus(session.getSessionId(), "DATASET_SELECTED");
        assertEquals("DATASET_SELECTED", manager.getUiState(session.getSessionId()).getString("forge.workflow.status"));

        // 7. Start Training
        manager.updateStatus(session.getSessionId(), ForgeStatus.TRAINING);
        assertEquals(ForgeStatus.TRAINING, session.getStatus());
        assertEquals("TRAINING_ACTIVE", manager.getUiState(session.getSessionId()).getString("forge.workflow.status"));
    }
}
