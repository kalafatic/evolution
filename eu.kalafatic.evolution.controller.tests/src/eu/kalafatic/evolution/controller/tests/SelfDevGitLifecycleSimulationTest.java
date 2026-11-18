package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactManifestStore;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitBaseline;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP9 - Git lifecycle simulation tests.
 * ZERO real Git mutations: every test verifies the repository state (read-only)
 * before and after simulated operations and asserts it is unchanged.
 */
public class SelfDevGitLifecycleSimulationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;
    private final List<File> createdRunDirs = new ArrayList<>();
    private File realRepo;
    private String headBefore;
    private String statusBefore;

    @Before
    public void setUp() throws Exception {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
        SelfDevContext probe = new SelfDevContext(null, resourceManager.getOrchestrator());
        realRepo = probe.getRepositoryRoot();
        createdRunDirs.add(probe.getBuildDirectory().getParentFile());

        GitManager readOnly = new GitManager(realRepo);
        headBefore = readOnly.getHeadCommit();
        statusBefore = readOnly.getStatusPorcelain();
    }

    @After
    public void tearDown() throws Exception {
        for (File dir : createdRunDirs) {
            try {
                deleteRecursively(dir);
            } catch (Exception ignored) {}
        }
        createdRunDirs.clear();
        // Final read-only assertion: the repository is unchanged by the tests.
        if (realRepo != null) {
            GitManager readOnly = new GitManager(realRepo);
            assertEquals("HEAD must be unchanged (zero real git mutations)", headBefore, readOnly.getHeadCommit());
            assertEquals("Working tree must be unchanged (zero real git mutations)", statusBefore, readOnly.getStatusPorcelain());
        }
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

    @Test
    public void testSimulationModeIsDefault() {
        GitManager manager = new GitManager(realRepo);
        assertTrue("Git simulation mode must be enabled by default", manager.isSimulationMode());
    }

    @Test
    public void testBaselineCaptureIsReadOnlyAndPersisted() throws Exception {
        SelfDevContext context = newContext();
        GitManager manager = new GitManager(realRepo);
        GitBaseline baseline = GitBaseline.capture(context, manager);
        assertNotNull(baseline.getBranch());
        assertNotNull(baseline.getHead());
        assertEquals(realRepo.getCanonicalFile().toString().length() > 0, true);

        GitBaseline.persist(context, baseline);
        File baselineFile = GitBaseline.baselineFile(context);
        assertNotNull(baselineFile);
        assertTrue("Baseline must be persisted to the run directory", baselineFile.exists());

        GitBaseline reloaded = GitBaseline.load(context);
        assertNotNull(reloaded);
        assertEquals(baseline.getHead(), reloaded.getHead());
        assertEquals(baseline.getBranch(), reloaded.getBranch());
        assertEquals(baseline.getStatusLines(), reloaded.getStatusLines());
    }

    @Test
    public void testCommitEligibilityGateBlocksWithoutEvidence() {
        SelfDevContext context = newContext();
        GitManager manager = new GitManager(realRepo);
        GitManager.GitOperationResult res = manager.commitWithEligibility("iteration change", context);
        assertFalse("Commit without build/test/export evidence must be blocked", res.isSuccess());
        assertTrue(res.getMessage().contains("eligibility"));
        assertEquals(0, manager.getSimulatedOperations().size());
    }

    @Test
    public void testEligibleCommitIsSimulatedAndRepoUnchanged() throws Exception {
        SelfDevContext context = newContext();

        // Record honest evidence: BUILD_EVO + EXPORT_EVO success and a manifest
        // entry with non-skipped test result.
        context.recordTaskResult(TaskResult.success("BUILD_EVO", "build ok"));
        context.recordTaskResult(TaskResult.success("EXPORT_EVO", "export ok"));
        File artifact = new File(context.getExportDirectory(), "evo-fake.tar.gz");
        artifact.getParentFile().mkdirs();
        Files.writeString(artifact.toPath(), "fake artifact");
        ArtifactManifestStore store = context.getArtifactManifestStore();
        store.record(ArtifactType.EVO_RCP, artifact, headBefore, store.nextBuildId(),
                "SUCCESS", "BUILD_VERIFY_EXECUTED", "SUCCESS", new HashMap<>());

        GitManager manager = new GitManager(realRepo);
        GitManager.GitOperationResult res = manager.commitWithEligibility("iteration change", context);
        assertTrue("Eligible commit must succeed (simulated): " + res.getMessage(), res.isSuccess());
        assertTrue("Commit must be simulated", res.isSimulated());
        assertFalse("Simulated operations must be recorded", manager.getSimulatedOperations().isEmpty());

        // Read-only verification: nothing changed.
        GitManager readOnly = new GitManager(realRepo);
        assertEquals(headBefore, readOnly.getHeadCommit());
        assertEquals(statusBefore, readOnly.getStatusPorcelain());
    }

    @Test
    public void testRollbackRefusedWithoutBaseline() throws Exception {
        GitManager manager = new GitManager(realRepo);
        try {
            manager.rollback(null);
        } catch (Exception e) {
            // Real mode would throw; simulation mode returns after refusing.
        }
        boolean refused = manager.getSimulatedOperations().stream().anyMatch(o -> o.contains("REFUSED"));
        assertTrue("Destructive rollback must be refused without a baseline", refused);
        GitManager readOnly = new GitManager(realRepo);
        assertEquals(headBefore, readOnly.getHeadCommit());
    }

    @Test
    public void testRestoreIterationBaselineIsScopedAndSimulated() throws Exception {
        SelfDevContext context = newContext();
        GitManager manager = new GitManager(realRepo);
        GitBaseline baseline = GitBaseline.capture(context, manager);

        List<String> changed = Arrays.asList("SELF-DEV-TEST-PLACEHOLDER.txt");
        GitManager.GitOperationResult res = manager.restoreIterationBaseline(baseline, changed, null);
        assertTrue("Scoped restoration must succeed (simulated): " + res.getMessage(), res.isSuccess());
        assertTrue(res.isSimulated());
        // The placeholder must NOT have been created or touched.
        assertFalse(new File(realRepo, "SELF-DEV-TEST-PLACEHOLDER.txt").exists());

        // Unsafe paths are refused.
        GitManager.GitOperationResult unsafe = manager.restoreIterationBaseline(baseline, Arrays.asList("../evil"), null);
        assertFalse("Traversal paths must be refused", unsafe.isSuccess());
        assertTrue(unsafe.getMessage().contains("unsafe"));
    }

    @Test
    public void testIterationChangedPathsExcludePreExistingChanges() {
        GitBaseline baseline = new GitBaseline("run-1", "master", "abc123",
                Arrays.asList(" M eu/kalafatic/existing.java", "?? preexisting-untracked.txt"),
                System.currentTimeMillis());
        List<String> current = Arrays.asList(
                " M eu/kalafatic/existing.java",
                "?? preexisting-untracked.txt",
                " M eu/kalafatic/newfile.java",
                "?? iteration-untracked.java");
        Set<String> changed = baseline.iterationChangedPaths(current);
        assertEquals("Only iteration-owned changes must be attributed",
                Set.of("eu/kalafatic/newfile.java", "iteration-untracked.java"), changed);
        assertFalse("Pre-existing changes must never be attributed to the iteration",
                changed.contains("eu/kalafatic/existing.java"));
    }

    @Test
    public void testPushRequiresExplicitAuthorization() throws Exception {
        SelfDevContext context = newContext();
        GitManager manager = new GitManager(realRepo);

        // Without authorization: PENDING_AUTHORIZATION, no simulated push.
        GitManager.GitOperationResult pending = manager.pushWithAuthorization("origin", "master", context);
        assertFalse("Push must not proceed without explicit authorization", pending.isSuccess());
        assertTrue(pending.getMessage().contains("PUSH_PENDING"));

        // With authorization: simulated success.
        context.setGitPushAuthorized(true);
        GitManager.GitOperationResult authorized = manager.pushWithAuthorization("origin", "master", context);
        assertTrue("Authorized push must succeed (simulated)", authorized.isSuccess());
        assertTrue(authorized.isSimulated());

        // Force-push is refused even with authorization.
        GitManager.GitOperationResult force = manager.pushWithAuthorization("origin -f", "master", context);
        assertFalse("Force-push must be refused", force.isSuccess());
        assertTrue(force.getMessage().contains("Force-push"));

        GitManager readOnly = new GitManager(realRepo);
        assertEquals(headBefore, readOnly.getHeadCommit());
        assertEquals(statusBefore, readOnly.getStatusPorcelain());
    }

    @Test
    public void testAllMutatingOperationsAreSimulated() throws Exception {
        SelfDevContext context = newContext();
        GitManager manager = new GitManager(realRepo);
        GitBaseline baseline = GitBaseline.capture(context, manager);

        manager.createBranch("selfdev-sim-test");
        manager.forceCheckout("master");
        manager.merge("master");
        manager.createWorktree("master", new File(tempFolder.getRoot(), "sim-worktree").getAbsolutePath());
        manager.removeWorktree(new File(tempFolder.getRoot(), "sim-worktree").getAbsolutePath());
        manager.pruneWorktrees();
        manager.commitWithEligibility("x", context); // eligibility-blocked, no op

        assertEquals("All mutating operations must be recorded as simulated", 6, manager.getSimulatedOperations().size());
        for (String op : manager.getSimulatedOperations()) {
            assertTrue("Every recorded operation must be marked simulated: " + op, op.startsWith("[SIMULATED]"));
        }

        GitManager readOnly = new GitManager(realRepo);
        assertEquals(headBefore, readOnly.getHeadCommit());
        assertEquals(statusBefore, readOnly.getStatusPorcelain());
    }
}
