package eu.kalafatic.evolution.controller.tests.teip;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.EvolutionServer;
import eu.kalafatic.evolution.controller.teip.service.TeipExecutionManager;

/**
 * Phase 3 WP-D3 live-runtime support test: boots the real EvolutionServer with the TEIP
 * 1.1 endpoints on the canonical port 28080 and holds it open for a bounded time so an
 * external client (curl / TRYST-style HTTP client) can exercise the wire contract
 * against the real runtime. Not part of normal suite runs.
 */
public class TeipLiveServerHoldTest {

    private static EvolutionServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        TeipExecutionManager.getInstance().clearAll();
        TeipExecutionManager.getInstance().setTeipEnabled(true);
        System.setProperty("evo.teip.devMode", "true");
        System.clearProperty("evo.teip.auth.token");
        int port = Integer.getInteger("teip.hold.port", 28081);
        server = new EvolutionServer(port);
        server.startServer();
        System.out.println("[TEIP-LIVE] EvolutionServer started on " + port);
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.stopServer();
        }
        TeipExecutionManager.getInstance().clearAll();
        System.clearProperty("evo.teip.devMode");
        System.clearProperty("evo.teip.auth.token");
    }

    @Test
    public void holdServer() throws Exception {
        long holdMillis = Long.getLong("teip.hold.seconds", 420) * 1000;
        System.out.println("[TEIP-LIVE] Holding server for " + holdMillis + " ms");
        Thread.sleep(holdMillis);
    }
}
