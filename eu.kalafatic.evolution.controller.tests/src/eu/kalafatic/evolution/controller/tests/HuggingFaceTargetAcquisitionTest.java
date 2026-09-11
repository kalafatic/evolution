package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

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
import eu.kalafatic.evolution.forge.data.impl.discovery.HuggingFaceSourceDiscovery;
import eu.kalafatic.evolution.forge.data.impl.evaluation.DefaultPreferenceEvaluator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;

public class HuggingFaceTargetAcquisitionTest {

    @Test
    public void testSmallFirstResultTriggersContinuedMultiSourceAcquisition() throws Exception {
        // Target: 50,000 bytes
        long targetBytes = 50_000;

        // Source 1 (3KB - small first result scenario)
        DatasetSource srcA = createMockSource("dataset-A", 3_000, "Content string chunk for dataset A testing multi-source acquisition. ");
        // Source 2 (15KB)
        DatasetSource srcB = createMockSource("dataset-B", 15_000, "Unique content string chunk for dataset B testing multi-source acquisition. ");
        // Source 3 (20KB)
        DatasetSource srcC = createMockSource("dataset-C", 20_000, "Unique content string chunk for dataset C testing multi-source acquisition. ");
        // Source 4 (25KB)
        DatasetSource srcD = createMockSource("dataset-D", 25_000, "Unique content string chunk for dataset D testing multi-source acquisition. ");

        AtomicInteger roundCall = new AtomicInteger(0);
        TrainingDataSourceDiscovery multiRoundDiscovery = prefs -> {
            int round = roundCall.getAndIncrement();
            if (round == 0) {
                return List.of(new DataSourceCandidate("dataset-A", "HUGGING_FACE", srcA, "en", List.of("text"), 3_000, 0.95));
            } else if (round == 1) {
                return List.of(
                        new DataSourceCandidate("dataset-B", "HUGGING_FACE", srcB, "en", List.of("text"), 15_000, 0.90),
                        new DataSourceCandidate("dataset-C", "HUGGING_FACE", srcC, "en", List.of("text"), 20_000, 0.85)
                );
            } else {
                return List.of(new DataSourceCandidate("dataset-D", "HUGGING_FACE", srcD, "en", List.of("text"), 25_000, 0.80));
            }
        };

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(true),
                new DefaultPreferenceEvaluator(),
                multiRoundDiscovery
        );

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(targetBytes)
                .sizeRequirementLevel(RequirementLevel.HARD)
                .build();

        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest().setPreferences(prefs);
        TrainingDataAcquisitionResult result = service.acquireDataset(req);

        assertTrue("Acquisition MUST NOT stop after 3KB first result. Acquired: " + result.getUsableContentBytes() + " / " + targetBytes,
                result.getUsableContentBytes() >= targetBytes);
        assertTrue("targetReached must be true", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.READY, result.getStatus());
        assertTrue(result.getSourcesUsed().size() > 1);
    }

    @Test
    public void testShortfallExhaustionProducesInsufficientSourceDataStatus() throws Exception {
        // Target: 50,000 bytes, but available universe is only 20,000 bytes
        long targetBytes = 50_000;
        DatasetSource srcA = createMockSource("small-source-A", 10_000, "Text chunk A for shortfall testing. ");
        DatasetSource srcB = createMockSource("small-source-B", 10_000, "Text chunk B for shortfall testing. ");

        TrainingDataSourceDiscovery finiteDiscovery = prefs -> List.of(
                new DataSourceCandidate("small-source-A", "HUGGING_FACE", srcA, "en", List.of("chat"), 10_000, 0.9),
                new DataSourceCandidate("small-source-B", "HUGGING_FACE", srcB, "en", List.of("chat"), 10_000, 0.8)
        );

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(true),
                new DefaultPreferenceEvaluator(),
                finiteDiscovery
        );

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(targetBytes)
                .sizeRequirementLevel(RequirementLevel.HARD)
                .build();

        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest().setPreferences(prefs);
        TrainingDataAcquisitionResult result = service.acquireDataset(req);

        assertFalse("targetReached MUST be false when acquired bytes < requested target", result.isTargetReached());
        assertNotEquals("Status MUST NOT be READY when hard target is unsatisfied", TrainingDataAcquisitionResult.Status.READY, result.getStatus());
        assertEquals(TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA, result.getStatus());
        assertTrue("Shortfall bytes must be reported correctly", result.getShortfallBytes() > 0);
        assertNotNull(result.getFailureReason());
        assertTrue("Failure reason must detail shortfall", result.getFailureReason().contains("INSUFFICIENT_SOURCE_DATA"));
    }

    @Test
    public void testFailedCandidateIsSkippedWithoutFailingAcquisition() throws Exception {
        long targetBytes = 20_000;

        // Failing source throws exception on read
        DatasetSource failingSrc = new DatasetSource() {
            private final DatasetSourceConfig config = new DatasetSourceConfig("MOCK", "failing-source");
            private final DatasetSourceStats stats = new DatasetSourceStats();
            @Override public String getSourceName() { return "failing-source"; }
            @Override public DatasetSourceConfig getConfig() { return config; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() throws Exception { throw new IOException("HTTP 403 Forbidden / Gated Dataset"); }
            @Override public boolean hasNext() { return false; }
            @Override public NormalizedSample next() { return null; }
            @Override public void close() {}
        };

        DatasetSource validSrc = createMockSource("valid-source-B", 25_000, "Valid content chunk from source B. ");

        TrainingDataSourceDiscovery discovery = prefs -> List.of(
                new DataSourceCandidate("failing-source", "HUGGING_FACE", failingSrc, "en", List.of("code"), 10_000, 0.95),
                new DataSourceCandidate("valid-source-B", "HUGGING_FACE", validSrc, "en", List.of("code"), 25_000, 0.90)
        );

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(true),
                new DefaultPreferenceEvaluator(),
                discovery
        );

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(targetBytes)
                .build();

        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest().setPreferences(prefs);
        TrainingDataAcquisitionResult result = service.acquireDataset(req);

        assertTrue("Failing source candidate MUST NOT fail entire acquisition. Valid source B should be processed.",
                result.getUsableContentBytes() >= targetBytes);
        assertTrue("Target reached must be true", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.READY, result.getStatus());
    }

    @Test
    public void testDuplicateContentIsNotDoubleCountedTowardsTarget() throws Exception {
        long targetBytes = 10_000;

        // Source 1 provides 8,000 bytes
        DatasetSource src1 = createMockSource("src1", 8_000, "Identical content block repeated across sources. ");
        // Source 2 provides duplicate text
        DatasetSource src2 = createMockSource("src2", 8_000, "Identical content block repeated across sources. ");
        // Source 3 provides distinct 8,000 bytes
        DatasetSource src3 = createMockSource("src3", 8_000, "Unique distinct content block for source 3. ");

        TrainingDataSourceDiscovery discovery = prefs -> List.of(
                new DataSourceCandidate("src1", "HUGGING_FACE", src1, "en", List.of("text"), 8_000, 0.9),
                new DataSourceCandidate("src2", "HUGGING_FACE", src2, "en", List.of("text"), 8_000, 0.8),
                new DataSourceCandidate("src3", "HUGGING_FACE", src3, "en", List.of("text"), 8_000, 0.7)
        );

        TrainingDataAcquisitionServiceImpl service = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(),
                new TrainingSampleQualityScorer(0.5),
                new DatasetDeduplicator(true),
                new DefaultPreferenceEvaluator(),
                discovery
        );

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .minimumUsableBytes(targetBytes)
                .build();

        TrainingDataAcquisitionRequest req = new TrainingDataAcquisitionRequest().setPreferences(prefs);
        TrainingDataAcquisitionResult result = service.acquireDataset(req);

        assertTrue("Duplicate bytes from src2 must be rejected. Source 3 must be acquired to reach target.",
                result.getUsableContentBytes() >= targetBytes);
        assertTrue("Duplicate bytes count must be recorded", result.getDuplicateBytes() > 0);
        assertTrue("Target reached must be true", result.isTargetReached());
    }

    @Test
    public void testRealHuggingFaceDiscoveryPaginationAndMultiResult() {
        HuggingFaceSourceDiscovery discovery = new HuggingFaceSourceDiscovery();

        TrainingDataPreferences prefs = TrainingDataPreferences.builder()
                .addDomain("code")
                .capabilityObjective("programming")
                .minimumUsableBytes(100_000)
                .build();

        List<DataSourceCandidate> batch0 = discovery.discover(prefs);
        assertNotNull("Batch 0 discovery should return candidates list", batch0);
        assertFalse("Batch 0 candidates should not be empty", batch0.isEmpty());

        List<DataSourceCandidate> batch1 = discovery.discover(prefs);
        assertNotNull("Batch 1 (pagination/query expansion) should return candidates list", batch1);

        // Verify deduplication across rounds
        for (DataSourceCandidate c1 : batch0) {
            for (DataSourceCandidate c2 : batch1) {
                assertNotEquals("Discovery MUST NOT return duplicate repo candidates across rounds", c1.getDatasetId(), c2.getDatasetId());
            }
        }
    }

    private DatasetSource createMockSource(String name, long targetBytes, String textPrefix) {
        return new DatasetSource() {
            private int count = 0;
            private final DatasetSourceConfig config = new DatasetSourceConfig("MOCK", name);
            private final DatasetSourceStats stats = new DatasetSourceStats();

            @Override public String getSourceName() { return name; }
            @Override public DatasetSourceConfig getConfig() { return config; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() {}

            @Override
            public boolean hasNext() {
                return count * 200 < targetBytes;
            }

            @Override
            public NormalizedSample next() {
                count++;
                return NormalizedSample.createTextSample(textPrefix + " sample turn #" + count, name);
            }

            @Override public void close() {}
        };
    }
}
