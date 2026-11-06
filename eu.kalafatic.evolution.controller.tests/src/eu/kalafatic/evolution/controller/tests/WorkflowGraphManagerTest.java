package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.workflow.EntityType;
import eu.kalafatic.evolution.controller.workflow.GraphEntity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.workflow.WorkflowGraphManager;

public class WorkflowGraphManagerTest {

    private String sessionId;
    private RuntimeEventBus eventBus;
    private WorkflowGraphManager manager;

    @Before
    public void setUp() {
        sessionId = "test-session-123";
        eventBus = new RuntimeEventBus(sessionId);
        manager = new WorkflowGraphManager(eventBus);
    }

    private void publish(RuntimeEvent event) {
        eventBus.publish(event);
        eventBus.flush();
    }

    @Test
    public void testFlowStartedAndCompletedEvents() {
        publish(new RuntimeEvent(RuntimeEventType.FLOW_STARTED, sessionId, "OrchestratorService", "Executing user prompt"));

        JSONObject graphJson = manager.getGraphJson(sessionId);
        assertNotNull(graphJson);
        JSONArray nodes = graphJson.getJSONArray("nodes");
        assertTrue(nodes.length() > 0);

        GraphEntity orchestrator = manager.getEntity(sessionId, "orchestrator");
        assertNotNull(orchestrator);
        assertEquals("RUNNING", orchestrator.getStatus());

        publish(new RuntimeEvent(RuntimeEventType.FLOW_COMPLETED, sessionId, "OrchestratorService", "Success"));

        assertEquals("DONE", orchestrator.getStatus());
    }

    @Test
    public void testCognitiveStateAndReasoningEvents() {
        publish(new RuntimeEvent(RuntimeEventType.FLOW_STARTED, sessionId, "OrchestratorService", "Executing user prompt"));
        publish(new RuntimeEvent(RuntimeEventType.COGNITIVE_STATE_CHANGED, sessionId, "CognitiveStatePublisher", "ANALYZING WORKSPACE"));

        GraphEntity orchestrator = manager.getEntity(sessionId, "orchestrator");
        assertNotNull(orchestrator);
        assertEquals("ANALYZING WORKSPACE", orchestrator.getRuntimeState());

        publish(new RuntimeEvent(RuntimeEventType.REASONING_STEP, sessionId, "PlannerAgent", "Generating step-by-step plan"));

        GraphEntity localLlm = manager.getEntity(sessionId, "local_llm");
        assertNotNull(localLlm);
        assertEquals("RUNNING", localLlm.getStatus());
        assertEquals("Generating step-by-step plan", localLlm.getRuntimeState());
    }

    @Test
    public void testTaskLifecycleEvents() {
        String taskId = "task-compile-1";

        publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "Kernel", taskId));

        GraphEntity taskNode = manager.getEntity(sessionId, taskId);
        assertNotNull(taskNode);
        assertEquals("RUNNING", taskNode.getStatus());

        publish(new RuntimeEvent(RuntimeEventType.TASK_COMPLETED, sessionId, "Kernel", taskId));
        assertEquals("DONE", taskNode.getStatus());

        String failedTaskId = "task-test-2";
        publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "Kernel", failedTaskId));
        publish(new RuntimeEvent(RuntimeEventType.TASK_FAILED, sessionId, "Kernel", failedTaskId));

        GraphEntity failedTaskNode = manager.getEntity(sessionId, failedTaskId);
        assertNotNull(failedTaskNode);
        assertEquals("FAILED", failedTaskNode.getStatus());
    }

    @Test
    public void testForgeWorkflowStagesAndTransitions() {
        publish(new RuntimeEvent(RuntimeEventType.FORGE_SESSION_CREATED, sessionId, "ForgeService", "Created"));

        JSONObject graphJson = manager.getGraphJson(sessionId);
        assertNotNull(graphJson);
        JSONArray nodes = graphJson.getJSONArray("nodes");
        assertTrue(nodes.length() >= 8);

        // Verify authoritative Forge pipeline entities exist
        GraphEntity forgeEngine = manager.getEntity(sessionId, "forge_engine");
        assertNotNull(forgeEngine);
        assertEquals("RUNNING", forgeEngine.getStatus());

        GraphEntity scanner = manager.getEntity(sessionId, "scanner");
        assertNotNull(scanner);
        assertEquals("PENDING", scanner.getStatus());
        assertEquals("Source Scanner", scanner.getLabel());

        GraphEntity enhancer = manager.getEntity(sessionId, "enhancer");
        assertNotNull(enhancer);
        assertEquals("PENDING", enhancer.getStatus());

        GraphEntity validator = manager.getEntity(sessionId, "validator");
        assertNotNull(validator);
        assertEquals("PENDING", validator.getStatus());

        GraphEntity trainer = manager.getEntity(sessionId, "trainer");
        assertNotNull(trainer);
        assertEquals("PENDING", trainer.getStatus());

        GraphEntity evaluator = manager.getEntity(sessionId, "evaluator");
        assertNotNull(evaluator);
        assertEquals("PENDING", evaluator.getStatus());

        GraphEntity exporter = manager.getEntity(sessionId, "exporter");
        assertNotNull(exporter);
        assertEquals("PENDING", exporter.getStatus());

        GraphEntity registration = manager.getEntity(sessionId, "registration");
        assertNotNull(registration);
        assertEquals("PENDING", registration.getStatus());

        // Test progressive stage updates
        publish(new RuntimeEvent(RuntimeEventType.EVOLUTION_PROGRESS, sessionId, "ForgeService", "SCANNING")
                .withMetadata("status", "SCANNING")
                .withMetadata("filesScanned", 42));

        assertEquals("RUNNING", scanner.getStatus());
        assertTrue(scanner.getRuntimeState().contains("42 files"));

        publish(new RuntimeEvent(RuntimeEventType.EVOLUTION_PROGRESS, sessionId, "ForgeService", "ENHANCING")
                .withMetadata("status", "ENHANCING"));

        assertEquals("DONE", scanner.getStatus());
        assertEquals("RUNNING", enhancer.getStatus());

        publish(new RuntimeEvent(RuntimeEventType.EVOLUTION_PROGRESS, sessionId, "ForgeService", "TRAINING")
                .withMetadata("status", "TRAINING")
                .withMetadata("currentLoss", 0.042)
                .withMetadata("currentEpoch", "2/5"));

        assertEquals("DONE", enhancer.getStatus());
        assertEquals("RUNNING", trainer.getStatus());
        assertTrue(trainer.getRuntimeState().contains("0.0420"));

        publish(new RuntimeEvent(RuntimeEventType.EVOLUTION_PROGRESS, sessionId, "ForgeService", "COMPLETE")
                .withMetadata("status", "COMPLETE"));

        assertEquals("DONE", trainer.getStatus());
        assertEquals("DONE", forgeEngine.getStatus());
    }

    @Test
    public void testLinkDeduplicationAndNoSelfLoops() {
        publish(new RuntimeEvent(RuntimeEventType.FORGE_SESSION_CREATED, sessionId, "ForgeService", "Created"));

        JSONObject graph1 = manager.getGraphJson(sessionId);
        int linkCount1 = graph1.getJSONArray("links").length();

        // Repeated forge template trigger should be idempotent
        publish(new RuntimeEvent(RuntimeEventType.FORGE_SESSION_CREATED, sessionId, "ForgeService", "Created"));
        JSONObject graph2 = manager.getGraphJson(sessionId);
        int linkCount2 = graph2.getJSONArray("links").length();

        assertEquals(linkCount1, linkCount2);

        // Verify no duplicate links exist
        JSONArray links = graph2.getJSONArray("links");
        java.util.Set<String> linkKeys = new java.util.HashSet<>();
        for (int i = 0; i < links.length(); i++) {
            JSONObject l = links.getJSONObject(i);
            String from = l.getString("from");
            String to = l.getString("to");
            String type = l.optString("type", "");

            assertNotEquals("Self-loop should not exist", from, to);
            String key = from + "->" + to + ":" + type;
            assertTrue("Duplicate link detected: " + key, linkKeys.add(key));
        }
    }

    @Test
    public void testSupervisorDoesNotPolluteForgeWorkflow() {
        // In Forge mode
        publish(new RuntimeEvent(RuntimeEventType.FORGE_SESSION_CREATED, sessionId, "ForgeService", "Created"));

        // When external or self-dev supervisor event arrives
        publish(new RuntimeEvent(RuntimeEventType.SUPERVISOR_STATUS_CHANGED, sessionId, "Kernel", "RUNNING"));

        // Supervisor should NOT be injected into Forge graph
        GraphEntity supervisor = manager.getEntity(sessionId, "supervisor");
        assertNull("Supervisor node should not pollute active Forge graph", supervisor);

        // Unrelated self-dev task should also not be injected into Forge pipeline
        publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "TaskExecutor", "sd-task-179999-0"));
        GraphEntity sdTask = manager.getEntity(sessionId, "sd-task-179999-0");
        assertNull("SD Task node should not pollute active Forge graph", sdTask);
    }

    @Test
    public void testModeSwitchingCleansStaleEntities() {
        // Start in Forge
        publish(new RuntimeEvent(RuntimeEventType.FORGE_SESSION_CREATED, sessionId, "ForgeService", "Created"));
        assertNotNull(manager.getEntity(sessionId, "forge_engine"));

        // Switch to Self-Dev mode
        publish(new RuntimeEvent(RuntimeEventType.MODE_CHANGED, sessionId, "User", "SELF_DEV"));

        assertNull("forge_engine should be removed when switching to SELF_DEV", manager.getEntity(sessionId, "forge_engine"));
        assertNotNull("supervisor should exist in SELF_DEV mode", manager.getEntity(sessionId, "supervisor"));
        assertNotNull("evolution_loop should exist in SELF_DEV mode", manager.getEntity(sessionId, "evolution_loop"));
    }

    @Test
    public void testTaskMetadataResolutionAndReadableLabels() {
        publish(new RuntimeEvent(RuntimeEventType.MODE_CHANGED, sessionId, "User", "SELF_DEV"));

        RuntimeEvent taskEvent = new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "TaskExecutor", "sd-task-1741234567890-0")
                .withMetadata("name", "Compile JdtModel")
                .withMetadata("taskType", "maven")
                .withMetadata("description", "Execute maven compilation on target bundle")
                .withMetadata("priority", 1);

        publish(taskEvent);

        GraphEntity task = manager.getEntity(sessionId, "sd-task-1741234567890-0");
        assertNotNull(task);
        assertEquals("Compile JdtModel", task.getLabel());
        assertEquals("maven", task.getCategory());
        assertEquals("Execute maven compilation on target bundle", task.getDescription());

        // Check fallback label when name is absent
        RuntimeEvent fallbackTaskEvent = new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "TaskExecutor", "sd-task-1741234567890-3");
        publish(fallbackTaskEvent);

        GraphEntity fallbackTask = manager.getEntity(sessionId, "sd-task-1741234567890-3");
        assertNotNull(fallbackTask);
        assertEquals("Task #4", fallbackTask.getLabel());
    }

    @Test
    public void testLargeGraphIdempotency() {
        publish(new RuntimeEvent(RuntimeEventType.MODE_CHANGED, sessionId, "User", "SELF_DEV"));

        for (int i = 0; i < 40; i++) {
            publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "TaskExecutor", "sd-task-batch-" + i)
                    .withMetadata("name", "Task #" + i));
            // Send repeated start event
            publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "TaskExecutor", "sd-task-batch-" + i));
        }

        JSONObject graph = manager.getGraphJson(sessionId);
        JSONArray nodes = graph.getJSONArray("nodes");
        // user + supervisor + evolution_loop + 40 tasks = 43 nodes
        assertEquals(43, nodes.length());
    }
}
