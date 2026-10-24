package eu.kalafatic.evolution.servers.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.Role;

public class RoleRepository {
    private final DatabaseManager dbManager;

    public RoleRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Role role) throws SQLException {
        String sql = "INSERT INTO roles (name, description, system_role, enabled) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, role.getName());
            pstmt.setString(2, role.getDescription());
            pstmt.setBoolean(3, role.isSystemRole());
            pstmt.setBoolean(4, role.isEnabled());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    role.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public Optional<Role> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM roles WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRole(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Role> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM roles WHERE name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRole(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Role> findAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM roles";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        }
        return roles;
    }

    public void update(Role role) throws SQLException {
        String sql = "UPDATE roles SET name = ?, description = ?, enabled = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role.getName());
            pstmt.setString(2, role.getDescription());
            pstmt.setBoolean(3, role.isEnabled());
            pstmt.setLong(4, role.getId());
            pstmt.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM roles WHERE id = ? AND system_role = 0";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getLong("id"));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));
        role.setSystemRole(rs.getBoolean("system_role"));
        role.setEnabled(rs.getBoolean("enabled"));
        return role;
    }
}
