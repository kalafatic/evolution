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
    public void testOllamaExporterGeneratesGguf() throws Exception {
        eu.kalafatic.evolution.forge.agent.export.OllamaExporter exporter = new eu.kalafatic.evolution.forge.agent.export.OllamaExporter();
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("forge_export_test");
        try {
            eu.kalafatic.evolution.forge.model.llm.EvoLlmModel model = new eu.kalafatic.evolution.forge.model.llm.EvoLlmModel(100, 64, 2, 1, 128, 4);
            exporter.export("test-model", tempDir, model);
            
            java.nio.file.Path ggufFile = tempDir.resolve("evo.gguf");
            java.nio.file.Path modelfile = tempDir.resolve("Modelfile");
            java.nio.file.Path weightsFile = tempDir.resolve("weights.bin");
            
            assertTrue(java.nio.file.Files.exists(ggufFile));
            assertTrue(java.nio.file.Files.exists(modelfile));
            assertTrue(java.nio.file.Files.exists(weightsFile));
            
            byte[] bytes = java.nio.file.Files.readAllBytes(ggufFile);
            assertTrue(bytes.length >= 4);
            assertEquals((byte) 0x47, bytes[0]); // 'G'
            assertEquals((byte) 0x47, bytes[1]); // 'G'
            assertEquals((byte) 0x55, bytes[2]); // 'U'
            assertEquals((byte) 0x46, bytes[3]); // 'F'
        } finally {
            // cleanup
            java.io.File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    f.delete();
                }
            }
            tempDir.toFile().delete();
        }
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

    @Test
    public void testDatasetsPersistenceInUiState() throws Exception {
        ForgeSession session = manager.createSession("Dataset Test", "SELF_EVO");
        assertNotNull(session);

        org.json.JSONArray datasetsArray = new org.json.JSONArray();
        org.json.JSONObject ds1 = new org.json.JSONObject();
        ds1.put("checked", true);
        ds1.put("path", "/path/to/folder1");
        ds1.put("type", "FOLDER");
        datasetsArray.put(ds1);

        org.json.JSONObject ds2 = new org.json.JSONObject();
        ds2.put("checked", false);
        ds2.put("path", "/path/to/file2.txt");
        ds2.put("type", "FILE");
        datasetsArray.put(ds2);

        manager.updateUiState(session.getSessionId(), "datasets", datasetsArray.toString());

        JSONObject uiState = manager.getUiState(session.getSessionId());
        assertTrue(uiState.has("datasets"));
        org.json.JSONArray restoredArr = new org.json.JSONArray(uiState.getString("datasets"));
        assertEquals(2, restoredArr.length());

        org.json.JSONObject restored1 = restoredArr.getJSONObject(0);
        assertTrue(restored1.getBoolean("checked"));
        assertEquals("/path/to/folder1", restored1.getString("path"));
        assertEquals("FOLDER", restored1.getString("type"));

        org.json.JSONObject restored2 = restoredArr.getJSONObject(1);
        assertFalse(restored2.getBoolean("checked"));
        assertEquals("/path/to/file2.txt", restored2.getString("path"));
        assertEquals("FILE", restored2.getString("type"));
    }
}
