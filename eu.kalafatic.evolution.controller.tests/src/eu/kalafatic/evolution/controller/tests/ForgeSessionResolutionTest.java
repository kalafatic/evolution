package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.resource.NetworkInterfaceHelper;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

import org.junit.Before;
import org.junit.Test;

/**
 * Regression tests for (a) Forge API URL normalization and (b) Forge session resolution.
 *
 * A session lookup that misses must NOT create a new session as a side effect; it must
 * fall back to the existing default session. A backslash-escaped URL (the
 * {@code Properties}-style {@code http\://} artifact) must be normalized at the
 * configuration-loading boundary so requests never receive a malformed URL.
 */
public class ForgeSessionResolutionTest {

    private ForgeSessionManager manager;
    private Orchestrator orchestrator;

    @Before
    public void setUp() {
        manager = ForgeSessionManager.getInstance();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-orch-resolution");
        manager.initialize(orchestrator);
    }

    @Test
    public void testMissingSessionLookupDoesNotCreateNewSession() {
        int before = orchestrator.getForgeSessions().size();
        assertTrue("Default sessions must be seeded", before > 0);

        ForgeSession resolved = manager.findSession("20261124-1927_task");

        // A missing session must NOT be silently substituted with another session
        // (INVALID REQUEST), and must never create a new session as a side effect.
        assertNull("Missing session lookup must return null (no silent substitution), got: "
                + (resolved != null ? resolved.getName() : "null"), resolved);
        assertEquals("Missing session lookup must not create a new session",
                before, orchestrator.getForgeSessions().size());
    }

    @Test
    public void testExplicitDefaultSessionSelection() {
        ForgeSession resolved = manager.findDefaultSession();
        assertNotNull("findDefaultSession() must return the seeded default session", resolved);
        assertEquals("Default session must be 'Self-Evo Forging'",
                "Self-Evo Forging", resolved.getName());
        assertNotNull(resolved.getSessionId());
    }

    @Test
    public void testMissingSessionDoesNotTouchDefaultSession() {
        ForgeSession defaultSession = manager.findDefaultSession();
        assertNotNull(defaultSession);
        String defaultId = defaultSession.getSessionId();
        String defaultName = defaultSession.getName();

        ForgeSession resolved = manager.findSession("09da2ed8-c78a-4463-b490-fc9322b2d49f");
        assertNull("Unknown UUID must not resolve to the default session", resolved);

        ForgeSession stillDefault = manager.findDefaultSession();
        assertNotNull(stillDefault);
        assertEquals("Default session identity must remain unchanged", defaultId, stillDefault.getSessionId());
        assertEquals("Default session name must remain unchanged", defaultName, stillDefault.getName());
    }

    @Test
    public void testExplicitSessionCreationStillWorks() {
        int before = orchestrator.getForgeSessions().size();
        ForgeSession created = manager.createSession("Explicit New Session", "MLP");
        assertNotNull(created);
        assertNotNull(created.getSessionId());
        assertEquals("Explicit creation must add exactly one session", before + 1,
                orchestrator.getForgeSessions().size());
    }

    @Test
    public void testSessionIdIsUuid() {
        ForgeSession created = manager.createSession("Uuid Check", "MLP");
        // ForgeSession IDs are UUIDs (not timestamp-based); timestamp-based IDs such as
        // "20261124-1927_task" come from the AI Commander ChatSession namespace.
        assertTrue("ForgeSession ID must be a UUID, got: " + created.getSessionId(),
                created.getSessionId().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
    }

    @Test
    public void testUrlNormalizationStripsBackslashEscape() {
        assertEquals("http://localhost:28080",
                NetworkInterfaceHelper.normalizeUrl("http\\://localhost:28080"));
        assertEquals("https://example.com/base",
                NetworkInterfaceHelper.normalizeUrl("https\\://example.com/base"));
    }

    @Test
    public void testUrlNormalizationKeepsValidUrlUnchanged() {
        assertEquals("http://localhost:28080",
                NetworkInterfaceHelper.normalizeUrl("http://localhost:28080"));
        assertEquals("https://example.com", NetworkInterfaceHelper.normalizeUrl("https://example.com"));
    }

    @Test
    public void testUrlNormalizationHandlesNull() {
        assertEquals(null, NetworkInterfaceHelper.normalizeUrl(null));
    }

    @Test
    public void testGetCurrentSessionIdReturnsDefaultWhenNoTraining() {
        String id = manager.getCurrentSessionId();
        ForgeSession def = manager.findDefaultSession();
        assertNotNull("getCurrentSessionId must return a session when the default exists", id);
        assertEquals("Without any training, the current Forge session must be the default session",
                def.getSessionId(), id);
    }

    @Test
    public void testGetCurrentSessionIdPrefersSessionWithNonIdleTelemetry() {
        ForgeSession completed = manager.createSession("Completed Run", "MLP");
        // A completed (non-IDLE) run owns its historical graph even though no training is
        // currently active; the current session must resolve to it so the Training Monitor
        // loads the historical metrics.
        ForgeSessionManager.TrainingTelemetry telemetry = manager.getTelemetry(completed.getSessionId());
        telemetry.status = "COMPLETED";

        String id = manager.getCurrentSessionId();
        assertEquals("A session with non-IDLE telemetry must be the current session",
                completed.getSessionId(), id);
    }

    @Test
    public void testGetCurrentSessionIdPrefersActiveTrainingSession() {
        ForgeSession training = manager.createSession("Active Training Run", "MLP");
        ForgeSessionManager.TrainingTelemetry telemetry = manager.getTelemetry(training.getSessionId());
        telemetry.status = "RUNNING";

        ForgeSession completed = manager.createSession("Another Completed Run", "MLP");
        ForgeSessionManager.TrainingTelemetry completedTelemetry = manager.getTelemetry(completed.getSessionId());
        completedTelemetry.status = "COMPLETED";

        String id = manager.getCurrentSessionId();
        assertEquals("The actively training session must take priority over a completed session",
                training.getSessionId(), id);
    }
}
