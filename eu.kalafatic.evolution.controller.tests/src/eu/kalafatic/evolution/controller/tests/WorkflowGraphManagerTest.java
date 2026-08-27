package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

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

    @Test
    public void testFlowStartedAndCompletedEvents() {
        eventBus.publish(new RuntimeEvent(RuntimeEventType.FLOW_STARTED, sessionId, "OrchestratorService", "Executing user prompt"));

        JSONObject graphJson = manager.getGraphJson(sessionId);
        assertNotNull(graphJson);
        JSONArray nodes = graphJson.getJSONArray("nodes");
        assertTrue(nodes.length() > 0);

        GraphEntity orchestrator = manager.getEntity(sessionId, "orchestrator");
        assertNotNull(orchestrator);
        assertEquals("RUNNING", orchestrator.getStatus());

        eventBus.publish(new RuntimeEvent(RuntimeEventType.FLOW_COMPLETED, sessionId, "OrchestratorService", "Success"));

        assertEquals("DONE", orchestrator.getStatus());
    }

    @Test
    public void testCognitiveStateAndReasoningEvents() {
        eventBus.publish(new RuntimeEvent(RuntimeEventType.FLOW_STARTED, sessionId, "OrchestratorService", "Executing user prompt"));
        eventBus.publish(new RuntimeEvent(RuntimeEventType.COGNITIVE_STATE_CHANGED, sessionId, "CognitiveStatePublisher", "ANALYZING WORKSPACE"));

        GraphEntity orchestrator = manager.getEntity(sessionId, "orchestrator");
        assertNotNull(orchestrator);
        assertEquals("ANALYZING WORKSPACE", orchestrator.getRuntimeState());

        eventBus.publish(new RuntimeEvent(RuntimeEventType.REASONING_STEP, sessionId, "PlannerAgent", "Generating step-by-step plan"));

        GraphEntity localLlm = manager.getEntity(sessionId, "local_llm");
        assertNotNull(localLlm);
        assertEquals("RUNNING", localLlm.getStatus());
        assertEquals("Generating step-by-step plan", localLlm.getRuntimeState());
    }

    @Test
    public void testTaskLifecycleEvents() {
        String taskId = "task-compile-1";

        eventBus.publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "Kernel", taskId));

        GraphEntity taskNode = manager.getEntity(sessionId, taskId);
        assertNotNull(taskNode);
        assertEquals("RUNNING", taskNode.getStatus());

        eventBus.publish(new RuntimeEvent(RuntimeEventType.TASK_COMPLETED, sessionId, "Kernel", taskId));
        assertEquals("DONE", taskNode.getStatus());

        String failedTaskId = "task-test-2";
        eventBus.publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, sessionId, "Kernel", failedTaskId));
        eventBus.publish(new RuntimeEvent(RuntimeEventType.TASK_FAILED, sessionId, "Kernel", failedTaskId));

        GraphEntity failedTaskNode = manager.getEntity(sessionId, failedTaskId);
        assertNotNull(failedTaskNode);
        assertEquals("FAILED", failedTaskNode.getStatus());
    }
}
