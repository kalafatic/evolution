package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortLifecycleManager;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Validates the 8-stage port lifecycle sequence, listener management, and clean shutdown.
 */
public class PortLifecycleTest {

    private PortLifecycleManager lifecycleManager;

    @Before
    public void setUp() {
        PortAllocationManager.getInstance().initDefaults();
        lifecycleManager = PortLifecycleManager.getInstance();
    }

    @After
    public void tearDown() {
        lifecycleManager.shutdownAll();
    }

    @Test
    public void testPrepareLifecycleSuccess() {
        Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
        lifecycleManager.prepareLifecycle(orch);
        assertTrue("Lifecycle manager must be initialized after prepare", lifecycleManager.isInitialized());
    }

    @Test
    public void testManagedListenerLifecycle() throws IOException {
        final boolean[] listening = {false};
        final int port = 28995;

        PortLifecycleManager.ManagedListener listener = new PortLifecycleManager.ManagedListener() {
            @Override public String getServiceId() { return "TEST_LISTENER"; }
            @Override public int getPort() { return port; }
            @Override public void startListener() { listening[0] = true; }
            @Override public void stopListener() { listening[0] = false; }
            @Override public boolean isListening() { return listening[0]; }
        };

        lifecycleManager.startManagedListener(listener);
        assertTrue(listening[0]);
        assertTrue(lifecycleManager.isEndpointBound("TEST_LISTENER"));
        assertEquals(port, lifecycleManager.getBoundPort("TEST_LISTENER"));

        lifecycleManager.stopManagedListener("TEST_LISTENER");
        assertFalse(listening[0]);
        assertFalse(lifecycleManager.isEndpointBound("TEST_LISTENER"));
    }
}
