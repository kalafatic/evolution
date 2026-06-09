package eu.kalafatic.evolution.selfdev.genome.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact;
import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact;
import eu.kalafatic.evolution.selfdev.genome.core.Mode;
import eu.kalafatic.evolution.selfdev.genome.core.ProjectSnapshot;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEvent;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEventBus;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEventListener;
import eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub;
import eu.kalafatic.evolution.selfdev.genome.mediation.MediatedPackageProcessor;
import eu.kalafatic.evolution.selfdev.genome.repository.LocalGenomeRepository;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.ProjectContext;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.SecondhandUpgradeEngine;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradeContext;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradePlan;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradeProposal;

public class GenomeHubTest {

    @Test
    public void testUploadMediated() throws IOException {
        LocalGenomeRepository repository = new LocalGenomeRepository();
        MockEventBus eventBus = new MockEventBus();
        MediatedPackageProcessor processor = new MediatedPackageProcessor();
        SecondhandUpgradeEngine upgradeEngine = new SecondhandUpgradeEngine(repository);
        SelfDevGenomeHub hub = new SelfDevGenomeHub(repository, eventBus, processor, upgradeEngine);

        File zipFile = createSampleZip();
        try {
            GenomeArtifact artifact = hub.uploadMediated(zipFile, "test-project");

            assertNotNull(artifact);
            assertTrue(artifact instanceof MediatedPackageArtifact);
            assertEquals("test-topic", artifact.getTopic());
            assertEquals("test-project", artifact.getSourceProject());

            assertEquals(1, repository.findAll().size());
            assertEquals(1, eventBus.publishedEvents.size());
            assertEquals("NEW_MEDIATED_PACKAGE", eventBus.publishedEvents.get(0).getType());

            List<UpgradeProposal> proposals = hub.getUpgradeEngine().process(artifact, new ProjectContext());
            assertNotNull(proposals);
            assertTrue(proposals.size() > 0);

        } finally {
            zipFile.delete();
        }
    }

    @Test
    public void testUpgradePlanGeneration() {
        LocalGenomeRepository repository = new LocalGenomeRepository();
        MockEventBus eventBus = new MockEventBus();
        MediatedPackageProcessor processor = new MediatedPackageProcessor();
        SecondhandUpgradeEngine upgradeEngine = new SecondhandUpgradeEngine(repository);
        SelfDevGenomeHub hub = new SelfDevGenomeHub(repository, eventBus, processor, upgradeEngine);

        UpgradeContext context = new UpgradeContext();
        context.setMode(Mode.SELF_DEV);
        ProjectSnapshot snapshot = new ProjectSnapshot();
        snapshot.setProjectName("ECOS");
        context.setProject(snapshot);

        UpgradePlan plan = hub.generateUpgradePlan(context);

        assertNotNull(plan);
        assertNotNull(plan.getPlanId());
        assertEquals(Mode.SELF_DEV, plan.getMode());
        assertEquals("ECOS", plan.getTargetProject());
        assertTrue(plan.getExpectedFitnessGain() > 0);
        assertNotNull(plan.getReasoningSteps());
        assertNotNull(plan.getValidationHints());
    }

    private File createSampleZip() throws IOException {
        File zipFile = File.createTempFile("genome-test", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("METADATA.yaml");
            zos.putNextEntry(entry);
            zos.write("test-topic".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return zipFile;
    }

    private static class MockEventBus implements GenomeEventBus {
        java.util.List<GenomeEvent> publishedEvents = new java.util.ArrayList<>();

        @Override
        public void publish(GenomeEvent event) {
            publishedEvents.add(event);
        }

        @Override
        public void subscribe(GenomeEventListener listener) {
        }
    }
}
