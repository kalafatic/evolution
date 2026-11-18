package eu.kalafatic.evolution.controller.tests.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortRegistry;

/**
 * ENR port convention tests (Phase 6, WP-6.4): the ENR HTTP API is part of the
 * authoritative EVO port registry (family convention 28780).
 */
public class PortRegistryEnrTest {

    @Test
    public void enrPortConvention() {
        assertEquals(28780, PortRegistry.EVO_ENR.getDefaultPort());
        assertEquals(8780, PortRegistry.EVO_ENR.getOriginalPort());
        assertEquals("ENR", PortRegistry.EVO_ENR.getServiceId());
        assertEquals(PortRegistry.EVO_ENR, PortRegistry.findByServiceId("ENR"));
        assertEquals(PortRegistry.EVO_ENR, PortRegistry.findByDefaultPort(28780));
    }

    @Test
    public void allocationManagerServesEnrPort() {
        int port = PortAllocationManager.getInstance().getPort("ENR");
        assertEquals(28780, port);
        assertNotNull(PortAllocationManager.getInstance().getConfiguration("ENR"));
    }
}
