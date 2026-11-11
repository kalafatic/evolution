package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.ServerEntry;
import eu.kalafatic.evolution.controller.orchestration.ServerLifecycleListener;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.orchestration.ServerState;
import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;
import eu.kalafatic.evolution.controller.resource.PortRegistry;

/**
 * End-to-end tests for ServerManager authoritative lifecycle and automatic startup.
 */
public class ServerManagerLifecycleTest {

    private ServerManager serverManager;

    @Before
    public void setUp() {
        serverManager = ServerManager.getInstance();
    }

    @After
    public void tearDown() {
        if (serverManager != null) {
            serverManager.stopAll();
        }
    }

    @Test
    public void testCanonicalServerEntriesInitialized() {
        List<ServerEntry> entries = serverManager.getServerEntries();
        assertNotNull("Server entries must not be null", entries);
        assertEquals("Must manage all 6 canonical ports", 6, entries.size());

        assertNotNull("Primary HTTP must exist", serverManager.getServerEntry(PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort()));
        assertNotNull("Secondary HTTP must exist", serverManager.getServerEntry(PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort()));
        assertNotNull("MCP must exist", serverManager.getServerEntry(PortRegistry.EVO_MCP.getDefaultPort()));
        assertNotNull("Develop must exist", serverManager.getServerEntry(PortRegistry.EVO_DEVELOP.getDefaultPort()));
        assertNotNull("Supervisor API must exist", serverManager.getServerEntry(PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()));
        assertNotNull("Supervisor Control must exist", serverManager.getServerEntry(PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort()));
    }

    @Test
    public void testWebApplicationsRegistered() {
        List<WebApplicationDescriptor> webApps = serverManager.getWebApplications();
        assertNotNull("Web applications must not be null", webApps);
        assertTrue("Must register EVO web applications", webApps.size() >= 5);

        boolean hasChat = webApps.stream().anyMatch(a -> a.getId().equals("chat"));
        boolean hasDashboard = webApps.stream().anyMatch(a -> a.getId().equals("dashboard"));
        boolean hasDevelop = webApps.stream().anyMatch(a -> a.getId().equals("develop"));
        boolean hasMcp = webApps.stream().anyMatch(a -> a.getId().equals("mcp"));
        boolean hasForge = webApps.stream().anyMatch(a -> a.getId().equals("forge"));

        assertTrue("Must include chat web application", hasChat);
        assertTrue("Must include dashboard web application", hasDashboard);
        assertTrue("Must include develop web application", hasDevelop);
        assertTrue("Must include mcp web application", hasMcp);
        assertTrue("Must include forge web application", hasForge);
    }

    @Test
    public void testLifecycleTransitionsAndListenerNotifications() throws Exception {
        CountDownLatch stateChangedLatch = new CountDownLatch(1);
        AtomicBoolean receivedRunning = new AtomicBoolean(false);

        ServerLifecycleListener testListener = new ServerLifecycleListener() {
            @Override
            public void onServerStateChanged(ServerEntry entry) {
                if (entry.getPort() == PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort() && entry.getState() == ServerState.RUNNING) {
                    receivedRunning.set(true);
                    stateChangedLatch.countDown();
                }
            }

            @Override
            public void onAllServersStarted(List<ServerEntry> servers) {}

            @Override
            public void onWebApplicationReady(WebApplicationDescriptor webApp) {}

            @Override
            public void onWebApplicationUnavailable(WebApplicationDescriptor webApp) {}
        };

        serverManager.addListener(testListener);

        try {
            // Start primary server
            int port = PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort();
            serverManager.start(port);

            boolean notified = stateChangedLatch.await(5, TimeUnit.SECONDS);
            assertTrue("Listener must receive RUNNING notification", notified);
            assertTrue("State must be RUNNING", receivedRunning.get());

            ServerEntry primaryEntry = serverManager.getServerEntry(port);
            assertEquals("Entry state must be RUNNING", ServerState.RUNNING, primaryEntry.getState());
            assertTrue("canStop must be true when RUNNING", primaryEntry.canStop());
            assertTrue("canRestart must be true when RUNNING", primaryEntry.canRestart());

            // Stop server
            serverManager.stop(port);
            assertEquals("Entry state must be STOPPED", ServerState.STOPPED, primaryEntry.getState());
            assertTrue("canStart must be true when STOPPED", primaryEntry.canStart());
        } finally {
            serverManager.removeListener(testListener);
        }
    }

    @Test
    public void testAutoStartAllNonBlocking() throws Exception {
        CountDownLatch allStartedLatch = new CountDownLatch(1);

        ServerLifecycleListener testListener = new ServerLifecycleListener() {
            @Override
            public void onServerStateChanged(ServerEntry entry) {}

            @Override
            public void onAllServersStarted(List<ServerEntry> servers) {
                allStartedLatch.countDown();
            }

            @Override
            public void onWebApplicationReady(WebApplicationDescriptor webApp) {}

            @Override
            public void onWebApplicationUnavailable(WebApplicationDescriptor webApp) {}
        };

        serverManager.addListener(testListener);
        try {
            serverManager.autoStartAll();
            boolean completed = allStartedLatch.await(10, TimeUnit.SECONDS);
            assertTrue("autoStartAll must complete and fire onAllServersStarted", completed);

            // Primary server should be RUNNING
            ServerEntry primary = serverManager.getServerEntry(PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort());
            assertEquals("Primary server must be RUNNING", ServerState.RUNNING, primary.getState());
        } finally {
            serverManager.removeListener(testListener);
        }
    }
}
