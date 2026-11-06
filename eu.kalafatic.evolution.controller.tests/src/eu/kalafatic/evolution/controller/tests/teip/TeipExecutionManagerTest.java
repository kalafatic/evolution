package eu.kalafatic.evolution.controller.tests.teip;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.teip.dto.*;
import eu.kalafatic.evolution.controller.teip.service.TeipExecutionManager;
import eu.kalafatic.evolution.controller.teip.store.TeipEventStore;

/**
 * Unit & Integration test for TeipExecutionManager, TeipEventStore, and idempotency guarantees.
 */
public class TeipExecutionManagerTest {

    private TeipExecutionManager manager;

    @Before
    public void setUp() {
        manager = TeipExecutionManager.getInstance();
        manager.clearAll();
        manager.setTeipEnabled(true);
    }

    @After
    public void tearDown() {
        manager.clearAll();
    }

    @Test
    public void testHealthCheck() {
        TeipHealthResponse health = manager.checkHealth();
        assertNotNull(health);
        assertEquals("UP", health.getStatus());
        assertTrue(health.isReady());

        manager.setTeipEnabled(false);
        TeipHealthResponse disabledHealth = manager.checkHealth();
        assertEquals("DISABLED", disabledHealth.getStatus());
        assertFalse(disabledHealth.isReady());
    }

    @Test
    public void testCapabilityDiscovery() {
        TeipCapabilityDescriptor caps = manager.getCapabilities();
        assertNotNull(caps);
        assertFalse(caps.getSupportedModes().isEmpty());
        assertTrue(caps.getSupportedModes().contains("DARWIN_MODE"));
        assertTrue(caps.getSupportedModes().contains("SELF_DEV_MODE"));
        assertFalse(caps.getAgents().isEmpty());
        assertFalse(caps.getModels().isEmpty());
        assertTrue(caps.getFeatures().get("asyncExecution"));
    }

    @Test
    public void testCreateExecutionAndIdempotency() {
        TeipExecutionRequest req1 = new TeipExecutionRequest();
        req1.setRequestId("req-1");
        req1.setCorrelationId("corr-1");
        req1.setIdempotencyKey("idem-unique-123");
        req1.setCapability("CODE_IMPLEMENTATION");
        req1.setMode("DARWIN_MODE");
        req1.setInstruction("Implement unit tests for AuthController");
        req1.setCaller(Map.of("app", "TRYST", "taskId", "task-99"));

        TeipAcceptedResponse resp1 = manager.createExecution(req1);
        assertNotNull(resp1);
        assertNotNull(resp1.getExecutionId());
        assertEquals("corr-1", resp1.getCorrelationId());
        assertEquals(TeipExecutionStatus.QUEUED, resp1.getStatus());

        // Repeated request with same idempotency key returns exact same execution
        TeipAcceptedResponse resp2 = manager.createExecution(req1);
        assertEquals(resp1.getExecutionId(), resp2.getExecutionId());
        assertEquals(resp1.getCreatedAt(), resp2.getCreatedAt());

        // Status query
        TeipExecutionStatusResponse status = manager.getStatus(resp1.getExecutionId());
        assertNotNull(status);
        assertEquals(resp1.getExecutionId(), status.getExecutionId());
        assertEquals("corr-1", status.getCorrelationId());
    }

    @Test
    public void testEventSequencingAndCursorPagination() {
        String execId = "exec-test-events";
        TeipEventStore store = TeipEventStore.getInstance();
        store.clear(execId);

        store.appendEvent(execId, "EVENT_1", "corr-1", Map.of("step", 1));
        store.appendEvent(execId, "EVENT_2", "corr-1", Map.of("step", 2));
        store.appendEvent(execId, "EVENT_3", "corr-1", Map.of("step", 3));

        // Get all from sequence 0
        TeipEventsResponse respAll = store.getEvents(execId, 0, 10);
        assertEquals(3, respAll.getEvents().size());
        assertEquals(1, respAll.getEvents().get(0).getSequence());
        assertEquals(2, respAll.getEvents().get(1).getSequence());
        assertEquals(3, respAll.getEvents().get(2).getSequence());
        assertEquals(3, respAll.getNextSequence());
        assertFalse(respAll.isHasMore());

        // Paginate after sequence 1 with limit 1
        TeipEventsResponse respPage = store.getEvents(execId, 1, 1);
        assertEquals(1, respPage.getEvents().size());
        assertEquals(2, respPage.getEvents().get(0).getSequence());
        assertTrue(respPage.isHasMore());
        assertEquals(2, respPage.getNextSequence());

        // Paginate after sequence 2
        TeipEventsResponse respPage2 = store.getEvents(execId, 2, 10);
        assertEquals(1, respPage2.getEvents().size());
        assertEquals(3, respPage2.getEvents().get(0).getSequence());
        assertFalse(respPage2.isHasMore());
    }

    @Test
    public void testExecutionCancellation() {
        TeipExecutionRequest req = new TeipExecutionRequest();
        req.setRequestId("req-cancel-1");
        req.setCorrelationId("corr-cancel-1");
        req.setInstruction("Long running analysis");

        TeipAcceptedResponse accepted = manager.createExecution(req);
        String execId = accepted.getExecutionId();

        boolean cancelled = manager.cancelExecution(execId, "Test operator cancellation");
        assertTrue(cancelled);

        TeipExecutionStatusResponse status = manager.getStatus(execId);
        assertEquals(TeipExecutionStatus.CANCELLED, status.getStatus());

        // Idempotent cancellation
        boolean cancelledAgain = manager.cancelExecution(execId, "Repeat cancellation");
        assertTrue(cancelledAgain);
    }
}
