package eu.kalafatic.evolution.forge.data.api.service;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.evaluation.TrainingDataPreferenceEvaluation;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import java.util.List;

/**
 * Immutable domain result object for training data acquisition.
 */
public class TrainingDataAcquisitionResult {

    public enum Status {
        READY,
        INSUFFICIENT_SOURCE_DATA,
        CANCELLED,
        FAILED
    }

    private final List<NormalizedSample> acceptedSamples;
    private final DatasetSourceStats globalStats;
    private final boolean targetReached;
    private final boolean sourceExhausted;
    private final double coveragePercent;
    private final long requestedMinimumUsableBytes;
    private final long usableContentBytes;
    private final long shortfallBytes;
    private final Status status;
    private final String failureReason;
    private final TrainingDataPreferenceEvaluation preferenceEvaluation;

    public TrainingDataAcquisitionResult(
            List<NormalizedSample> acceptedSamples,
            DatasetSourceStats globalStats,
            boolean targetReached,
            boolean sourceExhausted,
            double coveragePercent,
            long requestedMinimumUsableBytes,
            long usableContentBytes,
            long shortfallBytes,
            Status status,
            String failureReason) {
        this(acceptedSamples, globalStats, targetReached, sourceExhausted, coveragePercent, requestedMinimumUsableBytes, usableContentBytes, shortfallBytes, status, failureReason, null);
    }

    public TrainingDataAcquisitionResult(
            List<NormalizedSample> acceptedSamples,
            DatasetSourceStats globalStats,
            boolean targetReached,
            boolean sourceExhausted,
            double coveragePercent,
            long requestedMinimumUsableBytes,
            long usableContentBytes,
            long shortfallBytes,
            Status status,
            String failureReason,
            TrainingDataPreferenceEvaluation preferenceEvaluation) {
        this.acceptedSamples = acceptedSamples != null ? List.copyOf(acceptedSamples) : List.of();
        this.globalStats = globalStats != null ? globalStats : new DatasetSourceStats();
        this.targetReached = targetReached;
        this.sourceExhausted = sourceExhausted;
        this.coveragePercent = coveragePercent;
        this.requestedMinimumUsableBytes = requestedMinimumUsableBytes;
        this.usableContentBytes = usableContentBytes;
        this.shortfallBytes = shortfallBytes;
        this.status = status;
        this.failureReason = failureReason;
        this.preferenceEvaluation = preferenceEvaluation;
    }

    public List<NormalizedSample> getAcceptedSamples() {
        return acceptedSamples;
    }

    public DatasetSourceStats getGlobalStats() {
        return globalStats;
    }

    public boolean isTargetReached() {
        return targetReached;
    }

    public boolean isSourceExhausted() {
        return sourceExhausted;
    }

    public double getCoveragePercent() {
        return coveragePercent;
    }

    public long getRequestedMinimumUsableBytes() {
        return requestedMinimumUsableBytes;
    }

    public long getUsableContentBytes() {
        return usableContentBytes;
    }

    public long getShortfallBytes() {
        return shortfallBytes;
    }

    public Status getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public TrainingDataPreferenceEvaluation getPreferenceEvaluation() {
        return preferenceEvaluation;
    }
}
