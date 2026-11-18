package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactManifestStore;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.PromoteTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.RollbackTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP10 - Rollback and recovery tests: honest aggregation, deterministic
 * known-good selection, checksum verification, scope isolation.
 * Git-local steps are simulation-only (WP9 boundary).
 */
public class SelfDevRollbackTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;
    private final java.util.List<File> createdRunDirs = new java.util.ArrayList<>();

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
    public void testPromotedIndexNewestVerifiedSelection() throws Exception {
        File promotedDir = tempFolder.newFolder("promoted");
        File old = new File(promotedDir, "old.tar.gz");
        File newer = new File(promotedDir, "new.tar.gz");
        Files.writeString(old.toPath(), "old-artifact");
        Files.writeString(newer.toPath(), "new-artifact");

        BuildArtifact oldA = new BuildArtifact(ArtifactType.EVO_RCP, old, "r1", null, null,
                "run-x", "b1", old.length(), ArtifactManifestStore.computeSha256(old), "SUCCESS", "T", "SUCCESS");
        BuildArtifact newA = new BuildArtifact(ArtifactType.EVO_RCP, newer, "r2", null, null,
                "run-x", "b2", newer.length(), ArtifactManifestStore.computeSha256(newer), "SUCCESS", "T", "SUCCESS");
        PromoteTask.appendPromotedIndex(promotedDir, oldA);
        PromoteTask.appendPromotedIndex(promotedDir, newA);

        BuildArtifact selected = PromoteTask.findNewestVerifiedPromoted(promotedDir);
        assertNotNull(selected);
        assertEquals("Newest verified promoted artifact must be selected", newer.getAbsolutePath(), selected.getPath().getAbsolutePath());

        // Tamper with the newest: fallback to the older verified one.
        Files.writeString(newer.toPath(), "tampered!!");
        BuildArtifact fallback = PromoteTask.findNewestVerifiedPromoted(promotedDir);
        assertNotNull(fallback);
        assertEquals(old.getAbsolutePath(), fallback.getPath().getAbsolutePath());
    }

    @Test
    public void testRollbackFailsHonestlyWithoutKnownGood() {
        SelfDevContext context = newContext();
        // Remove the promoted dir entirely -> no known-good available.
        File promotedDir = new File(context.getExportDirectory(), "promoted");
        promotedDir.mkdirs(); // empty

        RollbackTask task = new RollbackTask("ROLLBACK",
                List.of(RollbackTask.RollbackScope.PROCESS_ONLY, RollbackTask.RollbackScope.DEPLOYMENT));
        TaskResult res = task.execute(context);
        assertEquals("Rollback without a verified known-good must FAIL (D-04)", TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("no verified known-good artifact"));
        assertFalse("Failure message must not claim success", res.getMessage().contains("completed successfully"));
    }

    @Test
    public void testProcessOnlyRollbackIsHonestNoop() {
        SelfDevContext context = newContext();
        RollbackTask task = new RollbackTask("ROLLBACK",
                List.of(RollbackTask.RollbackScope.PROCESS_ONLY));
        TaskResult res = task.execute(context);
        assertEquals(TaskStatus.SUCCESS, res.getStatus());
        assertNotNull(res.getDiagnostic("PROCESS_ONLY.STOP_EVO", null));
        assertNotNull(res.getDiagnostic("PROCESS_ONLY.STOP_SUPERVISOR", null));
    }

    @Test
    public void testGitLocalRollbackFailsWithoutBaseline() {
        SelfDevContext context = newContext();
        // Point the prepared reactor at the real repository (read-only + simulated
        // operations only; no real git mutation occurs).
        final File realRepo = context.getRepositoryRoot();
        SelfDevContext scopedContext = new SelfDevContext(null, resourceManager.getOrchestrator()) {
            @Override
            public File getPreparedReactorDirectory() {
                return realRepo;
            }
        };
        createdRunDirs.add(scopedContext.getBuildDirectory().getParentFile());

        RollbackTask task = new RollbackTask("ROLLBACK",
                List.of(RollbackTask.RollbackScope.GIT_LOCAL));
        TaskResult res = task.execute(scopedContext);
        assertEquals("Git-local rollback without a baseline must FAIL", TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("no iteration baseline"));
    }

    @Test
    public void testSourceCopyScopeAloneDoesNotTouchDeployment() {
        SelfDevContext context = newContext();
        // No prepared reactor content: SOURCE_COPY-only scope must be a no-op
        // success and must not evaluate DEPLOYMENT.
        RollbackTask task = new RollbackTask("ROLLBACK",
                List.of(RollbackTask.RollbackScope.SOURCE_COPY));
        TaskResult res = task.execute(context);
        assertEquals(TaskStatus.SUCCESS, res.getStatus());
        assertNull("DEPLOYMENT step must not run for SOURCE_COPY-only scope",
                res.getDiagnostic("DEPLOYMENT.RESTORE", null));
        assertNotNull(res.getDiagnostic("GIT_LOCAL.RESTORE", null));
    }
}
