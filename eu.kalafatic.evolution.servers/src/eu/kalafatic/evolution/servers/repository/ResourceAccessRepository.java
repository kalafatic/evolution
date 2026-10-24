package eu.kalafatic.evolution.servers.repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.ResourceAccessPolicy;

public class ResourceAccessRepository {
    private final DatabaseManager dbManager;

    public ResourceAccessRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(ResourceAccessPolicy policy) throws SQLException {
        String sql = "INSERT INTO resource_access (principal_type, principal_id, resource_id, action, effect, created_at, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, policy.getPrincipalType());
            pstmt.setLong(2, policy.getPrincipalId());
            pstmt.setLong(3, policy.getResourceId());
            pstmt.setString(4, policy.getAction());
            pstmt.setString(5, policy.getEffect());
            pstmt.setString(6, policy.getCreatedAt() != null ? policy.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (policy.getCreatedBy() != null) {
                pstmt.setLong(7, policy.getCreatedBy());
            } else {
                pstmt.setNull(7, Types.BIGINT);
            }
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    policy.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public List<ResourceAccessPolicy> findAll() throws SQLException {
        List<ResourceAccessPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM resource_access";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPolicy(rs));
            }
        }
        return list;
    }

    public List<ResourceAccessPolicy> findByResourceId(Long resourceId) throws SQLException {
        List<ResourceAccessPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM resource_access WHERE resource_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, resourceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPolicy(rs));
                }
            }
        }
        return list;
    }

    public List<ResourceAccessPolicy> findByPrincipal(String principalType, Long principalId) throws SQLException {
        List<ResourceAccessPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM resource_access WHERE principal_type = ? AND principal_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, principalType);
            pstmt.setLong(2, principalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPolicy(rs));
                }
            }
        }
        return list;
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM resource_access WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    private ResourceAccessPolicy mapResultSetToPolicy(ResultSet rs) throws SQLException {
        ResourceAccessPolicy policy = new ResourceAccessPolicy();
        policy.setId(rs.getLong("id"));
        policy.setPrincipalType(rs.getString("principal_type"));
        policy.setPrincipalId(rs.getLong("principal_id"));
        policy.setResourceId(rs.getLong("resource_id"));
        policy.setAction(rs.getString("action"));
        policy.setEffect(rs.getString("effect"));
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            policy.setCreatedAt(LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) {
            policy.setCreatedBy(createdBy);
        }
        return policy;
    }
}
