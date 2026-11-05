package eu.kalafatic.evolution.selfdev.genome.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

import org.junit.Test;

import eu.kalafatic.evolution.selfdev.genome.milestone.GenomeGenerationProgressListener;
import eu.kalafatic.evolution.selfdev.genome.milestone.GenomeGenerationStage;
import eu.kalafatic.evolution.selfdev.genome.model.GenomeUpdateResult;
import eu.kalafatic.evolution.selfdev.genome.model.GenomeUpdateResult.GenomeUpdateStatus;

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

    @Test
    public void testUpdateGenomeLifecycleWithProgressListener() throws IOException {
        File repoDir = createTempRepo();
        try {
            LocalGenomeRepository repository = new LocalGenomeRepository();
            MockEventBus eventBus = new MockEventBus();
            MediatedPackageProcessor processor = new MediatedPackageProcessor();
            SecondhandUpgradeEngine upgradeEngine = new SecondhandUpgradeEngine(repository);
            SelfDevGenomeHub hub = new SelfDevGenomeHub(repository, eventBus, processor, upgradeEngine);

            List<GenomeGenerationStage> startedStages = new ArrayList<>();
            List<GenomeGenerationStage> completedStages = new ArrayList<>();

            GenomeGenerationProgressListener listener = new GenomeGenerationProgressListener() {
                @Override
                public void onStageStarted(GenomeGenerationStage stage, String message) {
                    startedStages.add(stage);
                }

                @Override
                public void onProgress(GenomeGenerationStage stage, long current, long total, String message) {
                }

                @Override
                public void onStageCompleted(GenomeGenerationStage stage, String message) {
                    completedStages.add(stage);
                }

                @Override
                public void onError(GenomeGenerationStage stage, String message, Throwable error) {
                }
            };

            // 1. Initial Update Genome
            GenomeUpdateResult res1 = hub.updateGenome(repoDir, "TestRepo", "v1.0.0", listener);
            assertNotNull(res1);
            assertTrue(res1.isSuccess());
            assertEquals(GenomeUpdateStatus.SUCCESS, res1.getStatus());
            assertTrue(res1.isHasChanges());
            assertTrue(res1.getScannedFiles() >= 2);
            assertTrue(res1.getNewFiles() >= 2);
            assertFalse(res1.getUpdatedDocuments().isEmpty());

            assertTrue(startedStages.contains(GenomeGenerationStage.VALIDATING));
            assertTrue(startedStages.contains(GenomeGenerationStage.SCANNING_FILES));
            assertTrue(startedStages.contains(GenomeGenerationStage.GENERATING_ARTIFACTS));
            assertTrue(startedStages.contains(GenomeGenerationStage.COMPLETED));

            File liveGenome = new File(repoDir, "genome/current/genome.json");
            File liveArch = new File(repoDir, "genome/current/architecture.md");
            assertTrue(liveGenome.exists());
            assertTrue(liveArch.exists());

            String archContent = Files.readString(liveArch.toPath(), StandardCharsets.UTF_8);
            assertTrue(archContent.contains("## Verified Source Code Facts (FACT)"));
            assertTrue(archContent.contains("## Architectural Observations (OBSERVATION)"));

            // 2. Idempotency Verification: Second run with NO changes
            startedStages.clear();
            completedStages.clear();

            GenomeUpdateResult res2 = hub.updateGenome(repoDir, "TestRepo", "v1.0.0", listener);
            assertNotNull(res2);
            assertTrue(res2.isSuccess());
            assertEquals(GenomeUpdateStatus.UNCHANGED, res2.getStatus());
            assertFalse(res2.isHasChanges());
            assertEquals(0, res2.getNewFiles());
            assertEquals(0, res2.getChangedFiles());
            assertEquals(0, res2.getRemovedFiles());
            assertTrue(res2.getUpdatedDocuments().isEmpty());
            assertTrue(startedStages.contains(GenomeGenerationStage.UNCHANGED));

            // 3. Incremental Change Detection: Add, Modify, Delete files
            File newFile = new File(repoDir, "src/NewClass.java");
            Files.writeString(newFile.toPath(), "package com.example; public class NewClass {}", StandardCharsets.UTF_8);

            File sampleFile = new File(repoDir, "src/SampleClass.java");
            Files.writeString(sampleFile.toPath(), "package com.example; public class SampleClass { int x = 42; }", StandardCharsets.UTF_8);

            File pomFile = new File(repoDir, "pom.xml");
            pomFile.delete();

            GenomeUpdateResult res3 = hub.updateGenome(repoDir, "TestRepo", "v1.0.0");
            assertNotNull(res3);
            assertTrue(res3.isSuccess());
            assertEquals(GenomeUpdateStatus.SUCCESS, res3.getStatus());
            assertTrue(res3.isHasChanges());
            assertEquals(1, res3.getNewFiles());
            assertEquals(1, res3.getChangedFiles());
            assertEquals(1, res3.getRemovedFiles());
            assertFalse(res3.getUpdatedDocuments().isEmpty());

        } finally {
            deleteDir(repoDir);
        }
    }

    @Test
    public void testEvolutionaryMemoryIntelligenceAndAtomicSafety() throws IOException {
        File repoDir = createTempRepo();
        try {
            LocalGenomeRepository repository = new LocalGenomeRepository();
            MockEventBus eventBus = new MockEventBus();
            MediatedPackageProcessor processor = new MediatedPackageProcessor();
            SecondhandUpgradeEngine upgradeEngine = new SecondhandUpgradeEngine(repository);
            SelfDevGenomeHub hub = new SelfDevGenomeHub(repository, eventBus, processor, upgradeEngine);

            // Create memory/audit.jsonl to simulate real previous execution experience
            File memDir = new File(repoDir, "memory");
            memDir.mkdirs();
            File auditFile = new File(memDir, "audit.jsonl");
            String auditJson1 = "{\"id\":\"audit-1\",\"strategy\":\"GENETIC_REPAIR\",\"result\":\"SUCCESS\",\"score\":0.95,\"timestamp\":1700000000,\"goal\":\"Fix broken parser\"}";
            String auditJson2 = "{\"id\":\"audit-2\",\"strategy\":\"BRUTE_FORCE_PATCH\",\"result\":\"FAIL\",\"score\":0.2,\"errorMessage\":\"Compilation timeout\",\"goal\":\"Attempt timeout fix\"}";
            Files.writeString(auditFile.toPath(), auditJson1 + "\n" + auditJson2 + "\n", StandardCharsets.UTF_8);

            // Execute Genome Update
            GenomeUpdateResult res = hub.updateGenome(repoDir, "EvoPlatform", "v1.0.0");
            assertNotNull(res);
            assertTrue(res.isSuccess());
            assertEquals(GenomeUpdateStatus.SUCCESS, res.getStatus());
            assertTrue(res.isHasChanges());

            // 1. Verify genome/current/genome.json structure
            File genomeJsonFile = new File(repoDir, "genome/current/genome.json");
            assertTrue("genome.json must exist in current", genomeJsonFile.exists());
            String jsonStr = Files.readString(genomeJsonFile.toPath(), StandardCharsets.UTF_8);

            // Check FACT, OBSERVATION, PROBLEM, RECOMMENDATION, HISTORY
            assertTrue(jsonStr.contains("\"facts\":"));
            assertTrue(jsonStr.contains("\"observations\":"));
            assertTrue(jsonStr.contains("\"problems\":"));
            assertTrue(jsonStr.contains("\"recommendations\":"));
            assertTrue(jsonStr.contains("\"history\":"));
            assertTrue(jsonStr.contains("\"experiences\":"));

            // Verify extracted execution experiences from memory
            assertTrue(jsonStr.contains("\"strategy\": \"GENETIC_REPAIR\""));
            assertTrue(jsonStr.contains("\"type\": \"SUCCESS\""));
            assertTrue(jsonStr.contains("\"strategy\": \"BRUTE_FORCE_PATCH\""));
            assertTrue(jsonStr.contains("\"type\": \"FAIL\""));
            assertTrue(jsonStr.contains("Compilation timeout"));

            // Verify recommendations learned from experiences
            assertTrue("Recommendations must include successful pattern",
                    jsonStr.contains("GENETIC_REPAIR") && jsonStr.contains("RECOMMENDATION:"));

            // Verify problems learned from failures
            assertTrue("Problems must include failure lesson",
                    jsonStr.contains("BRUTE_FORCE_PATCH") || jsonStr.contains("timeout"));

            // 2. Verify historical snapshot in genome-docs/ddMMyyyy/
            String datePattern = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
            File snapDir = new File(new File(repoDir, "genome-docs"), datePattern);
            assertTrue("genome-docs/" + datePattern + " must exist", snapDir.exists() && snapDir.isDirectory());
            File sysAnalysis = new File(snapDir, "SYSTEM_ANALYSIS_" + datePattern + ".md");
            assertTrue("SYSTEM_ANALYSIS markdown snapshot must exist", sysAnalysis.exists());

            // 3. Verify Atomic Failure Safety: Staging folder must be cleaned up if generation fails
            File corruptRepo = new File(repoDir, "non_existent_sub");
            GenomeUpdateResult failRes = hub.updateGenome(corruptRepo, "Corrupt", "v1.0.0");
            assertFalse(failRes.isSuccess());
            assertEquals(GenomeUpdateStatus.FAILED, failRes.getStatus());

            File[] leftOverStaging = new File(repoDir, "genome").listFiles((d, n) -> n.startsWith("current_tmp_"));
            assertTrue("Staging directories must be completely removed on completion or failure",
                    leftOverStaging == null || leftOverStaging.length == 0);

        } finally {
            deleteDir(repoDir);
        }
    }

    private File createTempRepo() throws IOException {
        File dir = Files.createTempDirectory("evo-genome-test-repo").toFile();
        File srcDir = new File(dir, "src");
        srcDir.mkdirs();

        File sampleJava = new File(srcDir, "SampleClass.java");
        Files.writeString(sampleJava.toPath(), "package com.example;\npublic class SampleClass {}\n", StandardCharsets.UTF_8);

        File pom = new File(dir, "pom.xml");
        Files.writeString(pom.toPath(), "<project><artifactId>sample</artifactId></project>\n", StandardCharsets.UTF_8);

        return dir;
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) deleteDir(child);
        }
        dir.delete();
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
