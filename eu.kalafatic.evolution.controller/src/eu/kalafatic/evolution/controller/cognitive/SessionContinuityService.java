package eu.kalafatic.evolution.controller.cognitive;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.model.SessionDescriptor;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.servers.database.DatabaseManager;

/**
 * Durable session registry and continuity service.
 *
 * <p>Stores session descriptors in SQLite (cognitive_sessions), lineage in
 * session_relationships and layered snapshot manifests in
 * <code>&lt;workspace&gt;/shared/sessions/&lt;id&gt;/</code>. Kernel state is
 * never duplicated: a FULL snapshot manifest references the existing
 * IterationMemoryService checkpoint by id. The runtime session registry
 * remains SessionManager; IterationManager remains the sole authority for
 * Darwin iteration transitions.</p>
 */
public class SessionContinuityService {
    private static final Logger LOGGER = Logger.getLogger(SessionContinuityService.class.getName());

    public static final String SNAPSHOT_KIND_KERNEL = "KERNEL";
    public static final String SNAPSHOT_KIND_CONTEXT = "CONTEXT";
    public static final String SNAPSHOT_KIND_FULL = "FULL";

    private static SessionContinuityService instance;

    private final DatabaseManager databaseManager;
    private File sessionsDir;

    private SessionContinuityService() {
        this.databaseManager = new DatabaseManager();
        initStoragePath();
    }

    public static synchronized SessionContinuityService getInstance() {
        if (instance == null) {
            instance = new SessionContinuityService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    private void initStoragePath() {
        try {
            String workspacePath = ProjectModelManager.getWorkspacePath();
            if (workspacePath != null && !workspacePath.isEmpty()) {
                this.sessionsDir = new File(new File(workspacePath, "shared"), "sessions");
            } else {
                this.sessionsDir = new File(new File(System.getProperty("user.home"), ".evo/shared"), "sessions");
            }
        } catch (Exception e) {
            this.sessionsDir = new File(new File(System.getProperty("user.home"), ".evo/shared"), "sessions");
        }
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs();
        }
    }

    // ------------------------------------------------------------------
    // Registry
    // ------------------------------------------------------------------

    /**
     * Registers a new cognitive session. The id is canonical (UUID v4);
     * legacy aliases are preserved in legacy_alias for resolution.
     */
    public SessionDescriptor beginSession(String kind, String ownerUserId, String applicationId,
            String projectRef, String parentSessionId, String metadata) {
        String id = SessionIdService.newSessionId();
        return registerSession(id, null, kind, ownerUserId, applicationId, projectRef, parentSessionId, metadata);
    }

    public SessionDescriptor registerSession(String id, String legacyAlias, String kind, String ownerUserId,
            String applicationId, String projectRef, String parentSessionId, String metadata) {
        SessionDescriptor descriptor = new SessionDescriptor();
        descriptor.setId(id);
        descriptor.setLegacyAlias(legacyAlias);
        descriptor.setKind(kind != null ? kind : SessionDescriptor.KIND_CONVERSATION);
        descriptor.setOwnerUserId(ownerUserId);
        descriptor.setApplicationId(applicationId);
        descriptor.setProjectRef(projectRef);
        descriptor.setStatus(SessionDescriptor.STATUS_ACTIVE);
        descriptor.setStartedAt(System.currentTimeMillis());
        descriptor.setLastActivityAt(descriptor.getStartedAt());
        descriptor.setParentSessionId(parentSessionId);
        descriptor.setMetadata(metadata);
        upsert(descriptor);
        if (parentSessionId != null && !parentSessionId.isEmpty()) {
            relate(parentSessionId, id, "CONTINUES");
        }
        publishEvent(id, RuntimeEventType.SESSION_REGISTERED);
        return descriptor;
    }

    /** Resolves an id (canonical or legacy alias) to its descriptor. */
    public SessionDescriptor resolve(String sessionIdOrAlias) {
        if (sessionIdOrAlias == null) {
            return null;
        }
        SessionDescriptor byId = findByPk(sessionIdOrAlias);
        if (byId != null) {
            return byId;
        }
        return findByAlias(sessionIdOrAlias);
    }

    public SessionDescriptor findByPk(String id) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, legacy_alias, kind, owner_user_id, application_id, project_ref, status, " +
                     "started_at, last_activity_at, ended_at, parent_session_id, metadata FROM cognitive_sessions WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapDescriptor(rs) : null;
            }
        } catch (Exception e) {
            LOGGER.severe("SessionContinuityService: resolve failed: " + e.getMessage());
            return null;
        }
    }

    private SessionDescriptor findByAlias(String alias) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, legacy_alias, kind, owner_user_id, application_id, project_ref, status, " +
                     "started_at, last_activity_at, ended_at, parent_session_id, metadata FROM cognitive_sessions WHERE legacy_alias = ?")) {
            ps.setString(1, alias);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapDescriptor(rs) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private SessionDescriptor mapDescriptor(ResultSet rs) throws java.sql.SQLException {
        SessionDescriptor d = new SessionDescriptor();
        d.setId(rs.getString("id"));
        d.setLegacyAlias(rs.getString("legacy_alias"));
        d.setKind(rs.getString("kind"));
        d.setOwnerUserId(rs.getString("owner_user_id"));
        d.setApplicationId(rs.getString("application_id"));
        d.setProjectRef(rs.getString("project_ref"));
        d.setStatus(rs.getString("status"));
        d.setStartedAt(rs.getLong("started_at"));
        d.setLastActivityAt(rs.getLong("last_activity_at"));
        d.setEndedAt(rs.getLong("ended_at"));
        d.setParentSessionId(rs.getString("parent_session_id"));
        d.setMetadata(rs.getString("metadata"));
        return d;
    }

    private void upsert(SessionDescriptor d) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO cognitive_sessions (id, legacy_alias, kind, owner_user_id, application_id, project_ref, " +
                     "status, started_at, last_activity_at, ended_at, parent_session_id, metadata) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
                     "ON CONFLICT(id) DO UPDATE SET kind=excluded.kind, owner_user_id=excluded.owner_user_id, " +
                     "application_id=excluded.application_id, project_ref=excluded.project_ref, status=excluded.status, " +
                     "last_activity_at=excluded.last_activity_at, ended_at=excluded.ended_at, parent_session_id=excluded.parent_session_id, " +
                     "metadata=excluded.metadata")) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getLegacyAlias());
            ps.setString(3, d.getKind());
            ps.setString(4, d.getOwnerUserId());
            ps.setString(5, d.getApplicationId());
            ps.setString(6, d.getProjectRef());
            ps.setString(7, d.getStatus());
            ps.setLong(8, d.getStartedAt());
            ps.setLong(9, d.getLastActivityAt());
            if (d.getEndedAt() > 0) {
                ps.setLong(10, d.getEndedAt());
            } else {
                ps.setNull(10, java.sql.Types.BIGINT);
            }
            ps.setString(11, d.getParentSessionId());
            ps.setString(12, d.getMetadata());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("SessionContinuityService: upsert failed: " + e.getMessage());
        }
    }

    public void updateStatus(String sessionId, String status) {
        SessionDescriptor d = findByPk(sessionId);
        if (d == null) {
            return;
        }
        d.setStatus(status);
        d.setLastActivityAt(System.currentTimeMillis());
        if (SessionDescriptor.STATUS_COMPLETED.equals(status) || SessionDescriptor.STATUS_FAILED.equals(status)) {
            d.setEndedAt(System.currentTimeMillis());
        }
        upsert(d);
    }

    public void touch(String sessionId) {
        SessionDescriptor d = findByPk(sessionId);
        if (d == null) {
            return;
        }
        d.setLastActivityAt(System.currentTimeMillis());
        upsert(d);
    }

    /**
     * Terminates a session: marks COMPLETED/FAILED and closes the runtime
     * container if one exists. Consolidation hooks fire downstream.
     */
    public void finishSession(String sessionId, boolean failed) {
        updateStatus(sessionId, failed ? SessionDescriptor.STATUS_FAILED : SessionDescriptor.STATUS_COMPLETED);
        try {
            var container = SessionManager.getInstance().getSession(sessionId);
            if (container != null) {
                SessionManager.getInstance().shutdownSession(sessionId);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Startup reconciliation: sessions left ACTIVE are re-evaluated. With a
     * kernel checkpoint present they become PAUSED (resumable); without one
     * they become FAILED. Never auto-resumes execution.
     */
    public int reconcileAfterRestart() {
        int reconciled = 0;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM cognitive_sessions WHERE status IN ('ACTIVE','PAUSED')");
             ResultSet rs = ps.executeQuery()) {
            List<String> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
            for (String id : ids) {
                SessionDescriptor d = findByPk(id);
                if (d == null) {
                    continue;
                }
                boolean hasCheckpoint = hasKernelCheckpoint(id);
                updateStatus(id, hasCheckpoint ? SessionDescriptor.STATUS_PAUSED : SessionDescriptor.STATUS_FAILED);
                reconciled++;
            }
        } catch (Exception e) {
            LOGGER.severe("SessionContinuityService: reconciliation failed: " + e.getMessage());
        }
        return reconciled;
    }

    private boolean hasKernelCheckpoint(String sessionId) {
        try {
            // Reuses the existing IterationMemoryService checkpoint location
            // convention without depending on its runtime state.
            File probe = new File(new File(sessionsDir.getParentFile().getParentFile(), "orchestrator/memory"),
                    "checkpoint_" + sessionId + ".json");
            if (probe.exists()) {
                return true;
            }
            // Fallback: any checkpoint file mentioning the session under the workspace
            File memoryDir = new File(new File(sessionsDir.getParentFile().getParentFile(), "orchestrator"), "memory");
            File[] files = memoryDir.listFiles((dir, name) -> name.startsWith("checkpoint_") && name.contains(sessionId));
            return files != null && files.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Relationships
    // ------------------------------------------------------------------

    public void relate(String fromSessionId, String toSessionId, String kind) {
        if (fromSessionId == null || toSessionId == null) {
            return;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO session_relationships (from_session_id, to_session_id, kind, created_at) VALUES (?,?,?,?)")) {
            ps.setString(1, fromSessionId);
            ps.setString(2, toSessionId);
            ps.setString(3, kind != null ? kind : "RELATED");
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.warning("SessionContinuityService: relate failed: " + e.getMessage());
        }
    }

    public List<JSONObject> listRelationships(String sessionId) {
        List<JSONObject> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT from_session_id, to_session_id, kind, created_at FROM session_relationships " +
                     "WHERE from_session_id = ? OR to_session_id = ? ORDER BY created_at ASC")) {
            ps.setString(1, sessionId);
            ps.setString(2, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject rel = new JSONObject();
                    rel.put("fromSessionId", rs.getString("from_session_id"));
                    rel.put("toSessionId", rs.getString("to_session_id"));
                    rel.put("kind", rs.getString("kind"));
                    rel.put("createdAt", rs.getLong("created_at"));
                    list.add(rel);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("SessionContinuityService: listRelationships failed: " + e.getMessage());
        }
        return list;
    }

    public List<SessionDescriptor> listSessions(String ownerUserId, String applicationId, String projectRef,
            String status, int limit) {
        List<SessionDescriptor> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, legacy_alias, kind, owner_user_id, application_id, project_ref, status, " +
                "started_at, last_activity_at, ended_at, parent_session_id, metadata FROM cognitive_sessions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (ownerUserId != null) {
            sql.append(" AND owner_user_id = ?");
            params.add(ownerUserId);
        }
        if (applicationId != null) {
            sql.append(" AND application_id = ?");
            params.add(applicationId);
        }
        if (projectRef != null) {
            sql.append(" AND project_ref = ?");
            params.add(projectRef);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY last_activity_at DESC LIMIT ").append(limit > 0 ? limit : 100);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDescriptor(rs));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("SessionContinuityService: listSessions failed: " + e.getMessage());
        }
        return list;
    }

    public int countSessions() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cognitive_sessions")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // Snapshots
    // ------------------------------------------------------------------

    /**
     * Creates a layered snapshot manifest. KERNEL/CONTEXT snapshots reference
     * existing state; FULL references all three layers. Kernel checkpoints
     * are never duplicated.
     */
    public JSONObject snapshot(String sessionId, String kind, String conversationRef, String contextRef,
            String checkpointRef, String createdBy) {
        SessionDescriptor d = resolve(sessionId);
        if (d == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        String snapshotId = SessionIdService.newSnapshotId();
        long now = System.currentTimeMillis();

        JSONObject manifest = new JSONObject();
        manifest.put("snapshotId", snapshotId);
        manifest.put("sessionId", d.getId());
        manifest.put("schemaVersion", "1.0");
        manifest.put("createdAt", now);
        manifest.put("createdBy", createdBy);
        JSONObject layers = new JSONObject();
        if (conversationRef != null) layers.put("conversationRef", conversationRef);
        if (contextRef != null) layers.put("contextRef", contextRef);
        if (checkpointRef != null) layers.put("checkpointRef", checkpointRef);
        manifest.put("layers", layers);

        File sessionDir = new File(sessionsDir, d.getId());
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }
        File manifestFile = new File(sessionDir, "snapshot_" + snapshotId + ".json");
        try {
            Files.writeString(manifestFile.toPath(), manifest.toString(2), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.severe("SessionContinuityService: snapshot write failed: " + e.getMessage());
            throw new IllegalStateException("Failed to persist session snapshot", e);
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO session_snapshots (id, session_id, kind, schema_version, ref, created_at, created_by) " +
                     "VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, snapshotId);
            ps.setString(2, d.getId());
            ps.setString(3, kind != null ? kind : SNAPSHOT_KIND_FULL);
            ps.setString(4, "1.0");
            ps.setString(5, manifestFile.getAbsolutePath());
            ps.setLong(6, now);
            ps.setString(7, createdBy);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.warning("SessionContinuityService: snapshot index write failed (manifest intact): " + e.getMessage());
        }
        publishEvent(d.getId(), RuntimeEventType.SESSION_SNAPSHOT_CREATED);
        return manifest;
    }

    public JSONObject getLatestSnapshot(String sessionId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, session_id, kind, schema_version, ref, created_at, created_by " +
                     "FROM session_snapshots WHERE session_id = ? ORDER BY created_at DESC LIMIT 1")) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                JSONObject snapshot = new JSONObject();
                snapshot.put("snapshotId", rs.getString("id"));
                snapshot.put("sessionId", rs.getString("session_id"));
                snapshot.put("kind", rs.getString("kind"));
                snapshot.put("schemaVersion", rs.getString("schema_version"));
                snapshot.put("ref", rs.getString("ref"));
                snapshot.put("createdAt", rs.getLong("created_at"));
                snapshot.put("createdBy", rs.getString("created_by"));
                // Merge the layered manifest so restore() sees the references.
                String ref = rs.getString("ref");
                if (ref != null && !ref.isEmpty()) {
                    File manifestFile = new File(ref);
                    if (manifestFile.exists()) {
                        try {
                            JSONObject manifest = new JSONObject(
                                    Files.readString(manifestFile.toPath(), StandardCharsets.UTF_8));
                            if (manifest.has("layers")) {
                                snapshot.put("layers", manifest.getJSONObject("layers"));
                            }
                        } catch (Exception e) {
                            LOGGER.warning("SessionContinuityService: manifest read failed: " + e.getMessage());
                        }
                    }
                }
                return snapshot;
            }
        } catch (Exception e) {
            LOGGER.warning("SessionContinuityService: getLatestSnapshot failed: " + e.getMessage());
            return null;
        }
    }

    public List<JSONObject> listSnapshots(String sessionId) {
        List<JSONObject> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, session_id, kind, schema_version, ref, created_at, created_by " +
                     "FROM session_snapshots WHERE session_id = ? ORDER BY created_at DESC")) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject snapshot = new JSONObject();
                    snapshot.put("snapshotId", rs.getString("id"));
                    snapshot.put("sessionId", rs.getString("session_id"));
                    snapshot.put("kind", rs.getString("kind"));
                    snapshot.put("schemaVersion", rs.getString("schema_version"));
                    snapshot.put("ref", rs.getString("ref"));
                    snapshot.put("createdAt", rs.getLong("created_at"));
                    snapshot.put("createdBy", rs.getString("created_by"));
                    list.add(snapshot);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("SessionContinuityService: listSnapshots failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Restores the supported session layers: conversation history reference,
     * context snapshot and kernel checkpoint reference. Returns what was
     * restored and what is missing. Kernel state restore itself remains the
     * responsibility of the existing IterationManager restart path.
     */
    public JSONObject restore(String sessionId) {
        SessionDescriptor d = resolve(sessionId);
        JSONObject result = new JSONObject();
        if (d == null) {
            result.put("restored", false);
            result.put("error", "Unknown session: " + sessionId);
            return result;
        }
        JSONObject snapshot = getLatestSnapshot(sessionId);
        JSONArray missing = new JSONArray();
        JSONObject restored = new JSONObject();
        boolean restoredAny = false;

        if (snapshot != null) {
            JSONObject layers = snapshot.optJSONObject("layers");
            if (layers != null) {
                if (layers.has("conversationRef")) {
                    restored.put("conversationRef", layers.getString("conversationRef"));
                    restoredAny = true;
                }
                if (layers.has("contextRef")) {
                    restored.put("contextRef", layers.getString("contextRef"));
                    restoredAny = true;
                }
                if (layers.has("checkpointRef")) {
                    restored.put("checkpointRef", layers.getString("checkpointRef"));
                    restoredAny = true;
                } else if (hasKernelCheckpoint(sessionId)) {
                    restored.put("checkpointRef", "checkpoint_" + sessionId + ".json");
                    restoredAny = true;
                }
            }
        }
        if (!restored.has("conversationRef")) {
            File transcript = new File(new File(sessionsDir.getParentFile(), "conversations"), sessionId + ".jsonl");
            if (transcript.exists()) {
                restored.put("conversationRef", transcript.getName());
                restoredAny = true;
            } else {
                missing.put("conversation history");
            }
        }
        if (!restored.has("checkpointRef") && !hasKernelCheckpoint(sessionId)) {
            missing.put("kernel checkpoint");
        }
        if (!restored.has("contextRef")) {
            missing.put("context snapshot");
        }
        if (d.getStatus().equals(SessionDescriptor.STATUS_ACTIVE)
                || d.getStatus().equals(SessionDescriptor.STATUS_FAILED)) {
            updateStatus(sessionId, SessionDescriptor.STATUS_PAUSED);
        }
        result.put("restored", restoredAny);
        result.put("restoredAt", System.currentTimeMillis());
        result.put("sessionId", d.getId());
        result.put("status", d.getStatus());
        result.put("layers", restored);
        result.put("missing", missing);
        publishEvent(d.getId(), RuntimeEventType.SESSION_RESTORED);
        return result;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    private void publishEvent(String sessionId, RuntimeEventType type) {
        try {
            var container = SessionManager.getInstance().getSession(sessionId);
            if (container != null && container.getEventBus() != null) {
                container.getEventBus().publish(new RuntimeEvent(type, sessionId, "SessionContinuityService", sessionId));
            }
        } catch (Exception ignored) {
        }
    }
}
