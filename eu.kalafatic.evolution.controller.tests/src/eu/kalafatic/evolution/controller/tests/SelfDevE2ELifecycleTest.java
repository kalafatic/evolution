package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpServer;

import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactManifestStore;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpRuntime;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitBaseline;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ProcessOwnershipRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.RcpReadinessLevel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StartEvoTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP12 - End-to-end lifecycle scenarios with a real child process stand-in.
 *
 * The child product is a small compiled Java HTTP stub wrapped as the product
 * executable; this exercises the REAL deployment, ownership, readiness,
 * start/stop and rollback machinery without a full Tycho product build.
 * Git-related steps are simulation-only (WP9 boundary) - zero real Git
 * mutations.
 */
public class SelfDevE2ELifecycleTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;
    private final List<File> createdRunDirs = new ArrayList<>();
    private final List<HttpServer> spawnedServers = new ArrayList<>();
    private File stubClassDir;
    private final Set<Long> childPids = new HashSet<>();

    private String headBefore;
    private String statusBefore;

    @Before
    public void setUp() throws Exception {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
        stubClassDir = compileStub();
        // Use the canonical real repository directly for read-only checks,
        // independent of any EMF orchestrator state mutation.
        File realRepo = new File(System.getProperty("user.home"), "git/evolution");
        if (!new File(realRepo, ".git").exists()) {
            realRepo = new File(".").getAbsoluteFile();
            while (realRepo != null && !new File(realRepo, ".git").exists()) {
                realRepo = realRepo.getParentFile();
            }
        }
        GitManager readOnly = new GitManager(realRepo);
        headBefore = readOnly.getHeadCommit();
        statusBefore = readOnly.getStatusPorcelain();
    }

    @After
    public void tearDown() throws Exception {
        // Kill only processes WE launched (recorded child PIDs).
        for (Long pid : childPids) {
            try {
                ProcessHandle.of(pid).ifPresent(h -> {
                    h.descendants().forEach(ProcessHandle::destroyForcibly);
                    h.destroyForcibly();
                });
            } catch (Exception ignored) {}
        }
        childPids.clear();
        for (HttpServer server : spawnedServers) {
            try { server.stop(0); } catch (Exception ignored) {}
        }
        spawnedServers.clear();
        for (File dir : createdRunDirs) {
            if (System.getenv("SELFDEV_KEEP_RUNS") == null) {
                try { deleteRecursively(dir); } catch (Exception ignored) {}
            }
        }
        createdRunDirs.clear();
        // Zero real Git mutations assertion.
        File realRepo = new File(System.getProperty("user.home"), "git/evolution");
        if (!new File(realRepo, ".git").exists()) {
            realRepo = new File(".").getAbsoluteFile();
            while (realRepo != null && !new File(realRepo, ".git").exists()) {
                realRepo = realRepo.getParentFile();
            }
        }
        GitManager readOnly = new GitManager(realRepo);
        assertEquals("HEAD must be unchanged (zero real git mutations)", headBefore, readOnly.getHeadCommit());
        assertEquals("Working tree must be unchanged (zero real git mutations)", statusBefore, readOnly.getStatusPorcelain());
        assertTrue("Parent test JVM must survive all scenarios", ProcessHandle.current().isAlive());
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

    private static int freePort() throws Exception {
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    // ------------------------------------------------------------------
    // Child stand-in product
    // ------------------------------------------------------------------

    private File compileStub() throws Exception {
        File dir = tempFolder.newFolder("stub");
        File source = new File(dir, "SelfDevStubServer.java");
        String code = """
            import com.sun.net.httpserver.HttpServer;
            import java.io.OutputStream;
            import java.net.InetSocketAddress;
            import java.nio.charset.StandardCharsets;
            public class SelfDevStubServer {
                public static void main(String[] args) throws Exception {
                    int parsed = 18080;
                    for (String a : args) {
                        if (a.startsWith("--port=")) { parsed = Integer.parseInt(a.substring("--port=".length())); }
                    }
                    final int port = parsed;
                    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
                    server.createContext("/server/status", ex -> {
                        byte[] b = ("{\\"status\\":\\"RUNNING\\",\\"port\\":" + port + "}").getBytes(StandardCharsets.UTF_8);
                        ex.sendResponseHeaders(200, b.length);
                        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
                    });
                    server.createContext("/server/osgi", ex -> {
                        byte[] b = "{\\"bundles\\":[{\\"symbolicName\\":\\"eu.kalafatic.evolution.view\\",\\"resolved\\":true,\\"state\\":\\"ACTIVE\\"}]}".getBytes(StandardCharsets.UTF_8);
                        ex.sendResponseHeaders(200, b.length);
                        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
                    });
                    server.start();
                    Thread.sleep(Long.MAX_VALUE);
                }
            }
            """;
        Files.writeString(source.toPath(), code);
        Process javac = new ProcessBuilder(
                new File(System.getProperty("java.home"), "bin/javac").getAbsolutePath(),
                "-d", dir.getAbsolutePath(), source.getAbsolutePath()).start();
        String err = new String(javac.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = javac.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Stub compilation failed: " + err);
        }
        return dir;
    }

    /**
     * Builds a fake but layout-valid EVO product ZIP whose "evo" executable is
     * a wrapper script launching the compiled stub child.
     */
    private File buildProductZip(SelfDevContext context, boolean exitImmediately) throws Exception {
        File zip = new File(tempFolder.getRoot(), "evo-product-" + System.nanoTime() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("plugins/eu.kalafatic.evolution.view_1.0.jar"));
            zos.write("dummy bundle".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("configuration/config.ini"));
            zos.write("eclipse.product=evolution\neclipse.application=evolution.application".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            String javaBin = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
            String script = exitImmediately
                    ? "#!/bin/sh\nexit 7\n"
                    : "#!/bin/sh\nfor a in \"$@\"; do case \"$a\" in --port=*) P=\"${a#--port=}\";; esac; done\nexec " + javaBin + " -cp " + stubClassDir.getAbsolutePath() + " SelfDevStubServer \"--port=${P:-18080}\"\n";
            zos.putNextEntry(new ZipEntry("evo"));
            zos.write(script.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return zip;
    }

    private SelfDevContext newContextWithFreePort(int port) {
        SelfDevContext base = new SelfDevContext(null, resourceManager.getOrchestrator());
        createdRunDirs.add(base.getBuildDirectory().getParentFile());
        SelfDevContext overridden = new SelfDevContext(null, resourceManager.getOrchestrator()) {
            @Override
            public int getEffectiveServerPort() {
                return port;
            }
            @Override
            public String getRunId() {
                return base.getRunId();
            }
            @Override
            public File getBuildDirectory() { return base.getBuildDirectory(); }
            @Override
            public File getExportDirectory() { return base.getExportDirectory(); }
            @Override
            public File getRuntimeDirectory() { return base.getRuntimeDirectory(); }
            @Override
            public File getLogDirectory() { return base.getLogDirectory(); }
            @Override
            public File getRepositoryRoot() { return base.getRepositoryRoot(); }
            @Override
            public File getPreparedReactorDirectory() { return base.getPreparedReactorDirectory(); }
            @Override
            public File getProjectRoot() { return base.getProjectRoot(); }
            @Override
            public File getSupervisorDirectory() { return base.getSupervisorDirectory(); }
            @Override
            public File getGenomeDirectory() { return base.getGenomeDirectory(); }
            @Override
            public String getOs() { return base.getOs(); }
            @Override
            public String getWs() { return base.getWs(); }
            @Override
            public String getArch() { return base.getArch(); }
            @Override
            public String getProductId() { return base.getProductId(); }
            @Override
            public String getLauncher() { return base.getLauncher(); }
            @Override
            public String getSourceRevision() { return base.getSourceRevision(); }
            @Override
            public boolean isDebugMode() { return base.isDebugMode(); }
        };
        createdRunDirs.add(overridden.getBuildDirectory().getParentFile());
        return overridden;
    }

    /**
     * Records honest BUILD + EXPORT evidence in the manifest store and context.
     */
    private void recordBuildExportEvidence(SelfDevContext context, File productZip) throws Exception {
        ArtifactManifestStore store = context.getArtifactManifestStore();
        BuildArtifact artifact = store.record(ArtifactType.EVO_RCP, productZip,
                headBefore, store.nextBuildId(), "SUCCESS", "BUILD_VERIFY_EXECUTED", "SUCCESS",
                new HashMap<>(Map.of("productId", "evolution")));
        context.recordArtifact(artifact);
        context.recordTaskResult(TaskResult.success("BUILD_EVO", "E2E simulated build"));
        context.recordTaskResult(TaskResult.success("EXPORT_EVO", "E2E simulated export"));
    }

    // ------------------------------------------------------------------
    // Scenario A - successful lifecycle
    // ------------------------------------------------------------------

    @Test
    public void debugCrossProcessHttpProbe() throws Exception {
        int port = freePort();
        ProcessBuilder pb = new ProcessBuilder(
                new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(),
                "-cp", stubClassDir.getAbsolutePath(), "SelfDevStubServer", "--port=" + port);
        pb.redirectErrorStream(true);
        Process child = pb.start();
        childPids.add(child.pid());
        try {
            Thread.sleep(1200);
            java.net.URL url = new java.net.URI("http://127.0.0.1:" + port + "/server/status").toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            System.out.println("[DEBUG-PROBE] code=" + conn.getResponseCode());
            try (java.io.InputStream is = conn.getInputStream()) {
                System.out.println("[DEBUG-PROBE] body=" + new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            child.destroyForcibly();
            childPids.remove(child.pid());
        }
    }

    @Test
    public void scenarioA_successfulLifecycle() throws Exception {
        int port = freePort();
        SelfDevContext context = newContextWithFreePort(port);
        File productZip = buildProductZip(context, false);
        recordBuildExportEvidence(context, productZip);

        StartEvoTask task = new StartEvoTask("START_EVO");
        TaskResult startRes = task.execute(context);
        assertTrue("START_EVO must succeed: " + startRes.getMessage(), startRes.isSuccess());
        String readiness = startRes.getDiagnostic("readinessLevel", "NONE");
        assertNotNull(readiness);
        assertTrue("Readiness must be at least APPLICATION_INITIALIZED, was " + readiness,
                RcpReadinessLevel.valueOf(readiness).atLeast(RcpReadinessLevel.APPLICATION_INITIALIZED));

        ProcessOwnershipRecord ownership =
                ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);
        assertNotNull("Ownership record must exist after start", ownership);
        assertTrue("Ownership must be run-scoped", ProcessOwnershipRecord.isOwnedByRun(ownership, context));
        assertTrue("Ownership identity must verify", ProcessOwnershipRecord.verifyIdentity(ownership));
        childPids.add(ownership.getPid());

        // Stop only our child.
        EvoRcpRuntime runtime = new EvoRcpRuntime();
        TaskResult stopRes = runtime.stop(context);
        assertTrue("STOP must succeed: " + stopRes.getMessage(), stopRes.isSuccess());
        assertFalse("Child must be terminated", ProcessHandle.of(ownership.getPid()).map(ProcessHandle::isAlive).orElse(false));
        childPids.remove(ownership.getPid());
        ProcessOwnershipRecord cleared =
                ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);
        assertNotNull("Exit status must be recorded", cleared != null ? cleared.getExitStatus() : null);
    }

    // ------------------------------------------------------------------
    // Scenario B - build failure propagation
    // ------------------------------------------------------------------

    @Test
    public void scenarioB_buildFailureBlocksPipeline() throws Exception {
        SelfDevContext context = newContextWithFreePort(freePort());
        context.recordTaskResult(TaskResult.failure("BUILD_EVO", "E2E simulated build failure", null));
        SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);
        // Gate needs a valid snapshot.
        context.setResolvedResources(new eu.kalafatic.evolution.controller.orchestration.selfdev.ResolvedSelfDevResources(
                context.getRepositoryRoot(), context.getProjectRoot(),
                context.getSourceReactorDirectory(), context.getPreparedReactorDirectory(),
                context.getBuildDirectory(), context.getExportDirectory(),
                context.getRuntimeDirectory(), context.getLogDirectory(),
                context.getSupervisorDirectory(), context.getGenomeDirectory(),
                context.getOs(), context.getWs(), context.getArch(),
                context.getProductId(), context.getLauncher(),
                new File(System.getProperty("java.home"), "bin/java"), new File("mvn")));

        TaskResult exportRes = orchestrator.executeTaskWithDependencies("EXPORT_EVO");
        assertEquals("EXPORT_EVO must be BLOCKED after a build failure", TaskStatus.BLOCKED, exportRes.getStatus());
        assertTrue(exportRes.getMessage().contains("BUILD_EVO"));
        assertNull("START_EVO must not run after a build failure", context.getTaskResult("START_EVO"));
    }

    // ------------------------------------------------------------------
    // Scenario C - invalid artifact
    // ------------------------------------------------------------------

    @Test
    public void scenarioC_invalidArtifactsRejected() throws Exception {
        SelfDevContext context = newContextWithFreePort(freePort());
        File zip = buildProductZip(context, false);

        // 1. Missing manifest record: START must not guess.
        StartEvoTask noManifest = new StartEvoTask("START_EVO");
        TaskResult missingRes = noManifest.execute(context);
        assertEquals(TaskStatus.FAILED, missingRes.getStatus());
        assertTrue(missingRes.getMessage().contains("missing required EVO RCP build artifact"));

        // 2. Checksum mismatch: record then tamper the artifact.
        ArtifactManifestStore store = context.getArtifactManifestStore();
        BuildArtifact recorded = store.record(ArtifactType.EVO_RCP, zip, headBefore, store.nextBuildId(),
                "SUCCESS", "BUILD_VERIFY_EXECUTED", "SUCCESS", new HashMap<>());
        context.recordArtifact(recorded);
        Files.writeString(zip.toPath(), "tampered archive bytes", StandardCharsets.UTF_8);
        StartEvoTask tampered = new StartEvoTask("START_EVO");
        TaskResult tamperedRes = tampered.execute(context);
        assertEquals(TaskStatus.FAILED, tamperedRes.getStatus());
        assertTrue("Checksum mismatch must be rejected: " + tamperedRes.getMessage(),
                tamperedRes.getMessage().contains("Checksum mismatch"));

        // 3. Stale (deleted) artifact.
        Files.delete(zip.toPath());
        StartEvoTask stale = new StartEvoTask("START_EVO");
        TaskResult staleRes = stale.execute(context);
        assertEquals(TaskStatus.FAILED, staleRes.getStatus());
        assertTrue(staleRes.getMessage().contains("missing") || staleRes.getMessage().contains("stale")
                || staleRes.getMessage().contains("not found") || staleRes.getMessage().contains("Checksum"));
    }

    // ------------------------------------------------------------------
    // Scenario D - child startup failure
    // ------------------------------------------------------------------

    @Test
    public void scenarioD_childStartupFailure() throws Exception {
        int port = freePort();
        SelfDevContext context = newContextWithFreePort(port);
        File failingZip = buildProductZip(context, true);
        recordBuildExportEvidence(context, failingZip);

        StartEvoTask task = new StartEvoTask("START_EVO");
        TaskResult res = task.execute(context);
        assertEquals("A child that exits immediately must FAIL startup", TaskStatus.FAILED, res.getStatus());

        ProcessOwnershipRecord ownership =
                ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);
        if (ownership != null) {
            assertNotNull("Failure must record the exit status", ownership.getExitStatus());
        }
        assertTrue("Parent JVM must remain alive", ProcessHandle.current().isAlive());
    }

    // ------------------------------------------------------------------
    // Scenario E - git failure (simulation only)
    // ------------------------------------------------------------------

    @Test
    public void scenarioE_gitFailuresAreSafe() throws Exception {
        SelfDevContext context = newContextWithFreePort(freePort());
        GitManager manager = new GitManager(context.getRepositoryRoot());
        GitBaseline baseline = GitBaseline.capture(context, manager);
        GitBaseline.persist(context, baseline);

        // Dirty-worktree protection: pre-existing changes are excluded from
        // iteration attribution (covered in WP9 tests; re-asserted here).
        List<String> current = new ArrayList<>(baseline.getStatusLines());
        current.add(" M iteration-owned-file.java");
        Set<String> changed = baseline.iterationChangedPaths(current);
        assertTrue(changed.contains("iteration-owned-file.java"));
        assertFalse("Pre-existing baseline entries must not be attributed", changed.containsAll(baseline.getStatusLines()));

        // Commit without evidence is blocked; push without authorization is pending.
        GitManager.GitOperationResult commit = manager.commitWithEligibility("x", context);
        assertFalse(commit.isSuccess());
        GitManager.GitOperationResult push = manager.pushWithAuthorization("origin", "master", context);
        assertFalse(push.isSuccess());
        assertTrue(push.getMessage().contains("PUSH_PENDING"));

        // Read-only verification: repo unchanged.
        GitManager readOnly = new GitManager(context.getRepositoryRoot());
        assertEquals(headBefore, readOnly.getHeadCommit());
        assertEquals(statusBefore, readOnly.getStatusPorcelain());
    }

    // ------------------------------------------------------------------
    // Scenario F - repeated iterations, leak detection
    // ------------------------------------------------------------------

    @Test
    public void scenarioF_repeatedIterationsNoLeaks() throws Exception {
        List<ProcessOwnershipRecord> owners = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int port = freePort();
            SelfDevContext context = newContextWithFreePort(port);
            File productZip = buildProductZip(context, false);
            recordBuildExportEvidence(context, productZip);

            StartEvoTask task = new StartEvoTask("START_EVO");
            TaskResult res = task.execute(context);
            assertTrue("Iteration " + i + " start failed: " + res.getMessage(), res.isSuccess());

            ProcessOwnershipRecord ownership =
                    ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);
            assertNotNull(ownership);
            owners.add(ownership);
            childPids.add(ownership.getPid());

            // Exactly one child per run: no other stub child besides this one.
            long aliveStubs = owners.stream()
                    .filter(o -> ProcessHandle.of(o.getPid()).map(ProcessHandle::isAlive).orElse(false))
                    .count();
            assertEquals("Exactly one expected child per active run", 1, aliveStubs);

            EvoRcpRuntime runtime = new EvoRcpRuntime();
            TaskResult stopRes = runtime.stop(context);
            assertTrue("Iteration " + i + " stop failed: " + stopRes.getMessage(), stopRes.isSuccess());
            childPids.remove(ownership.getPid());
        }

        // After both iterations: zero live stub children.
        long stillAlive = owners.stream()
                .filter(o -> ProcessHandle.of(o.getPid()).map(ProcessHandle::isAlive).orElse(false))
                .count();
        assertEquals("No leaked children after repeated iterations", 0, stillAlive);
    }

    // ------------------------------------------------------------------
    // Fault injections
    // ------------------------------------------------------------------

    @Test
    public void faultInjection_interruptedDeploymentFailsHonestly() throws Exception {
        SelfDevContext context = newContextWithFreePort(freePort());
        File garbageZip = new File(tempFolder.getRoot(), "garbage.zip");
        Files.writeString(garbageZip.toPath(), "this is not a zip archive at all");
        ArtifactManifestStore store = context.getArtifactManifestStore();
        BuildArtifact artifact = store.record(ArtifactType.EVO_RCP, garbageZip, headBefore,
                store.nextBuildId(), "SUCCESS", "BUILD_VERIFY_EXECUTED", "SUCCESS", new HashMap<>());
        context.recordArtifact(artifact);

        eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpDeployer deployer =
                new eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpDeployer();
        TaskResult res = deployer.deploy(context, artifact);
        assertFalse("Corrupted archive deployment must fail", res.isSuccess());
    }
}
