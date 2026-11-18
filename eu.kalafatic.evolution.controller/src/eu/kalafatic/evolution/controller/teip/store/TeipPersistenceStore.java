package eu.kalafatic.evolution.controller.teip.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.teip.dto.TeipEvent;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.utils.log.Log;

/**
 * Persisted TEIP state (TEIP 1.1 §7): the idempotencyKey→executionId relation and
 * sequenced execution events MUST survive server restarts. Uses the existing EVO
 * SQLite DatabaseManager (data/evolution.db). All operations are best-effort: on
 * any persistence failure the store degrades to a no-op and the in-memory runtime
 * structures remain authoritative, so a database problem never breaks the API.
 */
public class TeipPersistenceStore {

    private static final TeipPersistenceStore INSTANCE = new TeipPersistenceStore();

    private volatile boolean schemaEnsured = false;
    private volatile boolean available = true;

    private TeipPersistenceStore() {}

    public static TeipPersistenceStore getInstance() {
        return INSTANCE;
    }

    public boolean isAvailable() {
        return available;
    }

    private synchronized void ensureSchema() {
        if (schemaEnsured) return;
        schemaEnsured = true;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS teip_executions (" +
                        "execution_id TEXT PRIMARY KEY," +
                        "idempotency_key TEXT," +
                        "correlation_id TEXT," +
                        "status TEXT NOT NULL," +
                        "created_at TEXT NOT NULL" +
                        ");");
                stmt.execute("CREATE TABLE IF NOT EXISTS teip_idempotency (" +
                        "idempotency_key TEXT PRIMARY KEY," +
                        "execution_id TEXT NOT NULL," +
                        "created_at TEXT NOT NULL" +
                        ");");
                stmt.execute("CREATE TABLE IF NOT EXISTS teip_events (" +
                        "event_id TEXT PRIMARY KEY," +
                        "execution_id TEXT NOT NULL," +
                        "sequence INTEGER NOT NULL," +
                        "event_type TEXT NOT NULL," +
                        "correlation_id TEXT," +
                        "timestamp TEXT NOT NULL," +
                        "data_json TEXT NOT NULL" +
                        ");");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_teip_events_exec_seq ON teip_events(execution_id, sequence);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_teip_executions_status ON teip_executions(status);");
            }
        } catch (Throwable t) {
            available = false;
            logWarn("TEIP persistence unavailable, degrading to in-memory operation: " + t.getMessage());
        }
    }

    public Optional<String> findExecutionIdByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        ensureSchema();
        if (!available) return Optional.empty();
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT execution_id FROM teip_idempotency WHERE idempotency_key = ?")) {
                pstmt.setString(1, idempotencyKey);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(rs.getString("execution_id"));
                    }
                }
            }
        } catch (SQLException e) {
            logWarn("Failed to read idempotency mapping: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void saveExecution(String executionId, String idempotencyKey, String correlationId, String status) {
        ensureSchema();
        if (!available) return;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO teip_executions (execution_id, idempotency_key, correlation_id, status, created_at) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, executionId);
                pstmt.setString(2, idempotencyKey);
                pstmt.setString(3, correlationId);
                pstmt.setString(4, status);
                pstmt.setString(5, Instant.now().toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logWarn("Failed to persist execution row: " + e.getMessage());
        }
    }

    public void updateExecutionStatus(String executionId, String status) {
        ensureSchema();
        if (!available) return;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE teip_executions SET status = ? WHERE execution_id = ?")) {
                pstmt.setString(1, status);
                pstmt.setString(2, executionId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logWarn("Failed to update execution status: " + e.getMessage());
        }
    }

    public void saveIdempotency(String idempotencyKey, String executionId) {
        ensureSchema();
        if (!available) return;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO teip_idempotency (idempotency_key, execution_id, created_at) VALUES (?, ?, ?)")) {
                pstmt.setString(1, idempotencyKey);
                pstmt.setString(2, executionId);
                pstmt.setString(3, Instant.now().toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logWarn("Failed to persist idempotency mapping: " + e.getMessage());
        }
    }

    public void saveEvent(TeipEvent event) {
        ensureSchema();
        if (!available) return;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR IGNORE INTO teip_events (event_id, execution_id, sequence, event_type, correlation_id, timestamp, data_json) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                pstmt.setString(1, event.getEventId());
                pstmt.setString(2, event.getExecutionId());
                pstmt.setLong(3, event.getSequence());
                pstmt.setString(4, event.getType());
                pstmt.setString(5, event.getCorrelationId());
                pstmt.setString(6, event.getTimestamp());
                pstmt.setString(7, new JSONObject(event.getData()).toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logWarn("Failed to persist event: " + e.getMessage());
        }
    }

    /**
     * Hydrates the persisted event stream for an execution (restart replay support).
     */
    public List<TeipEvent> loadEvents(String executionId) {
        ensureSchema();
        List<TeipEvent> events = new ArrayList<>();
        if (!available) return events;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT event_id, execution_id, sequence, event_type, correlation_id, timestamp, data_json FROM teip_events WHERE execution_id = ? ORDER BY sequence ASC")) {
                pstmt.setString(1, executionId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> data = new LinkedHashMap<>();
                        try {
                            JSONObject dataJson = new JSONObject(rs.getString("data_json"));
                            data = jsonToMap(dataJson);
                        } catch (Throwable ignored) {}
                        TeipEvent event = new TeipEvent(
                                rs.getString("event_id"),
                                rs.getString("execution_id"),
                                rs.getLong("sequence"),
                                rs.getString("event_type"),
                                rs.getString("correlation_id"),
                                data
                        );
                        event.setTimestamp(rs.getString("timestamp"));
                        events.add(event);
                    }
                }
            }
        } catch (SQLException e) {
            logWarn("Failed to hydrate events: " + e.getMessage());
        }
        return events;
    }

    /**
     * Returns [executionId, idempotencyKey, correlationId] rows whose status is not terminal.
     */
    public List<String[]> loadOpenExecutions() {
        ensureSchema();
        List<String[]> rows = new ArrayList<>();
        if (!available) return rows;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT execution_id, idempotency_key, correlation_id FROM teip_executions WHERE status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                                rs.getString("execution_id"),
                                rs.getString("idempotency_key"),
                                rs.getString("correlation_id")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            logWarn("Failed to load open executions: " + e.getMessage());
        }
        return rows;
    }

    /**
     * Returns [executionId, idempotencyKey, correlationId, status] rows for all persisted executions.
     */
    public List<String[]> loadAllExecutions() {
        ensureSchema();
        List<String[]> rows = new ArrayList<>();
        if (!available) return rows;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT execution_id, idempotency_key, correlation_id, status FROM teip_executions")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                                rs.getString("execution_id"),
                                rs.getString("idempotency_key"),
                                rs.getString("correlation_id"),
                                rs.getString("status")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            logWarn("Failed to load persisted executions: " + e.getMessage());
        }
        return rows;
    }

    public void clearAll() {
        ensureSchema();
        if (!available) return;
        try {
            DatabaseManager db = new DatabaseManager();
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM teip_events;");
                stmt.execute("DELETE FROM teip_idempotency;");
                stmt.execute("DELETE FROM teip_executions;");
            }
        } catch (SQLException e) {
            logWarn("Failed to clear persisted TEIP state: " + e.getMessage());
        }
    }

    private static Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (json == null) return map;
        java.util.Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = String.valueOf(keys.next());
            map.put(key, json.opt(key));
        }
        return map;
    }

    private static void logWarn(String message) {
        try {
            Log.log("[TEIP][PERSISTENCE] " + message);
        } catch (Throwable ignored) {}
    }
}
