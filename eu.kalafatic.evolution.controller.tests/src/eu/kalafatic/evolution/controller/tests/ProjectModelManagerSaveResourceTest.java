package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.GenomeSnapshot;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.selfdev.genome.milestone.MilestoneGenerator;
import eu.kalafatic.evolution.selfdev.genome.model.GenomeUpdateResult;

/**
 * Unit test verifying ProjectModelManager.saveResource hardening and milestone snapshot logic.
 */
public class ProjectModelManagerSaveResourceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSaveResourceNullHandled() throws IOException {
        // Must handle null resource gracefully
        ProjectModelManager.getInstance().saveResource(null);
    }

    @Test
    public void testSaveResourceAssertionFailedExceptionHandled() throws IOException {
        // Verify that AssertionFailedException thrown during resource save is caught and handled safely
        ResourceImpl throwingResource = new ResourceImpl() {
            @Override
            public void save(Map<?, ?> options) throws IOException {
                throw new AssertionFailedException("null argument:");
            }
        };

        // Should not rethrow AssertionFailedException
        ProjectModelManager.getInstance().saveResource(throwingResource);
    }

    @Test
    public void testSaveResourceSkipsNoOpWriteAndWritesOnChange() throws Exception {
        File modelFile = new File(tempFolder.newFolder("model_dir"), "evo_config.xml");

        org.eclipse.emf.ecore.resource.Resource resource =
                ProjectModelManager.getInstance().getResourceSet()
                        .createResource(org.eclipse.emf.common.util.URI.createFileURI(modelFile.getAbsolutePath()));
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        orchestrator.setName("Initial");
        resource.getContents().add(orchestrator);

        // First save writes the serialized model to disk.
        ProjectModelManager.getInstance().saveResource(resource);
        assertTrue("Model file must be written", modelFile.isFile());
        byte[] first = Files.readAllBytes(modelFile.toPath());
        long firstMtime = Files.getLastModifiedTime(modelFile.toPath()).toMillis();

        Thread.sleep(120); // ensure the filesystem mtime granularity is exceeded

        // A no-op save (unchanged model) must NOT touch the file.
        ProjectModelManager.getInstance().saveResource(resource);
        assertArrayEquals("Unchanged save must leave identical bytes", first, Files.readAllBytes(modelFile.toPath()));
        assertEquals("Unchanged save must not rewrite the file", firstMtime,
                Files.getLastModifiedTime(modelFile.toPath()).toMillis());

        // A changed model must produce a fresh write.
        orchestrator.setName("Changed");
        ProjectModelManager.getInstance().saveResource(resource);
        byte[] third = Files.readAllBytes(modelFile.toPath());
        assertFalse("Changed save must write new content", java.util.Arrays.equals(first, third));
    }

    @Test
    public void testGenomeSnapshotManagement() {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        assertNotNull(orchestrator.getGenomeSnapshots());

        for (int i = 0; i < 10; i++) {
            GenomeSnapshot snapshot = OrchestrationFactory.eINSTANCE.createGenomeSnapshot();
            snapshot.setTimestamp("snapshot_" + i);
            orchestrator.getGenomeSnapshots().add(snapshot);
        }

        assertEquals(10, orchestrator.getGenomeSnapshots().size());

        // Simulate 8 retention policy enforcement
        while (orchestrator.getGenomeSnapshots().size() > 8) {
            orchestrator.getGenomeSnapshots().remove(0);
        }

        assertEquals(8, orchestrator.getGenomeSnapshots().size());
        assertEquals("snapshot_2", orchestrator.getGenomeSnapshots().get(0).getTimestamp());
    }

    @Test
    public void testMilestoneGeneratorDashboardArtifactGeneration() throws IOException {
        File repoRoot = tempFolder.newFolder("mock_repo");
        File sampleFile = new File(repoRoot, "Sample.java");
        Files.writeString(sampleFile.toPath(), "public class Sample {}");

        MilestoneGenerator generator = new MilestoneGenerator();
        GenomeUpdateResult result = generator.generateMilestone(repoRoot, "mock_repo", "v1");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getHistoricalSnapshotPath());

        File historyDir = new File(repoRoot, result.getHistoricalSnapshotPath());
        assertTrue("Historical snapshot directory should exist", historyDir.exists());

        File dashboardFile = new File(historyDir, "milestone_dashboard.html");
        assertTrue("milestone_dashboard.html must exist in historical snapshot directory", dashboardFile.exists());
        assertTrue("milestone_dashboard.html must not be empty", dashboardFile.length() > 0);
    }
}
