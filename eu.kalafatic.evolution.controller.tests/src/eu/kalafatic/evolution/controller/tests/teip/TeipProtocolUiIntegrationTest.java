package eu.kalafatic.evolution.controller.tests.teip;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.ServerOperation;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.teip.dto.TeipAcceptedResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipCapabilityDescriptor;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionRequest;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionStatus;
import eu.kalafatic.evolution.controller.teip.dto.TeipHealthResponse;
import eu.kalafatic.evolution.controller.teip.service.TeipExecutionManager;
import eu.kalafatic.evolution.controller.teip.store.TeipEventStore;

/**
 * Unit and integration tests for TEIP Protocol UI & in-process bridge capabilities.
 * Verifies backend integration, DTO mappings, session correlation, execution cancellation,
 * event sequencing, configuration handling, and unavailable TRYST connection scenarios.
 */
public class TeipProtocolUiIntegrationTest {

    @Before
    public void setUp() {
        TeipExecutionManager.getInstance().clearAll();
        TeipExecutionManager.getInstance().setTeipEnabled(true);
        System.setProperty("evo.teip.enabled", "true");
        System.setProperty("evo.tryst.mcp.url", "http://localhost:3000/mcp");
        System.setProperty("evo.tryst.timeout.seconds", "30");
    }

    @After
    public void tearDown() {
        TeipExecutionManager.getInstance().clearAll();
    }

    @Test
    public void testOverviewDataRetrieval() {
        TeipHealthResponse health = TeipExecutionManager.getInstance().checkHealth();
        assertNotNull(health);
        assertEquals("TEIP", health.getProtocol());
        assertEquals("1.0", health.getVersion());
        assertTrue(health.isReady());
        assertEquals("UP", health.getStatus());

        TeipCapabilityDescriptor cap = TeipExecutionManager.getInstance().getCapabilities();
        assertNotNull(cap);
        assertTrue(cap.getSupportedModes().size() > 0);
        assertTrue(cap.getFeatures().getOrDefault("asyncExecution", false));
    }

    @Test
    public void testCapabilitiesDataRetrieval() {
        TeipCapabilityDescriptor cap = TeipExecutionManager.getInstance().getCapabilities();
        assertNotNull(cap.getSupportedModes());
        assertTrue(cap.getSupportedModes().contains("DARWIN_MODE"));
        assertTrue(cap.getSupportedModes().contains("SELF_DEV_MODE"));
        assertTrue(cap.getSupportedModes().contains("SIMPLE_CHAT"));
    }

    @Test
    public void testExecutionsAndSessionCorrelation() {
        TeipExecutionRequest req = new TeipExecutionRequest();
        req.setRequestId("req-test-1");
        req.setCorrelationId("corr-test-1");
        req.setIdempotencyKey("idem-test-1");
        req.setCapability("CODE_IMPLEMENTATION");
        req.setMode("DARWIN_MODE");
        req.setInstruction("Integration test instruction");
        req.setCaller(Map.of("app", "TRYST", "operator", "jules"));

        TeipAcceptedResponse accepted = TeipExecutionManager.getInstance().createExecution(req);
        assertNotNull(accepted);
        String execId = accepted.getExecutionId();
        assertNotNull(execId);

        // Verify mapped SessionContainer
        String evoSessionId = "evo-teip-" + execId;
        SessionContainer sc = SessionManager.getInstance().getSession(evoSessionId);
        assertNotNull("SessionContainer should be automatically mapped for TEIP execution", sc);

        // Verify ServerOperation in session
        ServerOperation op = sc.getOperation("op-" + execId);
        assertNotNull("ServerOperation op-<execId> should exist in container", op);
        assertEquals("evo-teip-" + execId, op.getSessionId());

        // Verify listAllExecutions contains our execution
        var list = TeipExecutionManager.getInstance().listAllExecutions();
        assertTrue(list.stream().anyMatch(e -> e.getExecutionId().equals(execId)));
    }

    @Test
    public void testExecutionCancellation() {
        TeipExecutionRequest req = new TeipExecutionRequest();
        req.setRequestId("req-cancel-1");
        req.setCorrelationId("corr-cancel-1");
        req.setIdempotencyKey("idem-cancel-1");
        req.setCapability("CODE_IMPLEMENTATION");
        req.setMode("DARWIN_MODE");
        req.setInstruction("Long-running task for cancellation");

        TeipAcceptedResponse accepted = TeipExecutionManager.getInstance().createExecution(req);
        String execId = accepted.getExecutionId();

        boolean cancelled = TeipExecutionManager.getInstance().cancelExecution(execId, "Operator cancelled from UI");
        assertTrue(cancelled);

        var status = TeipExecutionManager.getInstance().getStatus(execId);
        assertNotNull(status);
        assertEquals(TeipExecutionStatus.CANCELLED, status.getStatus());

        // Calling cancel again must be idempotent and succeed
        boolean secondCancel = TeipExecutionManager.getInstance().cancelExecution(execId, "Repeat cancel");
        assertTrue(secondCancel);
    }

    @Test
    public void testEventSequencingAndCursor() {
        String execId = "exec-seq-test-" + System.currentTimeMillis();
        TeipEventStore store = TeipEventStore.getInstance();

        var ev1 = store.appendEvent(execId, "EVENT_1", "corr-1", Map.of("step", 1));
        var ev2 = store.appendEvent(execId, "EVENT_2", "corr-1", Map.of("step", 2));
        var ev3 = store.appendEvent(execId, "EVENT_3", "corr-1", Map.of("step", 3));

        var resp1 = store.getEvents(execId, 0, 2);
        assertNotNull(resp1);
        assertEquals(2, resp1.getEvents().size());
        assertTrue(resp1.isHasMore());

        var resp2 = store.getEvents(execId, ev2.getSequence(), 2);
        assertNotNull(resp2);
        assertEquals(1, resp2.getEvents().size());
        assertEquals(ev3.getSequence(), resp2.getEvents().get(0).getSequence());
        assertFalse(resp2.isHasMore());
    }

    @Test
    public void testUnavailableTrystConnectionHandling() {
        // Test connecting to an unavailable endpoint on a non-existent port
        String deadUrl = "http://127.0.0.1:59999/mcp";
        boolean caught = false;
        try {
            eu.kalafatic.evolution.controller.teip.mcp.TrystMcpClient client =
                new eu.kalafatic.evolution.controller.teip.mcp.TrystMcpClient(deadUrl, "token", List.of("*"));
            client.initialize();
        } catch (Exception ex) {
            caught = true;
            assertNotNull(ex);
        }
        assertTrue("Expected connection failure to be caught", caught);
    }

    @Test
    public void testConfigurationUpdateAndMasking() {
        TeipExecutionManager.getInstance().setTeipEnabled(false);
        assertFalse(TeipExecutionManager.getInstance().isTeipEnabled());

        System.setProperty("evo.teip.auth.token", "secret-test-token-12345");
        String token = System.getProperty("evo.teip.auth.token");
        assertEquals("secret-test-token-12345", token);

        // Verify health response reflects disabled status
        var health = TeipExecutionManager.getInstance().checkHealth();
        assertFalse("Disabled TEIP must not report ready", health.isReady());
        assertEquals("DISABLED", health.getStatus());

        // Re-enable
        TeipExecutionManager.getInstance().setTeipEnabled(true);
        assertTrue(TeipExecutionManager.getInstance().isTeipEnabled());
    }
}
