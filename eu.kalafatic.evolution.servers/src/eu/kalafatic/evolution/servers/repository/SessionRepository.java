package eu.kalafatic.evolution.servers.repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.Session;

public class SessionRepository {
    private final DatabaseManager dbManager;

    public SessionRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Session session) throws SQLException {
        String sql = "INSERT INTO sessions (session_id, user_id, created_at, last_access, client_ip, workflow_type, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, session.getSessionId());
            pstmt.setLong(2, session.getUserId());
            pstmt.setString(3, session.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(4, session.getLastAccess().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(5, session.getClientIp());
            pstmt.setString(6, session.getWorkflowType());
            pstmt.setString(7, session.getMetadata());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    session.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public Optional<Session> findBySessionId(String sessionId) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE session_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void updateLastAccess(String sessionId, LocalDateTime lastAccess) throws SQLException {
        String sql = "UPDATE sessions SET last_access = ? WHERE session_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lastAccess.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(2, sessionId);
            pstmt.executeUpdate();
        }
    }

    public void updateWorkflowType(String sessionId, String workflowType) throws SQLException {
        String sql = "UPDATE sessions SET workflow_type = ? WHERE session_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workflowType);
            pstmt.setString(2, sessionId);
            pstmt.executeUpdate();
        }
    }

    public void deleteBySessionId(String sessionId) throws SQLException {
        String sql = "DELETE FROM sessions WHERE session_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.executeUpdate();
        }
    }

    public void deleteExpired(LocalDateTime threshold) throws SQLException {
        String sql = "DELETE FROM sessions WHERE last_access < ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, threshold.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.executeUpdate();
        }
    }

    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setId(rs.getLong("id"));
        session.setSessionId(rs.getString("session_id"));
        session.setUserId(rs.getLong("user_id"));
        session.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        session.setLastAccess(LocalDateTime.parse(rs.getString("last_access"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        session.setClientIp(rs.getString("client_ip"));
        session.setWorkflowType(rs.getString("workflow_type"));
        session.setMetadata(rs.getString("metadata"));
        return session;
    }
}
