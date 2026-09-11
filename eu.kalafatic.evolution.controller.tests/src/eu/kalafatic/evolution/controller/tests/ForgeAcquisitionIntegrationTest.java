package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.JobState;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.ModelObjective;
import eu.kalafatic.evolution.forge.controller.service.ForgeOrchestrator;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.preference.RequirementLevel;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.evaluation.DefaultPreferenceEvaluator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;

public class ForgeAcquisitionIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path projectDir;
    private Path localDataDir;

    @Before
    public void setUp() throws Exception {
        projectDir = tempFolder.newFolder("project").toPath();
        localDataDir = tempFolder.newFolder("localData").toPath();
    }

    @Test
    public void testMultiRoundAcquisitionReaches500MBTargetScenario() throws Exception {
        long targetBytes = 500_000_000L; // 500 MB

        // Simulated datasets: Round 1 -> dataset A (3MB), Round 2 -> B (120MB) & C (180MB), Round 3 -> D (220MB)
        DatasetSource srcA = createMockSource("dataset-A", 3_000_000L, "Dataset A text chunk for multi-round acquisition. ");
        DatasetSource srcB = createMockSource("dataset-B", 120_000_000L, "Dataset B text chunk for multi-round acquisition. ");
        DatasetSource srcC = createMockSource("dataset-C", 180_000_000L, "Dataset C text chunk for multi-round acquisition. ");
        DatasetSource srcD = createMockSource("dataset-D", 220_000_000L, "Dataset D text chunk for multi-round acquisition. ");

        AtomicInteger roundCall = new AtomicInteger(0);
        TrainingDataSourceDiscovery mockDiscovery = prefs -> {
            int round = roundCall.getAndIncrement();
            if (round == 0) {
                return List.of(new DataSourceCandidate("dataset-A", "HUGGING_FACE", srcA, "en", List.of("text"), 3_000_000L, 0.95));
            } else if (round == 1) {
                return List.of(
                        new DataSourceCandidate("dataset-B", "HUGGING_FACE", srcB, "en", List.of("text"), 120_000_000L, 0.90),
                        new DataSourceCandidate("dataset-C", "HUGGING_FACE", srcC, "en", List.of("text"), 180_000_000L, 0.85)
                );
            } else {
                return List.of(new DataSourceCandidate("dataset-D", "HUGGING_FACE", srcD, "en", List.of("text"), 220_000_000L, 0.80));
            }
        };

        TrainingDataAcquisitionServiceImpl acquisitionService = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(true),
                new DefaultPreferenceEvaluator(),
                mockDiscovery
        );

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(targetBytes)
                .targetUsableBytes(targetBytes)
                .sizeRequirementLevel(RequirementLevel.HARD)
                .build();

        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest().setPreferences(prefs);
        TrainingDataAcquisitionResult result = acquisitionService.acquireDataset(request);

        assertTrue("Acquisition MUST NOT stop after 3MB dataset A. Total acquired: " + result.getUsableContentBytes() + " / " + targetBytes,
                result.getUsableContentBytes() >= targetBytes);
        assertTrue("targetReached MUST be true", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.READY, result.getStatus());
        assertTrue("Multiple sources MUST be acquired (A, B, C, D)", result.getSourcesUsed().size() >= 4);
    }

    @Test
    public void testNegativeAcquisitionExhaustionReturnsInsufficientSourceData() throws Exception {
        long targetBytes = 500_000_000L; // 500 MB

        DatasetSource srcA = createMockSource("dataset-A", 3_000_000L, "Text chunk A. ");
        DatasetSource srcB = createMockSource("dataset-B", 5_000_000L, "Text chunk B. ");
        DatasetSource failingSrc = new DatasetSource() {
            private final DatasetSourceConfig cfg = new DatasetSourceConfig("MOCK", "dataset-C-failing");
            private final DatasetSourceStats stats = new DatasetSourceStats();
            @Override public String getSourceName() { return "dataset-C-failing"; }
            @Override public DatasetSourceConfig getConfig() { return cfg; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() throws Exception { throw new IOException("HTTP 500 Internal Server Error"); }
            @Override public boolean hasNext() { return false; }
            @Override public NormalizedSample next() { return null; }
            @Override public void close() {}
        };
        DatasetSource srcD = createMockSource("dataset-D", 10_000_000L, "Text chunk D. ");

        TrainingDataSourceDiscovery finiteDiscovery = prefs -> List.of(
                new DataSourceCandidate("dataset-A", "HUGGING_FACE", srcA, "en", List.of("text"), 3_000_000L, 0.9),
                new DataSourceCandidate("dataset-B", "HUGGING_FACE", srcB, "en", List.of("text"), 5_000_000L, 0.8),
                new DataSourceCandidate("dataset-C-failing", "HUGGING_FACE", failingSrc, "en", List.of("text"), 0L, 0.7),
                new DataSourceCandidate("dataset-D", "HUGGING_FACE", srcD, "en", List.of("text"), 10_000_000L, 0.6)
        );

        TrainingDataAcquisitionServiceImpl acquisitionService = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(true),
                new DefaultPreferenceEvaluator(),
                finiteDiscovery
        );

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(targetBytes)
                .targetUsableBytes(targetBytes)
                .sizeRequirementLevel(RequirementLevel.HARD)
                .build();

        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest().setPreferences(prefs);
        TrainingDataAcquisitionResult result = acquisitionService.acquireDataset(request);

        assertFalse("targetReached MUST be false when total acquired < 500MB target", result.isTargetReached());
        assertEquals("Status MUST be INSUFFICIENT_SOURCE_DATA when hard target is unsatisfied",
                TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA, result.getStatus());
        assertTrue("Shortfall bytes must be positive", result.getShortfallBytes() > 0);
        assertNotNull(result.getFailureReason());
    }

    @Test
    public void testForgeOrchestratorRefusesToTrainWithInsufficientData() throws Exception {
        // Set local source file that yields only a few bytes (less than requested minimum)
        Path sampleFile = localDataDir.resolve("sample.txt");
        Files.writeString(sampleFile, "Short text sample.");

        ForgeJob job = new ForgeJob("forge-insufficient-data-test");
        job.setObjective(ModelObjective.GENERAL);
        job.setSourcePaths(List.of(sampleFile.toAbsolutePath().toString()));
        job.setRequestedMinimumUsableBytes(500_000_000L); // Require 500 MB

        job.getModelConfig().setModelSize("NANO");
        job.getModelConfig().setHiddenSize(128);
        job.getModelConfig().setLayers(2);
        job.getModelConfig().setHeads(4);
        job.getModelConfig().setDff(256);
        job.getModelConfig().setMaxSeqLen(128);
        job.getTrainingConfig().setEpochs(1);

        ForgeOrchestrator orchestrator = new ForgeOrchestratorImpl();

        try {
            orchestrator.executeJob(job, projectDir);
            fail("ForgeOrchestrator MUST throw IllegalStateException when training data acquisition fails to satisfy requested target.");
        } catch (IllegalStateException ex) {
            assertTrue("Exception message should indicate acquisition incomplete", ex.getMessage().contains("Training data acquisition incomplete"));
            assertEquals("Job state should be marked FAILED", JobState.FAILED, job.getState());
            assertNotNull("Job failure reason should be populated", job.getFailureReason());
        }
    }

    @Test
    public void testForgeOrchestratorCompletesWhenTargetSatisfied() throws Exception {
        Path sampleFile = localDataDir.resolve("sample.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("EVO local dataset sample line ").append(i).append(" providing valid training content for Forge.\n");
        }
        Files.writeString(sampleFile, sb.toString());
        long fileSize = Files.size(sampleFile);

        ForgeJob job = new ForgeJob("forge-sufficient-data-test");
        job.setObjective(ModelObjective.GENERAL);
        job.setSourcePaths(List.of(sampleFile.toAbsolutePath().toString()));
        job.setRequestedMinimumUsableBytes(fileSize / 2); // Requested target is less than local file size

        job.getModelConfig().setModelSize("NANO");
        job.getModelConfig().setHiddenSize(128);
        job.getModelConfig().setLayers(2);
        job.getModelConfig().setHeads(4);
        job.getModelConfig().setDff(256);
        job.getModelConfig().setMaxSeqLen(128);
        job.getTrainingConfig().setEpochs(1);

        ForgeOrchestrator orchestrator = new ForgeOrchestratorImpl();
        ForgeJob completedJob = orchestrator.executeJob(job, projectDir);

        assertNotNull(completedJob);
        assertEquals("Job should complete successfully when target is satisfied", JobState.COMPLETED, completedJob.getState());
        assertTrue("Smoke test should pass", completedJob.getSmokeTestResult().isPassed());
    }

    private DatasetSource createMockSource(String name, long totalBytes, String textPrefix) {
        return new DatasetSource() {
            private long generatedBytes = 0;
            private final DatasetSourceConfig config = new DatasetSourceConfig("MOCK", name);
            private final DatasetSourceStats stats = new DatasetSourceStats();

            @Override public String getSourceName() { return name; }
            @Override public DatasetSourceConfig getConfig() { return config; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() {}

            @Override
            public boolean hasNext() {
                return generatedBytes < totalBytes;
            }

            @Override
            public NormalizedSample next() {
                String text = textPrefix + " sample chunk ending at " + (generatedBytes + 500) + " bytes. ";
                byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                generatedBytes += bytes.length;
                return NormalizedSample.createTextSample(text, name);
            }

            @Override public void close() {}
        };
    }
}
