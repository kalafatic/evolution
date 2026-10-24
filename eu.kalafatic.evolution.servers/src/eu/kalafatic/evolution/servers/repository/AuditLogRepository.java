package eu.kalafatic.evolution.servers.repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.AuditLogEntry;

public class AuditLogRepository {
    private final DatabaseManager dbManager;

    public AuditLogRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(AuditLogEntry entry) throws SQLException {
        String sql = "INSERT INTO audit_log (timestamp, actor_user_id, actor_username, action, resource_type, resource_id, outcome, details) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, entry.getTimestamp() != null ? entry.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (entry.getActorUserId() != null) {
                pstmt.setLong(2, entry.getActorUserId());
            } else {
                pstmt.setNull(2, Types.BIGINT);
            }
            pstmt.setString(3, entry.getActorUsername());
            pstmt.setString(4, entry.getAction());
            pstmt.setString(5, entry.getResourceType());
            pstmt.setString(6, entry.getResourceId());
            pstmt.setString(7, entry.getOutcome());
            pstmt.setString(8, entry.getDetails());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entry.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public List<AuditLogEntry> findAll(int limit) throws SQLException {
        List<AuditLogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY id DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEntry(rs));
                }
            }
        }
        return list;
    }

    private AuditLogEntry mapResultSetToEntry(ResultSet rs) throws SQLException {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setId(rs.getLong("id"));
        String ts = rs.getString("timestamp");
        if (ts != null) {
            entry.setTimestamp(LocalDateTime.parse(ts, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        long actorId = rs.getLong("actor_user_id");
        if (!rs.wasNull()) {
            entry.setActorUserId(actorId);
        }
        entry.setActorUsername(rs.getString("actor_username"));
        entry.setAction(rs.getString("action"));
        entry.setResourceType(rs.getString("resource_type"));
        entry.setResourceId(rs.getString("resource_id"));
        entry.setOutcome(rs.getString("outcome"));
        entry.getDetails();
        entry.setDetails(rs.getString("details"));
        return entry;
    }
}
