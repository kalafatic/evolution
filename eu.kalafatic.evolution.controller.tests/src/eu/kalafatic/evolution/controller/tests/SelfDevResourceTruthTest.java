package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpDeployer;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpVerifier;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflightResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * WP1 - Resource and workspace truth tests.
 * Verifies: collision-resistant run IDs, variable expansion in path resolution,
 * canonical validation of consumer-provided project roots, blocked-preflight
 * snapshot gating, zip-slip protection and the run-scoped readiness marker contract.
 */
public class SelfDevResourceTruthTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;
    private final List<File> createdRunDirs = new ArrayList<>();

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @After
    public void tearDown() {
        for (File dir : createdRunDirs) {
            try {
                deleteRecursively(dir);
            } catch (Exception ignored) {}
        }
        createdRunDirs.clear();
        resourceManager.refresh();
    }

    private void deleteRecursively(File f) throws IOException {
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

    // ------------------------------------------------------------------
    // 1. Run-ID collision resistance
    // ------------------------------------------------------------------

    @Test
    public void testRunIdsAreCollisionResistantWithinSameSecond() {
        SelfDevContext ctx1 = new SelfDevContext(null, resourceManager.getOrchestrator());
        SelfDevContext ctx2 = new SelfDevContext(null, resourceManager.getOrchestrator());

        assertNotNull(ctx1.getRunId());
        assertNotNull(ctx2.getRunId());
        assertFalse("Run IDs must differ: " + ctx1.getRunId(), ctx1.getRunId().equals(ctx2.getRunId()));

        File run1 = ctx1.getBuildDirectory().getParentFile();
        File run2 = ctx2.getBuildDirectory().getParentFile();
        assertFalse("Run directories must differ: " + run1.getAbsolutePath(), run1.equals(run2));
        createdRunDirs.add(run1);
        createdRunDirs.add(run2);
    }

    // ------------------------------------------------------------------
    // 2. Variable expansion in path resolution
    // ------------------------------------------------------------------

    @Test
    public void testResolvePathExpandsVariables() {
        String userHome = System.getProperty("user.home");
        File base = new File(userHome, "workspace");

        File expanded = SelfDevContext.resolvePath(base, "${WORKSPACE}/self-dev/x");
        assertNotNull(expanded);
        assertTrue(expanded.isAbsolute());
        assertEquals(new File(userHome, "workspace/self-dev/x").getAbsoluteFile(), expanded);

        File evoRootExpanded = SelfDevContext.resolvePath(null, "${EVO_ROOT}/eu.kalafatic.evolution.supervisor");
        assertEquals(new File(userHome, "git/evolution/eu.kalafatic.evolution.supervisor").getAbsoluteFile(), evoRootExpanded);

        File userHomeExpanded = SelfDevContext.resolvePath(base, "${user.home}/git/evolution");
        assertEquals(new File(userHome, "git/evolution").getAbsoluteFile(), userHomeExpanded);
    }

    // ------------------------------------------------------------------
    // 3. Canonical validation of consumer-provided project roots
    // ------------------------------------------------------------------

    @Test
    public void testNonCanonicalProjectRootFallsBackToRepositoryRoot() throws IOException {
        File outsideRoot = tempFolder.newFolder("outside-project");

        SelfDevContext context = new SelfDevContext(outsideRoot, resourceManager.getOrchestrator());
        File projectRoot = context.getProjectRoot();
        assertNotNull(projectRoot);
        String home = System.getProperty("user.home");
        assertTrue("Rejected non-canonical root leaked through: " + projectRoot,
                projectRoot.getAbsolutePath().startsWith(new File(home, "git").getAbsolutePath())
                || projectRoot.getAbsolutePath().startsWith(new File(home, "workspace").getAbsolutePath()));
        createdRunDirs.add(context.getBuildDirectory().getParentFile());
    }

    // ------------------------------------------------------------------
    // 4. Blocked preflight must not install a snapshot
    // ------------------------------------------------------------------

    @Test
    public void testBlockedPreflightDoesNotInstallSnapshot() {
        // Configure a canonical-root-resident but non-existent repository path
        // so that preflight deterministically returns BLOCKED.
        Orchestrator previousOrchestrator = resourceManager.getOrchestrator();
        try {
            Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
            Git badGit = OrchestrationFactory.eINSTANCE.createGit();
            badGit.setLocalPath(new File(System.getProperty("user.home"),
                    "git/selfdev-test-nonexistent-repo").getAbsolutePath());
            orch.setGit(badGit);
            resourceManager.setOrchestrator(orch);

            SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
            SelfDevOrchestrator orchestrator = new SelfDevOrchestrator(context, null, null);

            SelfDevPreflightResult result = orchestrator.executePreflight();
            assertEquals(SelfDevPreflightResult.PreflightStatus.BLOCKED, result.getStatus());
            assertNull("A BLOCKED preflight must never install a resolved resource snapshot",
                    context.getResolvedResources());
            createdRunDirs.add(context.getBuildDirectory().getParentFile());
        } finally {
            // Restore the shared ResourceManager singleton state to avoid
            // cross-class pollution in the OSGi test runtime.
            resourceManager.setOrchestrator(previousOrchestrator);
        }
    }

    // ------------------------------------------------------------------
    // 5. Zip-slip protection in EvoRcpDeployer
    // ------------------------------------------------------------------

    @Test
    public void testZipSlipRejectedByDeployer() throws IOException {
        File zip = new File(tempFolder.getRoot(), "evil.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("../escaped.txt"));
            zos.write("boom".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        BuildArtifact artifact = new BuildArtifact(ArtifactType.EVO_RCP, zip, "revision-1", "linux", new java.util.HashMap<>());

        EvoRcpDeployer deployer = new EvoRcpDeployer();
        TaskResult result = deployer.deploy(context, artifact);
        assertFalse("Zip-slip deploy must fail: " + result.getMessage(), result.isSuccess());

        File escaped = new File(context.getRuntimeDirectory(), "../escaped.txt").getAbsoluteFile();
        assertFalse("Escape file must not be created: " + escaped, escaped.exists());
        createdRunDirs.add(context.getBuildDirectory().getParentFile());
    }

    // ------------------------------------------------------------------
    // 6. Run-scoped readiness marker contract
    // ------------------------------------------------------------------

    @Test
    public void testReadinessFileDetectedInRunDirectory() throws IOException, InterruptedException {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return; // sleep-based probe process is POSIX-only
        }

        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        File runDir = context.getRuntimeDirectory().getParentFile();
        File runtimeDir = context.getRuntimeDirectory();
        File readyFile = new File(runDir, "evo_ready.txt");
        Files.writeString(readyFile.toPath(), String.valueOf(System.currentTimeMillis()));

        Process probe = new ProcessBuilder("sleep", "25").start();
        try {
            EvoRcpVerifier verifier = new EvoRcpVerifier();
            TaskResult result = verifier.verifyReady(probe, runtimeDir, null, -1, 5);
            assertTrue("Readiness file in run dir must be detected: " + result.getMessage(), result.isSuccess());
        } finally {
            probe.destroyForcibly();
            probe.waitFor();
        }
        createdRunDirs.add(runDir);
    }
}
