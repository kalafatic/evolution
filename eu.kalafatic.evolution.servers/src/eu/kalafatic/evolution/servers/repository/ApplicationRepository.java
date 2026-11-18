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
import eu.kalafatic.evolution.servers.model.ApplicationIdentity;

public class ApplicationRepository {
    private final DatabaseManager dbManager;

    public ApplicationRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(ApplicationIdentity app) throws SQLException {
        String sql = "INSERT INTO applications (application_id, name, kind, owner_user_id, enabled, created_at, updated_at, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            pstmt.setString(1, app.getApplicationId());
            pstmt.setString(2, app.getName());
            pstmt.setString(3, app.getKind());
            if (app.getOwnerUserId() != null) pstmt.setLong(4, app.getOwnerUserId()); else pstmt.setNull(4, java.sql.Types.BIGINT);
            pstmt.setBoolean(5, app.isEnabled());
            pstmt.setString(6, app.getCreatedAt() != null ? app.getCreatedAt() : now);
            pstmt.setString(7, now);
            pstmt.setString(8, app.getDescription());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) app.setId(keys.getLong(1));
            }
        }
    }

    public Optional<ApplicationIdentity> findByApplicationId(String applicationId) throws SQLException {
        String sql = "SELECT * FROM applications WHERE application_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, applicationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<ApplicationIdentity> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM applications WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<ApplicationIdentity> findAll() throws SQLException {
        List<ApplicationIdentity> list = new ArrayList<>();
        String sql = "SELECT * FROM applications ORDER BY application_id";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void update(ApplicationIdentity app) throws SQLException {
        String sql = "UPDATE applications SET name = ?, kind = ?, owner_user_id = ?, enabled = ?, updated_at = ?, description = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, app.getName());
            pstmt.setString(2, app.getKind());
            if (app.getOwnerUserId() != null) pstmt.setLong(3, app.getOwnerUserId()); else pstmt.setNull(3, java.sql.Types.BIGINT);
            pstmt.setBoolean(4, app.isEnabled());
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(6, app.getDescription());
            pstmt.setLong(7, app.getId());
            pstmt.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM applications WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    private ApplicationIdentity map(ResultSet rs) throws SQLException {
        ApplicationIdentity app = new ApplicationIdentity();
        app.setId(rs.getLong("id"));
        app.setApplicationId(rs.getString("application_id"));
        app.setName(rs.getString("name"));
        app.setKind(rs.getString("kind"));
        long owner = rs.getLong("owner_user_id");
        app.setOwnerUserId(rs.wasNull() ? null : owner);
        app.setEnabled(rs.getBoolean("enabled"));
        app.setCreatedAt(rs.getString("created_at"));
        app.setUpdatedAt(rs.getString("updated_at"));
        app.setDescription(rs.getString("description"));
        return app;
    }
}
