package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.net.ServerSocket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortAvailabilityChecker;
import eu.kalafatic.evolution.controller.resource.PortConfiguration;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;

/**
 * Platform-wide regression tests for authoritative EVO port management architecture.
 */
public class PortManagementTest {

    @Before
    public void setUp() {
        PortAllocationManager.getInstance().initDefaults();
    }

    @Test
    public void testAuthoritativeOffsetAndIncrement() {
        // Base offset +20000, step 100
        assertEquals(28080, PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort());
        assertEquals(28180, PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort());
        assertEquals(28280, PortRegistry.EVO_MCP.getDefaultPort());
        assertEquals(28380, PortRegistry.EVO_DEVELOP.getDefaultPort());
        assertEquals(28480, PortRegistry.EVO_SUPERVISOR_API.getDefaultPort());
        assertEquals(28580, PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort());
        assertEquals(28680, PortRegistry.EVO_AUXILIARY.getDefaultPort());

        for (PortRegistry reg : PortRegistry.values()) {
            assertEquals("Port must follow +20000 convention",
                    reg.getOriginalPort() + PortRegistry.BASE_OFFSET, reg.getDefaultPort());
        }
    }

    @Test
    public void testNoInternalCollisions() {
        Set<Integer> assignedPorts = new HashSet<>();
        for (PortRegistry reg : PortRegistry.values()) {
            boolean added = assignedPorts.add(reg.getDefaultPort());
            assertTrue("Duplicate port found in PortRegistry: " + reg.getDefaultPort(), added);
        }

        List<String> integrityIssues = PortAllocationManager.getInstance().validateIntegrity();
        assertTrue("Integrity issues detected: " + integrityIssues, integrityIssues.isEmpty());
    }

    @Test
    public void testLegacyPortMigration() {
        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();

        ServerSettings srv = OrchestrationFactory.eINSTANCE.createServerSettings();
        srv.setPort(48080); // legacy primary
        srv.setMcpPort(38080); // legacy MCP
        srv.setPortalPort(58080); // legacy secondary/portal
        srv.setPortalUrl("http://localhost:58080/dashboard.html");
        orch.setServerSettings(srv);
        orch.setMcpServerUrl("http://localhost:38080/mcp");

        SupervisorSettings sup = OrchestrationFactory.eINSTANCE.createSupervisorSettings();
        sup.setSettings("port=8089;controlPort=28080;debug=false");
        orch.setSupervisorSettings(sup);

        boolean migrated = PortAllocationManager.getInstance().migrateFromLegacy(orch);
        assertTrue("Migration must be executed for legacy models", migrated);

        assertEquals("Primary port migrated", 28080, srv.getPort());
        assertEquals("MCP port migrated", 28280, srv.getMcpPort());
        assertEquals("Portal port migrated", 28180, srv.getPortalPort());
        assertTrue("Portal URL migrated", srv.getPortalUrl().contains("28180"));
        assertTrue("MCP server URL migrated", orch.getMcpServerUrl().contains("28280"));

        assertTrue("Supervisor API port migrated to 28480", sup.getSettings().contains("port=28480"));
        assertTrue("Supervisor Control port migrated to 28580", sup.getSettings().contains("controlPort=28580"));
    }

    @Test
    public void testCollisionDetectionDiagnostics() throws Exception {
        int testPort = 28990;
        try (ServerSocket socket = new ServerSocket(testPort)) {
            socket.setReuseAddress(true);

            // Verification of occupied port
            assertFalse("Port must be detected as occupied", PortAvailabilityChecker.isPortAvailable(testPort));

            PortAvailabilityChecker.ConflictReport report = PortAvailabilityChecker.inspectPort(testPort, "TestService");
            assertTrue("Conflict report must indicate in use", report.isInUse());
            assertEquals(testPort, report.getPort());
            assertEquals("TestService", report.getAffectedService());
            assertNotNull(report.toString());
            assertTrue(report.toString().contains("PORT CONFLICT DETECTED"));
            assertTrue(report.toString().contains("EVO ports are deterministic"));
        }

        assertTrue("Port must be available after socket close", PortAvailabilityChecker.isPortAvailable(testPort));
    }

    @Test
    public void testResourceManagerServicesContainStandardPorts() {
        ResourceManager rm = ResourceManager.getInstance();
        assertNotNull(rm.getService("SERVER"));
        assertEquals(28080, rm.getService("SERVER").getPort());

        assertNotNull(rm.getService("MCP"));
        assertEquals(28280, rm.getService("MCP").getPort());

        assertNotNull(rm.getService("SUPERVISOR"));
        assertEquals(28480, rm.getService("SUPERVISOR").getPort());

        assertNotNull(rm.getService("SUPERVISOR_CONTROL"));
        assertEquals(28580, rm.getService("SUPERVISOR_CONTROL").getPort());

        assertNotNull(rm.getService("DEVELOP"));
        assertEquals(28380, rm.getService("DEVELOP").getPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPortCollisionRejection() {
        PortAllocationManager pam = PortAllocationManager.getInstance();
        // Attempt to assign primary port to MCP service
        pam.setAssignedPort("MCP", 28080);
    }
}
