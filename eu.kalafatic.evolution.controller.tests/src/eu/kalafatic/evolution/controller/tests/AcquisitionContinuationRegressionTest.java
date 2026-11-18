package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DataCleaner;
import eu.kalafatic.evolution.forge.data.impl.pipeline.DatasetDeduplicator;
import eu.kalafatic.evolution.forge.data.impl.pipeline.TrainingSampleQualityScorer;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Regression tests for acquisition continuation, amplification, recordization,
 * and cancellation (WP2).
 */
public class AcquisitionContinuationRegressionTest {

    private static DatasetSource finiteSource(String name, int recordCount, int targetPerRecordBytes) {
        return new DatasetSource() {
            private int idx = 0;
            private final DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", name);
            private final DatasetSourceStats stats = new DatasetSourceStats();

            @Override public String getSourceName() { return name; }
            @Override public DatasetSourceConfig getConfig() { return config; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() {}

            @Override
            public boolean hasNext() { return idx < recordCount; }

            @Override
            public NormalizedSample next() {
                idx++;
                StringBuilder sb = new StringBuilder();
                sb.append("Acquisition continuation regression record number ").append(idx).append(" with content. ");
                while (sb.length() < targetPerRecordBytes) {
                    sb.append("additional padding content to reach the requested record size. ");
                }
                return NormalizedSample.createTextSample(sb.toString(), name);
            }

            @Override public void close() {}
        };
    }

    private static TrainingDataAcquisitionResult acquire(List<DatasetSource> sources, long target, TrainingDataAcquisitionRequest request) throws Exception {
        if (request == null) {
            request = new TrainingDataAcquisitionRequest(sources, target, 0.02);
        }
        return new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(), new TrainingSampleQualityScorer(0.5), new DatasetDeduplicator(false)
        ).acquireDataset(request);
    }

    @Test
    public void amplificationCompletesTargetWhenCoverageIsSufficient() throws Exception {
        // ~3,400 usable bytes from the source, 10,000 requested (~34% coverage) —
        // above the 30% engagement gate, so marked amplification must complete the target.
        DatasetSource source = finiteSource("explicit/repo", 20, 170);
        TrainingDataAcquisitionResult result = acquire(List.of(source), 10000, null);

        assertTrue("target must be reached via amplification", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.READY, result.getStatus());
        assertTrue("accepted bytes must cover the target",
                result.getGlobalStats().getAcceptedBytes() >= 10000);

        boolean foundAmplified = false;
        boolean foundOriginal = false;
        for (NormalizedSample s : result.getAcceptedSamples()) {
            String src = s.getSource() != null ? s.getSource() : "";
            if (src.endsWith("#amplified")) foundAmplified = true;
            else foundOriginal = true;
        }
        assertTrue("amplification copies must be explicitly marked", foundAmplified);
        assertTrue("original source samples must be present", foundOriginal);
    }

    @Test
    public void amplificationIsSkippedBelowEngagementGate() throws Exception {
        // ~100 usable bytes vs 100,000 requested (~0.1% coverage) — genuinely
        // insufficient sources must still report INSUFFICIENT_SOURCE_DATA.
        DatasetSource source = finiteSource("tiny/repo", 1, 100);
        TrainingDataAcquisitionResult result = acquire(List.of(source), 100000, null);

        assertFalse("target must not be reached", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA, result.getStatus());
        assertEquals("no amplification copies below the gate", 1, result.getAcceptedSamples().size());
    }

    @Test
    public void amplificationCanBeDisabledExplicitly() throws Exception {
        DatasetSource source = finiteSource("explicit/repo", 20, 170);
        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest(List.of(source), 10000, 0.02)
                .setAllowAmplification(false);
        TrainingDataAcquisitionResult result = acquire(null, 10000, request);

        assertFalse("amplification must be disabled on request", result.isTargetReached());
        assertEquals(TrainingDataAcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA, result.getStatus());
        assertEquals(20, result.getAcceptedSamples().size());
    }

    @Test
    public void cancellationStopsAcquisitionWithCancelledStatus() throws Exception {
        // An effectively unbounded source cancelled after a few records: the service
        // must stop promptly, keep the partial data, and report CANCELLED — never READY.
        DatasetSource infinite = new DatasetSource() {
            private int idx = 0;
            private final DatasetSourceConfig config = new DatasetSourceConfig("LOCAL", "infinite/repo");
            private final DatasetSourceStats stats = new DatasetSourceStats();

            @Override public String getSourceName() { return "infinite/repo"; }
            @Override public DatasetSourceConfig getConfig() { return config; }
            @Override public DatasetSourceStats getStats() { return stats; }
            @Override public void initialize() {}
            @Override public boolean hasNext() { return true; }

            @Override
            public NormalizedSample next() {
                idx++;
                return NormalizedSample.createTextSample("Cancellation test record content number " + idx, "infinite/repo");
            }

            @Override public void close() {}
        };

        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest(List.of(infinite), 1_000_000_000L, 0.02);
        // Cancel as soon as the first record has been produced.
        Thread canceller = new Thread(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {}
            request.cancel();
        });
        canceller.start();

        long start = System.currentTimeMillis();
        TrainingDataAcquisitionResult result = new TrainingDataAcquisitionServiceImpl(
                new DataCleaner(), new TrainingSampleQualityScorer(0.5), new DatasetDeduplicator(false)
        ).acquireDataset(request);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals("cancelled acquisition must report CANCELLED",
                TrainingDataAcquisitionResult.Status.CANCELLED, result.getStatus());
        assertFalse("cancelled acquisition must not report target reached", result.isTargetReached());
        assertTrue("cancellation must be prompt", elapsed < 30000);
        canceller.join(5000);
    }

    @Test
    public void shortLinesBecomeRejectableRecordsWhileContentMerges() throws Exception {
        // Hybrid recordization: tiny lines must surface as rejected records while
        // the remaining content still forms an accepted sample.
        File dir = java.nio.file.Files.createTempDirectory("recordization").toFile();
        java.nio.file.Files.writeString(new java.io.File(dir, "noisy.txt").toPath(),
                "<html><div>HTML garbage line that gets stripped</div></html>\n" +
                "a\n" +
                "User: Substantial valid conversational turn number one.\n" +
                "Assistant: Detailed response explaining the conversation topic clearly.\n",
                StandardCharsets.UTF_8);

        eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource source =
                new eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource(
                        new DatasetSourceConfig("LOCAL", dir.getAbsolutePath()));
        // Unlimited target so the entire source (incl. junk records) is consumed.
        TrainingDataAcquisitionResult result = acquire(List.of(source), 0, null);

        assertTrue("valid content must be accepted", result.getGlobalStats().getAcceptedBytes() > 0);
        assertTrue("short junk lines must be rejected and accounted",
                result.getGlobalStats().getRejectedBytes() > 0);
        boolean validContentFound = false;
        for (NormalizedSample s : result.getAcceptedSamples()) {
            if (s.toFullText().contains("Substantial valid conversational turn number one")) {
                validContentFound = true;
                break;
            }
        }
        assertTrue("accepted samples must contain the valid conversation content", validContentFound);
    }
}
