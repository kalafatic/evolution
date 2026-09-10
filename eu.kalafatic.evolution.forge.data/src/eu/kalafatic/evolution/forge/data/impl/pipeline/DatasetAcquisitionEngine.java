package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Size-aware multi-source dataset composition engine.
 * Sequentially streams records from multiple sources (HF, Local, Evodata)
 * through normalization, quality filtering, and global deduplication until
 * target usable bytes/tokens are satisfied or all sources are exhausted.
 */
public class DatasetAcquisitionEngine {

    public static class AcquisitionResult {
        public enum Status {
            READY,
            INSUFFICIENT_SOURCE_DATA,
            CANCELLED,
            FAILED
        }

        private final List<NormalizedSample> acceptedSamples = new ArrayList<>();
        private final DatasetSourceStats globalStats = new DatasetSourceStats();
        private boolean targetReached = false;
        private boolean sourceExhausted = false;
        private double coveragePercent = 100.0;
        private long requestedMinimumUsableBytes = 0;
        private long usableContentBytes = 0;
        private long shortfallBytes = 0;
        private Status status = Status.READY;

        public List<NormalizedSample> getAcceptedSamples() { return acceptedSamples; }
        public DatasetSourceStats getGlobalStats() { return globalStats; }
        public boolean isTargetReached() { return targetReached; }
        public boolean isSourceExhausted() { return sourceExhausted; }
        public double getCoveragePercent() { return coveragePercent; }
        public long getRequestedMinimumUsableBytes() { return requestedMinimumUsableBytes; }
        public long getUsableContentBytes() { return usableContentBytes; }
        public long getShortfallBytes() { return shortfallBytes; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
    }

    private final DataCleaner cleaner;
    private final TrainingSampleQualityScorer scorer;
    private final DatasetDeduplicator globalDeduplicator;

    public DatasetAcquisitionEngine() {
        this(new DataCleaner(), new TrainingSampleQualityScorer(0.5), new DatasetDeduplicator(true));
    }

    public DatasetAcquisitionEngine(DataCleaner cleaner, TrainingSampleQualityScorer scorer, DatasetDeduplicator deduplicator) {
        this.cleaner = cleaner != null ? cleaner : new DataCleaner();
        this.scorer = scorer != null ? scorer : new TrainingSampleQualityScorer(0.5);
        this.globalDeduplicator = deduplicator != null ? deduplicator : new DatasetDeduplicator(true);
    }

    public AcquisitionResult acquireDataset(List<DatasetSource> sources, long targetUsableBytes, double valSplitRatio) throws Exception {
        AcquisitionResult result = new AcquisitionResult();
        DatasetSourceStats stats = result.globalStats;
        stats.setRequestedUsableBytes(targetUsableBytes);

        long accumulatedUsableBytes = 0;

        for (DatasetSource source : sources) {
            if (accumulatedUsableBytes >= targetUsableBytes) {
                break;
            }

            source.initialize();
            try (source) {
                while (accumulatedUsableBytes < targetUsableBytes && source.hasNext()) {
                    NormalizedSample s = source.next();
                    byte[] rawBytes = s.toFullText().getBytes(StandardCharsets.UTF_8);
                    long rawLen = rawBytes.length;

                    stats.addRawContentBytes(rawLen);
                    stats.addDownloadedBytes(rawLen);
                    stats.incrementSamplesRead();

                    NormalizedSample clean = cleaner.clean(s);
                    if (clean == null || !scorer.isAcceptable(clean)) {
                        stats.addRejectedBytes(rawLen);
                        stats.incrementRejected();
                        continue;
                    }

                    if (globalDeduplicator.isDuplicate(clean)) {
                        stats.addDuplicateBytes(rawLen);
                        stats.incrementExactDuplicates();
                        continue;
                    }

                    globalDeduplicator.register(clean);
                    byte[] cleanBytes = clean.toFullText().getBytes(StandardCharsets.UTF_8);
                    long cleanLen = cleanBytes.length;

                    result.acceptedSamples.add(clean);
                    accumulatedUsableBytes += cleanLen;

                    stats.addAcceptedBytes(cleanLen);
                    stats.incrementAccepted();
                    stats.addEstimatedTokens(clean.getTokenCount() > 0 ? clean.getTokenCount() : cleanLen / 4);

                    if (accumulatedUsableBytes >= targetUsableBytes) {
                        result.targetReached = true;
                        break;
                    }
                }
            }
        }

        result.requestedMinimumUsableBytes = targetUsableBytes;
        result.usableContentBytes = accumulatedUsableBytes;

        if (accumulatedUsableBytes < targetUsableBytes) {
            result.sourceExhausted = true;
            result.targetReached = false;
            result.status = AcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA;
            result.shortfallBytes = targetUsableBytes - accumulatedUsableBytes;
            result.coveragePercent = (accumulatedUsableBytes * 100.0) / Math.max(1, targetUsableBytes);
        } else {
            result.targetReached = true;
            result.status = AcquisitionResult.Status.READY;
            result.shortfallBytes = 0;
            result.coveragePercent = 100.0;
        }

        long valBytes = (long) (accumulatedUsableBytes * valSplitRatio);
        long trainBytes = accumulatedUsableBytes - valBytes;
        stats.setAcceptedBytes(accumulatedUsableBytes);
        stats.setTrainingBytes(trainBytes);
        stats.setValidationBytes(valBytes);

        return result;
    }
}
