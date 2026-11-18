package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.controller.cognitive.SessionContinuityService;
import eu.kalafatic.evolution.controller.cognitive.model.SessionDescriptor;

/**
 * Session registry and continuity: canonical ids, legacy alias resolution,
 * relationships, layered snapshots, restore and restart reconciliation.
 */
public class SessionContinuityServiceTest {

    private SessionContinuityService service = SessionContinuityService.getInstance();

    @Test
    public void beginSessionRegistersCanonicalDescriptor() {
        SessionDescriptor descriptor = service.beginSession(SessionDescriptor.KIND_TASK,
                "admin", "tryst", "/tmp/project-" + System.nanoTime(), null, null);
        assertNotNull(descriptor.getId());
        assertEquals(SessionDescriptor.STATUS_ACTIVE, descriptor.getStatus());
        assertEquals("admin", descriptor.getOwnerUserId());
        assertEquals("tryst", descriptor.getApplicationId());

        SessionDescriptor resolved = service.resolve(descriptor.getId());
        assertNotNull(resolved);
        assertEquals(descriptor.getId(), resolved.getId());
    }

    @Test
    public void legacyAliasResolution() {
        String legacyId = "legacy-session-" + System.nanoTime();
        SessionDescriptor descriptor = service.registerSession(
                eu.kalafatic.evolution.controller.cognitive.SessionIdService.newSessionId(),
                legacyId, SessionDescriptor.KIND_CONVERSATION, "admin", "evo", null, null, null);
        SessionDescriptor resolved = service.resolve(legacyId);
        assertNotNull("Legacy alias must resolve to the canonical session", resolved);
        assertEquals(descriptor.getId(), resolved.getId());
    }

    @Test
    public void sessionRelationshipsAreRecorded() {
        SessionDescriptor parent = service.beginSession(SessionDescriptor.KIND_CONVERSATION,
                "admin", "evo", null, null, null);
        SessionDescriptor child = service.beginSession(SessionDescriptor.KIND_TASK,
                "admin", "evo", null, parent.getId(), null);
        assertEquals(parent.getId(), child.getParentSessionId());
        boolean foundContinues = service.listRelationships(child.getId()).stream()
                .anyMatch(rel -> "CONTINUES".equals(rel.optString("kind")));
        assertTrue("Parent linkage must create a CONTINUES relationship", foundContinues);
    }

    @Test
    public void snapshotAndRestoreRoundTrip() {
        SessionDescriptor session = service.beginSession(SessionDescriptor.KIND_CONVERSATION,
                "admin", "evo", null, null, null);
        JSONObject manifest = service.snapshot(session.getId(), SessionContinuityService.SNAPSHOT_KIND_FULL,
                session.getId(), "ctx-ref", "checkpoint-ref", "admin");
        assertNotNull(manifest.optString("snapshotId"));
        assertEquals("checkpoint-ref", manifest.getJSONObject("layers").getString("checkpointRef"));

        JSONObject latest = service.getLatestSnapshot(session.getId());
        assertNotNull(latest);
        assertEquals(manifest.optString("snapshotId"), latest.optString("snapshotId"));

        JSONObject restored = service.restore(session.getId());
        assertTrue("Restore must report restored layers", restored.optBoolean("restored"));
        assertNotNull(restored.optJSONObject("layers").optString("conversationRef"));
    }

    @Test
    public void finishSessionTransitionsStatus() {
        SessionDescriptor session = service.beginSession(SessionDescriptor.KIND_TASK,
                "admin", "tryst", null, null, null);
        service.finishSession(session.getId(), true);
        assertEquals(SessionDescriptor.STATUS_FAILED,
                service.resolve(session.getId()).getStatus());
    }

    @Test
    public void reconcileAfterRestartMarksStaleSessions() {
        SessionDescriptor session = service.beginSession(SessionDescriptor.KIND_CONVERSATION,
                "admin", "evo", null, null, null);
        service.reconcileAfterRestart();
        SessionDescriptor reconciled = service.resolve(session.getId());
        assertNotNull(reconciled);
        // No kernel checkpoint exists for this synthetic session, so the
        // reconciliation must mark it FAILED rather than silently resuming.
        assertEquals(SessionDescriptor.STATUS_FAILED, reconciled.getStatus());
    }
}
