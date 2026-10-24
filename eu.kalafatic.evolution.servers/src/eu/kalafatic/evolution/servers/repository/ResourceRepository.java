package eu.kalafatic.evolution.servers.repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.ResourceItem;

public class ResourceRepository {
    private final DatabaseManager dbManager;

    public ResourceRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(ResourceItem resource) throws SQLException {
        String sql = "INSERT INTO resources (resource_type, resource_key, display_name, description, enabled, metadata) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, resource.getResourceType());
            pstmt.setString(2, resource.getResourceKey());
            pstmt.setString(3, resource.getDisplayName());
            pstmt.setString(4, resource.getDescription());
            pstmt.setBoolean(5, resource.isEnabled());
            pstmt.setString(6, resource.getMetadata());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    resource.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    public Optional<ResourceItem> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM resources WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<ResourceItem> findByTypeAndKey(String type, String key) throws SQLException {
        String sql = "SELECT * FROM resources WHERE resource_type = ? AND resource_key = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ResourceItem> findAll() throws SQLException {
        List<ResourceItem> list = new ArrayList<>();
        String sql = "SELECT * FROM resources";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToResource(rs));
            }
        }
        return list;
    }

    public List<ResourceItem> findByType(String type) throws SQLException {
        List<ResourceItem> list = new ArrayList<>();
        String sql = "SELECT * FROM resources WHERE resource_type = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToResource(rs));
                }
            }
        }
        return list;
    }

    public void update(ResourceItem resource) throws SQLException {
        String sql = "UPDATE resources SET display_name = ?, description = ?, enabled = ?, metadata = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, resource.getDisplayName());
            pstmt.setString(2, resource.getDescription());
            pstmt.setBoolean(3, resource.isEnabled());
            pstmt.setString(4, resource.getMetadata());
            pstmt.setLong(5, resource.getId());
            pstmt.executeUpdate();
        }
    }

    private ResourceItem mapResultSetToResource(ResultSet rs) throws SQLException {
        ResourceItem res = new ResourceItem();
        res.setId(rs.getLong("id"));
        res.setResourceType(rs.getString("resource_type"));
        res.setResourceKey(rs.getString("resource_key"));
        res.setDisplayName(rs.getString("display_name"));
        res.setDescription(rs.getString("description"));
        res.setEnabled(rs.getBoolean("enabled"));
        res.setMetadata(rs.getString("metadata"));
        return res;
    }
}
