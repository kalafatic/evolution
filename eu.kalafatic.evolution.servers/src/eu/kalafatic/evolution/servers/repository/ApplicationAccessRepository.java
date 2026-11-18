package eu.kalafatic.evolution.servers.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.ApplicationAccess;

public class ApplicationAccessRepository {
    private final DatabaseManager dbManager;

    public ApplicationAccessRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(ApplicationAccess access) throws SQLException {
        String sql = "INSERT INTO application_tokens (application_id, user_id, token_hash, scopes_json, revoked, issued_at, expires_at, last_used_at, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, access.getApplicationId());
            pstmt.setLong(2, access.getUserId());
            pstmt.setString(3, access.getTokenHash());
            pstmt.setString(4, access.getScopesJson());
            pstmt.setBoolean(5, access.isRevoked());
            pstmt.setString(6, access.getIssuedAt() != null ? access.getIssuedAt() : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(7, access.getExpiresAt());
            pstmt.setString(8, access.getLastUsedAt());
            pstmt.setString(9, access.getDescription());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) access.setId(keys.getLong(1));
            }
        }
    }

    public Optional<ApplicationAccess> findByTokenHash(String tokenHash) throws SQLException {
        String sql = "SELECT * FROM application_tokens WHERE token_hash = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tokenHash);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<ApplicationAccess> findByApplicationId(Long applicationId) throws SQLException {
        List<ApplicationAccess> list = new ArrayList<>();
        String sql = "SELECT * FROM application_tokens WHERE application_id = ? ORDER BY issued_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, applicationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<ApplicationAccess> findAll() throws SQLException {
        List<ApplicationAccess> list = new ArrayList<>();
        String sql = "SELECT * FROM application_tokens ORDER BY issued_at DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void revoke(Long id) throws SQLException {
        String sql = "UPDATE application_tokens SET revoked = 1 WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    public void updateLastUsed(Long id, String lastUsedAt) throws SQLException {
        String sql = "UPDATE application_tokens SET last_used_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lastUsedAt);
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM application_tokens WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    private ApplicationAccess map(ResultSet rs) throws SQLException {
        ApplicationAccess access = new ApplicationAccess();
        access.setId(rs.getLong("id"));
        access.setApplicationId(rs.getLong("application_id"));
        access.setUserId(rs.getLong("user_id"));
        access.setTokenHash(rs.getString("token_hash"));
        access.setScopesJson(rs.getString("scopes_json"));
        access.setRevoked(rs.getBoolean("revoked"));
        access.setIssuedAt(rs.getString("issued_at"));
        access.setExpiresAt(rs.getString("expires_at"));
        access.setLastUsedAt(rs.getString("last_used_at"));
        access.setDescription(rs.getString("description"));
        return access;
    }
}
