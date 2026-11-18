package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorClient;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP7 - Port isolation tests: single authoritative child-port contract,
 * debug offset consistency, effective-port usage.
 */
public class SelfDevPortIsolationTest {

    private ResourceManager resourceManager;

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @Test
    public void testDebugOffsetAppliedConsistently() {
        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        context.setDebugMode(false);

        int baseSupervisor = context.getEffectiveSupervisorPort();
        int baseControl = context.getEffectiveSupervisorControlPort();
        int baseServer = context.getEffectiveServerPort();
        assertEquals("Non-debug offset must be zero", 0, context.getPortOffset());

        context.setDebugMode(true);
        assertEquals(PortRegistry.DEBUG_PORT_OFFSET, context.getPortOffset());
        assertEquals("Supervisor port must use the debug offset", baseSupervisor + PortRegistry.DEBUG_PORT_OFFSET, context.getEffectiveSupervisorPort());
        assertEquals("Control port must use the debug offset", baseControl + PortRegistry.DEBUG_PORT_OFFSET, context.getEffectiveSupervisorControlPort());
        assertEquals("Server port must use the debug offset", baseServer + PortRegistry.DEBUG_PORT_OFFSET, context.getEffectiveServerPort());
    }

    @Test
    public void testSupervisorClientUsesEffectivePorts() {
        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        context.setDebugMode(false);
        SupervisorClient client = new SupervisorClient(context);
        assertTrue("SupervisorClient must target the effective supervisor port",
                client.getBaseUrl().endsWith(":" + context.getEffectiveSupervisorPort()));

        // Context-less fallback uses the centralized default registry value.
        SupervisorClient fallback = new SupervisorClient();
        assertTrue(fallback.getBaseUrl().endsWith(":" + PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()));
    }

    @Test
    public void testDebugPortsDistinctFromProductionBase() {
        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        context.setDebugMode(false);
        int prodSupervisor = context.getEffectiveSupervisorPort();
        context.setDebugMode(true);
        assertTrue("Debug supervisor port must differ from production base",
                context.getEffectiveSupervisorPort() != prodSupervisor);
    }
}
