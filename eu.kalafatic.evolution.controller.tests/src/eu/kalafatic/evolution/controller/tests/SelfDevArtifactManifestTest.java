package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactManifestStore;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ArtifactType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildArtifact;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ExportSupervisorTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MavenSupervisorBuilder;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StartEvoTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP4 - Artifact manifest and provenance tests.
 */
public class SelfDevArtifactManifestTest {

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
    public void testRecordAndResolveRoundTrip() throws Exception {
        SelfDevContext context = newContext();
        File artifactFile = new File(context.getExportDirectory(), "evo.zip");
        if (!artifactFile.getParentFile().exists()) artifactFile.getParentFile().mkdirs();
        Files.writeString(artifactFile.toPath(), "fake product bytes");

        ArtifactManifestStore store = context.getArtifactManifestStore();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("productId", "evolution");
        metadata.put("os", "linux");
        metadata.put("arch", "x86_64");
        BuildArtifact recorded = store.record(ArtifactType.EVO_RCP, artifactFile, "rev-123",
                store.nextBuildId(), "SUCCESS", "BUILD_EXECUTED", null, metadata);

        assertNotNull(recorded.getBuildId());
        assertEquals(context.getRunId(), recorded.getRunId());
        assertEquals("rev-123", recorded.getSourceRevision());
        assertNotNull(recorded.getSha256());
        assertTrue(recorded.getSize() > 0);
        assertEquals("evolution", recorded.getProductId());

        // Manifest persisted to disk
        assertTrue("Manifest file must be persisted", store.getManifestFile().exists());

        // A fresh store instance re-loads the same identity
        ArtifactManifestStore reloaded = context.getArtifactManifestStore();
        BuildArtifact resolved = reloaded.resolve(ArtifactType.EVO_RCP);
        assertNotNull(resolved);
        assertEquals(recorded.getBuildId(), resolved.getBuildId());
        assertEquals(recorded.getSha256(), resolved.getSha256());
        assertEquals(recorded.getPath().getAbsolutePath(), resolved.getPath().getAbsolutePath());
    }

    @Test
    public void testChecksumMismatchRejected() throws Exception {
        SelfDevContext context = newContext();
        File artifactFile = new File(context.getExportDirectory(), "evo2.zip");
        if (!artifactFile.getParentFile().exists()) artifactFile.getParentFile().mkdirs();
        Files.writeString(artifactFile.toPath(), "original bytes");

        ArtifactManifestStore store = context.getArtifactManifestStore();
        store.record(ArtifactType.EVO_RCP, artifactFile, "rev-1", store.nextBuildId(), "SUCCESS", null, null, new HashMap<>());

        // Tamper with the artifact after recording
        Files.writeString(artifactFile.toPath(), "tampered bytes!!");

        boolean threw = false;
        try {
            store.resolve(ArtifactType.EVO_RCP);
        } catch (java.io.IOException e) {
            threw = true;
            assertTrue(e.getMessage().contains("Checksum mismatch"));
        }
        assertTrue("Checksum mismatch must fail resolution", threw);
    }

    @Test
    public void testStaleManifestEntryRejected() throws Exception {
        SelfDevContext context = newContext();
        File artifactFile = new File(context.getExportDirectory(), "evo3.zip");
        if (!artifactFile.getParentFile().exists()) artifactFile.getParentFile().mkdirs();
        Files.writeString(artifactFile.toPath(), "bytes");

        ArtifactManifestStore store = context.getArtifactManifestStore();
        store.record(ArtifactType.EVO_RCP, artifactFile, "rev-1", store.nextBuildId(), "SUCCESS", null, null, new HashMap<>());

        Files.delete(artifactFile.toPath());

        boolean threw = false;
        try {
            store.resolve(ArtifactType.EVO_RCP);
        } catch (java.io.IOException e) {
            threw = true;
            assertTrue(e.getMessage().contains("missing on disk"));
        }
        assertTrue("Stale manifest entry must fail resolution", threw);
    }

    @Test
    public void testRehomePreservesIdentityAndVerifiesCopy() throws Exception {
        SelfDevContext context = newContext();
        File source = new File(context.getExportDirectory(), "candidate.zip");
        if (!source.getParentFile().exists()) source.getParentFile().mkdirs();
        Files.writeString(source.toPath(), "candidate bytes");

        ArtifactManifestStore store = context.getArtifactManifestStore();
        BuildArtifact candidate = store.record(ArtifactType.EVO_RCP, source, "rev-9",
                store.nextBuildId(), "SUCCESS", "BUILD_EXECUTED", "SUCCESS", new HashMap<>());

        File promoted = new File(context.getExportDirectory(), "promoted/candidate.zip");
        promoted.getParentFile().mkdirs();
        Files.copy(source.toPath(), promoted.toPath());

        BuildArtifact rehomed = store.rehome(candidate, promoted, "SUCCESS");
        assertNotNull(rehomed);
        assertEquals("Promotion must preserve build identity", candidate.getBuildId(), rehomed.getBuildId());
        assertEquals("Promotion must preserve run identity", candidate.getRunId(), rehomed.getRunId());
        assertEquals("Promotion must preserve revision", candidate.getSourceRevision(), rehomed.getSourceRevision());
        assertTrue("Promoted copy must verify", store.verifyIntegrity(rehomed));

        // Tampered copy must fail verification
        Files.writeString(promoted.toPath(), "corrupted copy");
        assertFalse("Tampered promoted copy must fail integrity verification", store.verifyIntegrity(rehomed));
    }

    @Test
    public void testSupervisorJarSelectionIsDeterministicNotFirstMatch() throws Exception {
        File runDir = tempFolder.newFolder("supSel");
        SelfDevContext context = new SelfDevContext(new File(runDir, "fake"), runDir, resourceManager.getOrchestrator());

        File targetDir = new File(runDir, "supervisor-target/target");
        targetDir.mkdirs();
        Files.writeString(new File(targetDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar").toPath(), "shaded");
        Files.writeString(new File(targetDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT-sources.jar").toPath(), "sources");

        SelfDevContext deterministicContext = new SelfDevContext(new File(runDir, "fake"), runDir, resourceManager.getOrchestrator()) {
            @Override
            public File getSupervisorDirectory() {
                return targetDir.getParentFile();
            }
        };

        MavenSupervisorBuilder builder = new MavenSupervisorBuilder();
        BuildArtifact artifact = builder.getArtifact(deterministicContext);
        assertNotNull(artifact);
        assertEquals("Versioned shaded jar must be selected, not sources/first-match",
                "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar", artifact.getPath().getName());

        // Ambiguity: two versioned candidates must yield null (refuse first-match)
        Files.writeString(new File(targetDir, "eu.kalafatic.evolution.supervisor-2.0.0-SNAPSHOT.jar").toPath(), "other");
        assertNull("Ambiguous candidates must be refused", builder.getArtifact(deterministicContext));
    }

    @Test
    public void testStartEvoTaskDoesNotScanExportDirForArbitraryZips() throws Exception {
        File runDir = tempFolder.newFolder("startNoScan");
        SelfDevContext context = new SelfDevContext(new File(runDir, "fake"), runDir, resourceManager.getOrchestrator());

        // Plant an arbitrary zip in the export directory - the pre-validation must
        // NOT pick it up (WP4: no arbitrary ZIP scanning).
        File exportDir = context.getExportDirectory();
        exportDir.mkdirs();
        Files.writeString(new File(exportDir, "evolution-linux.gtk.x86_64.tar.gz").toPath(), "stale zip");

        StartEvoTask task = new StartEvoTask("START_EVO");
        TaskResult res = task.execute(context);
        assertEquals(TaskStatus.FAILED, res.getStatus());
        assertTrue("START_EVO must refuse to guess an artifact from the export dir: " + res.getMessage(),
                res.getMessage().contains("missing required EVO RCP build artifact"));
    }

    @Test
    public void testExportSupervisorTaskProducesExportRecord() throws Exception {
        File runDir = tempFolder.newFolder("supExport");
        SelfDevContext context = new SelfDevContext(new File(runDir, "fake"), runDir, resourceManager.getOrchestrator());

        File fakeJar = new File(runDir, "fake-supervisor.jar");
        Files.writeString(fakeJar.toPath(), "fake supervisor jar content");

        BuildArtifact buildArtifact = new BuildArtifact(ArtifactType.SUPERVISOR, fakeJar, "rev-1", null, null);
        context.recordArtifact(buildArtifact);

        ExportSupervisorTask task = new ExportSupervisorTask("EXPORT_SUPERVISOR");
        TaskResult res = task.execute(context);
        assertTrue("EXPORT_SUPERVISOR must succeed with a valid artifact: " + res.getMessage(), res.isSuccess());

        File exportJar = new File(context.getExportDirectory(), "eu.kalafatic.evolution.supervisor.jar");
        assertTrue("Export jar must exist in the export directory", exportJar.exists());

        ArtifactManifestStore store = context.getArtifactManifestStore();
        BuildArtifact recorded = store.resolve(ArtifactType.SUPERVISOR);
        assertNotNull("Manifest entry must be recorded for the supervisor export", recorded);
        assertEquals(exportJar.getAbsolutePath(), recorded.getPath().getAbsolutePath());
        assertEquals("SUCCESS", recorded.getExportResult());
        assertNotNull(recorded.getSha256());
    }
}
