package eu.kalafatic.evolution.servers.repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.User;

public class UserRepository {
    private final DatabaseManager dbManager;

    public UserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (username, display_name, password_hash, role, enabled, created_at, updated_at, last_login_at, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            pstmt.setString(3, user.getPasswordHash() != null ? user.getPasswordHash() : "");
            pstmt.setString(4, user.getRole() != null ? user.getRole() : "USER");
            pstmt.setBoolean(5, user.isEnabled());
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            pstmt.setString(6, user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : now);
            pstmt.setString(7, user.getUpdatedAt() != null ? user.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : now);
            pstmt.setString(8, user.getLastLoginAt() != null ? user.getLastLoginAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            pstmt.setString(9, user.getDescription());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, display_name = ?, password_hash = ?, role = ?, enabled = ?, updated_at = ?, last_login_at = ?, description = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getDisplayName());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole());
            pstmt.setBoolean(5, user.isEnabled());
            pstmt.setString(6, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(7, user.getLastLoginAt() != null ? user.getLastLoginAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            pstmt.setString(8, user.getDescription());
            pstmt.setLong(9, user.getId());
            pstmt.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    // Role mapping helpers
    public List<Long> getUserRoleIds(Long userId) throws SQLException {
        List<Long> roleIds = new ArrayList<>();
        String sql = "SELECT role_id FROM user_roles WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    roleIds.add(rs.getLong("role_id"));
                }
            }
        }
        return roleIds;
    }

    public void assignUserRole(Long userId, Long roleId, Long assignedBy) throws SQLException {
        String sql = "INSERT OR IGNORE INTO user_roles (user_id, role_id, assigned_at, assigned_by) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);
            pstmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (assignedBy != null) pstmt.setLong(4, assignedBy); else pstmt.setNull(4, Types.BIGINT);
            pstmt.executeUpdate();
        }
    }

    public void removeUserRole(Long userId, Long roleId) throws SQLException {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);
            pstmt.executeUpdate();
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        try { user.setDisplayName(rs.getString("display_name")); } catch (SQLException e) {}
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setEnabled(rs.getBoolean("enabled"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) user.setCreatedAt(LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        try {
            String updatedAt = rs.getString("updated_at");
            if (updatedAt != null) user.setUpdatedAt(LocalDateTime.parse(updatedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            String lastLogin = rs.getString("last_login_at");
            if (lastLogin != null) user.setLastLoginAt(LocalDateTime.parse(lastLogin, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            user.setDescription(rs.getString("description"));
        } catch (SQLException e) {}
        return user;
    }
}
