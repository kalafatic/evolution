package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpRuntime;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpVerifier;
import eu.kalafatic.evolution.controller.orchestration.selfdev.RcpReadinessLevel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP6 - RCP readiness tests: explicit readiness levels, timeout => FAILED,
 * no success on mere liveness, OSGi verification, port conflict detection.
 */
public class SelfDevReadinessTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;
    private final List<File> createdRunDirs = new ArrayList<>();
    private final List<Process> spawnedProbes = new ArrayList<>();
    private final List<HttpServer> spawnedServers = new ArrayList<>();

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @After
    public void tearDown() {
        for (HttpServer server : spawnedServers) {
            try { server.stop(0); } catch (Exception ignored) {}
        }
        spawnedServers.clear();
        for (Process p : spawnedProbes) {
            try { p.destroyForcibly(); } catch (Exception ignored) {}
        }
        spawnedProbes.clear();
        for (File dir : createdRunDirs) {
            try { deleteRecursively(dir); } catch (Exception ignored) {}
        }
        createdRunDirs.clear();
    }

    private void deleteRecursively(File f) throws Exception {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursively(c);
                }
            }
        }
        Files.deleteIfExists(f.toPath());
    }

    private SelfDevContext newContext() {
        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        createdRunDirs.add(context.getBuildDirectory().getParentFile());
        return context;
    }

    private Process spawnSleep(String name) throws IOException {
        Process p = new ProcessBuilder("sleep", "30").start();
        spawnedProbes.add(p);
        return p;
    }

    private HttpServer startHttpServer(int port, String statusBody, String osgiBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/server/status", exchange -> {
            byte[] bytes = statusBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        if (osgiBody != null) {
            server.createContext("/server/osgi", exchange -> {
                byte[] bytes = osgiBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
        }
        server.start();
        spawnedServers.add(server);
        return server;
    }

    private static int freePort() throws IOException {
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    // ------------------------------------------------------------------

    @Test
    public void testTimeoutIsFailureNotAliveSuccess() throws Exception {
        SelfDevContext context = newContext();
        Process probe = spawnSleep("probe");

        EvoRcpVerifier verifier = new EvoRcpVerifier();
        TaskResult res = verifier.verifyReady(probe, context.getRuntimeDirectory(), null, -1, 3);
        assertEquals("Alive-but-unready must be FAILED (D-02)", TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("Timed out"));
        assertTrue(res.getMessage().contains("PROCESS_ALIVE"));
    }

    @Test
    public void testHttpWithoutAppReadinessIsFailureWithLevel() throws Exception {
        SelfDevContext context = newContext();
        int port = freePort();
        // No /server/osgi context -> OSGi can never be verified; status body
        // without RUNNING -> application never initialized.
        startHttpServer(port, "{\"status\":\"OK\"}", null);
        Process probe = spawnSleep("probe2");

        EvoRcpVerifier verifier = new EvoRcpVerifier();
        TaskResult res = verifier.verifyReady(probe, context.getRuntimeDirectory(), null, port, 3);
        assertEquals(TaskStatus.FAILED, res.getStatus());
        // status body without RUNNING -> only HTTP_RESPONSIVE is verified.
        assertTrue("Highest verified level must be HTTP_RESPONSIVE: " + res.getMessage(),
                res.getMessage().contains("Highest level verified: HTTP_RESPONSIVE"));
        assertFalse("Mere HTTP responsiveness must not be reported as initialized",
                res.getMessage().contains("Highest level verified: APPLICATION_INITIALIZED"));
    }

    @Test
    public void testFullReadinessSucceedsWithHighestLevel() throws Exception {
        SelfDevContext context = newContext();
        File runDir = context.getRuntimeDirectory().getParentFile();
        Files.writeString(new File(runDir, "evo_ready.txt").toPath(), String.valueOf(System.currentTimeMillis()));

        int port = freePort();
        String statusBody = "{\"status\":\"RUNNING\",\"port\":" + port + "}";
        String osgiBody = "{\"bundles\":[{\"symbolicName\":\"eu.kalafatic.evolution.view\",\"resolved\":true,\"state\":\"ACTIVE\"}]}";
        startHttpServer(port, statusBody, osgiBody);
        Process probe = spawnSleep("probe3");

        EvoRcpVerifier verifier = new EvoRcpVerifier();
        TaskResult res = verifier.verifyReady(probe, context.getRuntimeDirectory(), null, port, 5);
        assertTrue("Full readiness must succeed: " + res.getMessage(), res.isSuccess());
        assertEquals(RcpReadinessLevel.APPLICATION_HEALTHY.name(), res.getDiagnostic("readinessLevel", "NONE"));
    }

    @Test
    public void testUnresolvedOsgiBundleFailsStartup() throws Exception {
        SelfDevContext context = newContext();
        File runDir = context.getRuntimeDirectory().getParentFile();
        Files.writeString(new File(runDir, "evo_ready.txt").toPath(), String.valueOf(System.currentTimeMillis()));

        int port = freePort();
        String statusBody = "{\"status\":\"RUNNING\"}";
        String osgiBody = "{\"bundles\":[{\"symbolicName\":\"eu.kalafatic.evolution.view\",\"resolved\":false,\"state\":\"INSTALLED\"}]}";
        startHttpServer(port, statusBody, osgiBody);
        Process probe = spawnSleep("probe4");

        EvoRcpVerifier verifier = new EvoRcpVerifier();
        TaskResult res = verifier.verifyReady(probe, context.getRuntimeDirectory(), null, port, 5);
        assertEquals("Unresolved EVO bundles must fail startup verification", TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("OSGi bundle resolution failed"));
    }

    @Test
    public void testMinimumLevelParameterControlsSuccess() throws Exception {
        SelfDevContext context = newContext();
        int port = freePort();
        // No /server/osgi context and no RUNNING status -> the only level that
        // can be verified is HTTP_RESPONSIVE.
        startHttpServer(port, "{\"status\":\"OK\"}", null);
        Process probe = spawnSleep("probe5");

        EvoRcpVerifier verifier = new EvoRcpVerifier();
        TaskResult res = verifier.verifyReady(probe, context.getRuntimeDirectory(), null, port, 5, RcpReadinessLevel.HTTP_RESPONSIVE);
        assertTrue("HTTP_RESPONSIVE minimum must pass once HTTP responds: " + res.getMessage(), res.isSuccess());
        assertTrue(res.getDiagnostic("readinessLevel", "").contains("HTTP_RESPONSIVE"));
    }

    @Test
    public void testPortAvailabilityDetection() throws Exception {
        try (java.net.ServerSocket occupied = new java.net.ServerSocket(0)) {
            int busy = occupied.getLocalPort();
            assertFalse("Occupied port must be reported unavailable", EvoRcpRuntime.isPortAvailable(busy));
            assertTrue("Free port must be reported available", EvoRcpRuntime.isPortAvailable(freePort()));
        }
    }

    @Test
    public void testReadinessLevelOrdering() {
        assertTrue(RcpReadinessLevel.APPLICATION_INITIALIZED.atLeast(RcpReadinessLevel.HTTP_RESPONSIVE));
        assertFalse(RcpReadinessLevel.PROCESS_ALIVE.atLeast(RcpReadinessLevel.OSGI_RESOLVED));
        assertEquals(RcpReadinessLevel.APPLICATION_HEALTHY,
                RcpReadinessLevel.max(RcpReadinessLevel.APPLICATION_HEALTHY, RcpReadinessLevel.PROCESS_STARTED));
        assertNotNull(RcpReadinessLevel.valueOf("APPLICATION_INITIALIZED"));
    }
}
