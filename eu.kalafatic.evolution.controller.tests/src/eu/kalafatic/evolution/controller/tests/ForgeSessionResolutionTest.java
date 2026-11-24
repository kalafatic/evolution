package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

        // A missing session must fall back to an existing session, never create a new one.
        assertNotNull("Missing session lookup must return the default session, not null", resolved);
        assertEquals("Missing session lookup must not create a new session",
                before, orchestrator.getForgeSessions().size());
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
}
