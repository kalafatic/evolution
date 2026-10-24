package eu.kalafatic.evolution.servers.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.Permission;

public class PermissionRepository {
    private final DatabaseManager dbManager;

    public PermissionRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Permission permission) throws SQLException {
        String sql = "INSERT INTO permissions (permission_key, name, description, resource_type, action) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, permission.getPermissionKey());
            pstmt.setString(2, permission.getName());
            pstmt.setString(3, permission.getDescription());
            pstmt.setString(4, permission.getResourceType());
            pstmt.setString(5, permission.getAction());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    permission.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public Optional<Permission> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM permissions WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPermission(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Permission> findByKey(String key) throws SQLException {
        String sql = "SELECT * FROM permissions WHERE permission_key = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPermission(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Permission> findAll() throws SQLException {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT * FROM permissions";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                permissions.add(mapResultSetToPermission(rs));
            }
        }
        return permissions;
    }

    public List<Permission> findByRoleId(Long roleId) throws SQLException {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT p.* FROM permissions p " +
                     "JOIN role_permissions rp ON p.id = rp.permission_id " +
                     "WHERE rp.role_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapResultSetToPermission(rs));
                }
            }
        }
        return permissions;
    }

    private Permission mapResultSetToPermission(ResultSet rs) throws SQLException {
        Permission permission = new Permission();
        permission.setId(rs.getLong("id"));
        permission.setPermissionKey(rs.getString("permission_key"));
        permission.setName(rs.getString("name"));
        permission.setDescription(rs.getString("description"));
        permission.setResourceType(rs.getString("resource_type"));
        permission.setAction(rs.getString("action"));
        return permission;
    }
}
