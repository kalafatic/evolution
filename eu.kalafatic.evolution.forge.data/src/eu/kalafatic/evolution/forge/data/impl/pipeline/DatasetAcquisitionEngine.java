package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Size-aware multi-source dataset composition engine.
 * Delegates dataset acquisition to TrainingDataAcquisitionService while preserving
 * legacy API compatibility for DatasetAcquisitionEngine callers.
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
        private DatasetSourceStats globalStats = new DatasetSourceStats();
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

    private final TrainingDataAcquisitionService acquisitionService;

    public DatasetAcquisitionEngine() {
        this(new DataCleaner(), new TrainingSampleQualityScorer(0.5), new DatasetDeduplicator(true));
    }

    public DatasetAcquisitionEngine(DataCleaner cleaner, TrainingSampleQualityScorer scorer, DatasetDeduplicator deduplicator) {
        this.acquisitionService = new TrainingDataAcquisitionServiceImpl(cleaner, scorer, deduplicator);
    }

    public AcquisitionResult acquireDataset(List<DatasetSource> sources, long targetUsableBytes, double valSplitRatio) throws Exception {
        TrainingDataAcquisitionRequest request = new TrainingDataAcquisitionRequest(sources, targetUsableBytes, valSplitRatio);
        TrainingDataAcquisitionResult serviceResult = acquisitionService.acquireDataset(request);

        AcquisitionResult legacyResult = new AcquisitionResult();
        legacyResult.acceptedSamples.addAll(serviceResult.getAcceptedSamples());
        legacyResult.globalStats = serviceResult.getGlobalStats();
        legacyResult.targetReached = serviceResult.isTargetReached();
        legacyResult.sourceExhausted = serviceResult.isSourceExhausted();
        legacyResult.coveragePercent = serviceResult.getCoveragePercent();
        legacyResult.requestedMinimumUsableBytes = serviceResult.getRequestedMinimumUsableBytes();
        legacyResult.usableContentBytes = serviceResult.getUsableContentBytes();
        legacyResult.shortfallBytes = serviceResult.getShortfallBytes();

        switch (serviceResult.getStatus()) {
            case READY -> legacyResult.setStatus(AcquisitionResult.Status.READY);
            case INSUFFICIENT_SOURCE_DATA -> legacyResult.setStatus(AcquisitionResult.Status.INSUFFICIENT_SOURCE_DATA);
            case CANCELLED -> legacyResult.setStatus(AcquisitionResult.Status.CANCELLED);
            case FAILED -> legacyResult.setStatus(AcquisitionResult.Status.FAILED);
        }

        return legacyResult;
    }
}
