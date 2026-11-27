package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.EvolutionKernelContext;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Regression tests for the FORGE session lifecycle contract:
 *
 * <ul>
 *   <li>A missing session must never be silently substituted with another session.</li>
 *   <li>The default session is selected only through the explicit
 *       {@link ForgeSessionManager#findDefaultSession()} API.</li>
 *   <li>Session creation produces a canonical UUID identity that is restorable.</li>
 *   <li>Kernel context identity is stable within a task context.</li>
 *   <li>Forge sessions remain isolated (IDs, uiState, telemetry).</li>
 * </ul>
 */
public class ForgeSessionLifecycleRegressionTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ForgeSessionManager manager;
    private Orchestrator orchestrator;

    @Before
    public void setUp() {
        manager = ForgeSessionManager.getInstance();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setId("test-orch-lifecycle");
        manager.initialize(orchestrator);
    }

    // --- Test A: existing session restoration ---------------------------------
    @Test
    public void testExistingSessionRestorationReturnsExactSession() {
        ForgeSession created = manager.createSession("Restore Target", "MLP");
        assertNotNull(created);
        String id = created.getSessionId();

        ForgeSession restored = manager.findSessionById(id);
        assertNotNull("Existing session must be restorable by its canonical ID", restored);
        assertEquals("Restored session ID must remain unchanged", id, restored.getSessionId());
        assertEquals("Restored session name must match", "Restore Target", restored.getName());
    }

    // --- Test B: missing session restoration ----------------------------------
    @Test
    public void testMissingSessionRestorationDoesNotSubstituteDefault() {
        ForgeSession defaultSession = manager.findDefaultSession();
        assertNotNull(defaultSession);
        String defaultId = defaultSession.getSessionId();
        int before = manager.getSessions().size();

        ForgeSession missing = manager.findSession("99999999-9999-9999-9999-999999999999");
        assertNull("Missing RESTORE must not silently select the default session", missing);
        assertEquals("Missing RESTORE must not create a session", before, manager.getSessions().size());
        assertEquals("Default session identity must be unchanged",
                defaultId, manager.findDefaultSession().getSessionId());
    }

    // --- Test B2: foreign AI Commander namespace ID ----------------------------
    @Test
    public void testForeignChatSessionIdDoesNotResolve() {
        // Timestamp-based AI Commander chat session IDs are a distinct namespace.
        ForgeSession resolved = manager.findSession("20261126-1058_task");
        assertNull("AI Commander chat session ID must not resolve to a ForgeSession", resolved);
    }

    // --- Test C: explicit default session selection ----------------------------
    @Test
    public void testExplicitDefaultSessionSelection() {
        ForgeSession def = manager.findDefaultSession();
        assertNotNull("Explicit default selection must return the seeded default session", def);
        assertEquals("Default session must be 'Self-Evo Forging'", "Self-Evo Forging", def.getName());
        assertNotNull(def.getSessionId());
    }

    // --- Test D: new session creation ------------------------------------------
    @Test
    public void testNewSessionCreationGeneratesCanonicalIdAndIsRestorable() {
        ForgeSession def = manager.findDefaultSession();
        String defaultId = def.getSessionId();

        ForgeSession created = manager.createSession("Fresh Session", "CNN");
        assertNotNull(created);
        String newId = created.getSessionId();
        assertNotNull(newId);
        assertTrue("New session ID must be a UUID, got: " + newId,
                newId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));

        ForgeSession restored = manager.findSessionById(newId);
        assertNotNull("New session must be restorable by its canonical ID", restored);
        assertEquals(newId, restored.getSessionId());

        assertEquals("Default session must remain unchanged", defaultId, manager.findDefaultSession().getSessionId());
    }

    // --- Test E: context identity ----------------------------------------------
    @Test
    public void testContextIdentityIsStableWithinTaskContext() {
        String sid = "ctx-lifecycle-" + System.nanoTime();
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sid);
        File root = tmp.getRoot();
        TaskContext ctx = new TaskContext(orchestrator, root);
        ctx.setSessionId(sid);
        try {
            EvolutionKernelContext k1 = ctx.getKernelContext();
            EvolutionKernelContext k2 = ctx.getKernelContext();
            assertNotNull(k1);
            assertSame("Repeated context requests must return the same instance", k1, k2);
        } finally {
            SessionManager.getInstance().shutdownSession(sid);
        }
    }

    // --- Test F: session isolation ---------------------------------------------
    @Test
    public void testSessionIsolation() {
        ForgeSession s1 = manager.createSession("Isolated A", "MLP");
        ForgeSession s2 = manager.createSession("Isolated B", "CNN");
        assertNotEquals("Session IDs must differ", s1.getSessionId(), s2.getSessionId());

        // Use a non-structural uiState key so no architecture regeneration is
        // triggered, and assert on the persisted hyperparameters string (no
        // org.json dependency in the test bundle).
        manager.updateUiState(s1.getSessionId(), "isolationMarker", 111);
        manager.updateUiState(s2.getSessionId(), "isolationMarker", 222);

        String hp1 = s1.getModelState().getHyperparameters();
        String hp2 = s2.getModelState().getHyperparameters();
        assertTrue("Session A uiState must contain its own marker",
                hp1 != null && hp1.contains("\"isolationMarker\":111"));
        assertTrue("Session B uiState must contain its own marker",
                hp2 != null && hp2.contains("\"isolationMarker\":222"));
        assertFalse("Session A must not contain Session B's marker value",
                hp1 != null && hp1.contains("\"isolationMarker\":222"));
        assertFalse("Session B must not contain Session A's marker value",
                hp2 != null && hp2.contains("\"isolationMarker\":111"));

        ForgeSessionManager.TrainingTelemetry t1 = manager.getTelemetry(s1.getSessionId());
        ForgeSessionManager.TrainingTelemetry t2 = manager.getTelemetry(s2.getSessionId());
        assertNotSame("Telemetry must be isolated per session", t1, t2);
    }

    // --- Test H (partial): persistence does not fabricate a session -------------
    @Test
    public void testPersistStateForUnknownIdDoesNotFabricateSession() {
        int before = manager.getSessions().size();
        // persistSessionState for an ID that matches no session must not create one.
        manager.persistSessionState("09da2ed8-c78a-4463-b490-fc9322b2d49f");
        assertEquals("Persisting state for an unknown ID must not fabricate a session",
                before, manager.getSessions().size());
        assertNull("Unknown ID must still not resolve after persist",
                manager.findSessionById("09da2ed8-c78a-4463-b490-fc9322b2d49f"));
    }
}
