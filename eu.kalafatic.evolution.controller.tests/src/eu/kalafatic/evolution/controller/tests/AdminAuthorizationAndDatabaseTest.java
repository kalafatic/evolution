package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.model.*;
import eu.kalafatic.evolution.servers.repository.*;
import eu.kalafatic.evolution.servers.service.AuthorizationService;

public class AdminAuthorizationAndDatabaseTest {

    private DatabaseManager dbManager;
    private AuthorizationService authService;

    @Before
    public void setUp() throws Exception {
        // Ensure clean test database directory
        File dbDir = new File("data");
        if (!dbDir.exists()) dbDir.mkdirs();

        dbManager = new DatabaseManager();
        authService = new AuthorizationService(dbManager);
    }

    @Test
    public void testDatabaseInitializationAndSeeding() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            assertNotNull("Connection should be established", conn);
        }

        UserRepository userRepo = authService.getUserRepository();
        List<User> users = userRepo.findAll();
        assertFalse("Users table should have seeded default admin", users.isEmpty());
        assertTrue("Admin user should exist", users.stream().anyMatch(u -> "admin".equals(u.getUsername())));

        RoleRepository roleRepo = authService.getRoleRepository();
        List<Role> roles = roleRepo.findAll();
        assertEquals("Should seed 5 default system roles", 5, roles.size());

        PermissionRepository permRepo = authService.getPermissionRepository();
        List<Permission> perms = permRepo.findAll();
        assertFalse("Should seed default permissions", perms.isEmpty());
    }

    @Test
    public void testAuthorizationExplicitDenyOverride() throws SQLException {
        UserRepository userRepo = authService.getUserRepository();
        ResourceRepository resRepo = authService.getResourceRepository();
        ResourceAccessRepository policyRepo = authService.getResourceAccessRepository();

        // Create test user with DEVELOPER role
        User devUser = new User(null, "dev1", "Developer 1", "hash", "DEVELOPER", true, null, null, null, "Test Dev");
        userRepo.save(devUser);

        // Register resource
        authService.registerResource("MCP_TOOL", "test_tool", "Test Tool", "A test tool", true, "{}");
        ResourceItem resItem = resRepo.findByTypeAndKey("MCP_TOOL", "test_tool").get();

        // Grant explicit ALLOW to DEVELOPER role for EXECUTE
        Role devRole = authService.getRoleRepository().findByName("DEVELOPER").get();
        ResourceAccessPolicy allowPolicy = new ResourceAccessPolicy(null, "ROLE", devRole.getId(), resItem.getId(), "EXECUTE", "ALLOW", null, null);
        policyRepo.save(allowPolicy);

        // Assign user to DEVELOPER role in user_roles table
        userRepo.assignUserRole(devUser.getId(), devRole.getId(), null);

        // Verify authorization allowed
        boolean allowedBeforeDeny = authService.authorize("dev1", "MCP_TOOL", "test_tool", "EXECUTE");
        assertTrue("User dev1 should be allowed by ROLE ALLOW policy", allowedBeforeDeny);

        // Add explicit USER DENY policy
        ResourceAccessPolicy denyPolicy = new ResourceAccessPolicy(null, "USER", devUser.getId(), resItem.getId(), "EXECUTE", "DENY", null, null);
        policyRepo.save(denyPolicy);

        // Verify explicit DENY overrides ALLOW
        boolean allowedAfterDeny = authService.authorize("dev1", "MCP_TOOL", "test_tool", "EXECUTE");
        assertFalse("Explicit USER DENY policy must override ALLOW policy", allowedAfterDeny);
    }

    @Test
    public void testSystemPrincipalAuthorizationBypass() {
        boolean authorized = authService.authorize("SYSTEM", "MCP_TOOL", "any_tool", "EXECUTE");
        assertTrue("SYSTEM principal must be authorized by default", authorized);
    }
}
