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

    @Test
    public void testProtocolsMatchAuthoritativeSpecification() {
        assertEquals("Primary HTTP must use HTTP protocol", "http", PortRegistry.EVO_HTTP_PRIMARY.getProtocol());
        assertTrue(PortRegistry.EVO_HTTP_PRIMARY.getPortProtocol().isHttp());

        assertEquals("Secondary HTTP must use HTTP protocol", "http", PortRegistry.EVO_HTTP_SECONDARY.getProtocol());
        assertTrue(PortRegistry.EVO_HTTP_SECONDARY.getPortProtocol().isHttp());

        assertEquals("MCP must use HTTP protocol", "http", PortRegistry.EVO_MCP.getProtocol());
        assertTrue(PortRegistry.EVO_MCP.getPortProtocol().isHttp());

        assertEquals("DEVELOP must use HTTP protocol", "http", PortRegistry.EVO_DEVELOP.getProtocol());
        assertTrue(PortRegistry.EVO_DEVELOP.getPortProtocol().isHttp());

        assertEquals("Supervisor REST must use HTTP protocol", "http", PortRegistry.EVO_SUPERVISOR_API.getProtocol());
        assertTrue(PortRegistry.EVO_SUPERVISOR_API.getPortProtocol().isHttp());

        assertEquals("Supervisor Control must use TCP protocol", "tcp", PortRegistry.EVO_SUPERVISOR_CONTROL.getProtocol());
        assertTrue(PortRegistry.EVO_SUPERVISOR_CONTROL.getPortProtocol().isTcp());

        assertEquals("Auxiliary must use HTTP protocol", "http", PortRegistry.EVO_AUXILIARY.getProtocol());
        assertTrue(PortRegistry.EVO_AUXILIARY.getPortProtocol().isHttp());

        // Verify ResourceManager reflects TCP for SUPERVISOR_CONTROL
        ResourceManager rm = ResourceManager.getInstance();
        assertEquals("tcp", rm.getService("SUPERVISOR_CONTROL").getProtocol());
    }

    @Test
    public void testDebugPortIsolationAndCollisionPrevention() {
        assertEquals(10, PortRegistry.DEBUG_PORT_OFFSET);
        assertEquals(28090, PortRegistry.EVO_HTTP_PRIMARY.getDebugPort());
        assertEquals(28490, PortRegistry.EVO_SUPERVISOR_API.getDebugPort());
        assertEquals(28590, PortRegistry.EVO_SUPERVISOR_CONTROL.getDebugPort());

        assertTrue(PortRegistry.isDebugPort(28090));
        assertTrue(PortRegistry.isDebugPort(28490));
        assertTrue(PortRegistry.isDebugPort(28590));
        assertFalse(PortRegistry.isDebugPort(28080));
        assertFalse(PortRegistry.isDebugPort(28480));

        assertEquals(PortRegistry.EVO_HTTP_PRIMARY, PortRegistry.findByDebugPort(28090));

        // Ensure PortAllocationManager rejects assigning a debug port to a production service
        try {
            PortAllocationManager.getInstance().setAssignedPort("MCP", 28090);
            fail("Must not allow assigning a reserved debug port");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("reserved for debug"));
        }
    }

    @Test
    public void testNanoHttpdRejectsTcpControlPort() {
        try {
            new eu.kalafatic.evolution.controller.orchestration.EvolutionServer(28580);
            fail("NanoHTTPD EvolutionServer must reject TCP control port 28580");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("TCP control port"));
        }

        try {
            new eu.kalafatic.evolution.servers.server.EvolutionServer(28580);
            fail("Servers EvolutionServer must reject TCP control port 28580");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("TCP control port"));
        }
    }

    @Test
    public void testMcpDemoDocumentationServerLocalhostConnectivity() throws Exception {
        eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager mcpManager =
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance();
        mcpManager.start();
        try {
            assertTrue(mcpManager.isRunning());
            int port = mcpManager.getPort();
            assertEquals(28280, port);

            // Connect using localhost
            java.net.URL url = new java.net.URI("http://localhost:" + port + "/mcp/").toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int responseCode = conn.getResponseCode();
            assertEquals(200, responseCode);
        } finally {
            mcpManager.stop();
            assertFalse(mcpManager.isRunning());
        }
    }
}
