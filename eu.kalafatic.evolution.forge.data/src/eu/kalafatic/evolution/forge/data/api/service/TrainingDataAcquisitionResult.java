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

    // Detailed metrics
    private final long downloadedBytes;
    private final long extractedBytes;
    private final long rawRecordBytes;
    private final long acceptedContentBytes;
    private final long rejectedBytes;
    private final long duplicateBytes;
    private final long trainingBytes;
    private final long validationBytes;
    private final long estimatedTokens;
    private final List<String> sourcesUsed;
    private final List<String> sourcesExhausted;
    private final List<String> hardRequirementsFailed;
    private final List<String> warnings;
    private final List<String> errors;
    private final String provenance;

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
        this(acceptedSamples, globalStats, targetReached, sourceExhausted, coveragePercent, requestedMinimumUsableBytes, usableContentBytes, shortfallBytes, status, failureReason, preferenceEvaluation,
                globalStats != null ? globalStats.getDownloadedBytes() : 0,
                globalStats != null ? globalStats.getExtractedBytes() : 0,
                globalStats != null ? globalStats.getRawContentBytes() : 0,
                usableContentBytes,
                globalStats != null ? globalStats.getRejectedBytes() : 0,
                globalStats != null ? globalStats.getDuplicateBytes() : 0,
                globalStats != null ? globalStats.getTrainingBytes() : 0,
                globalStats != null ? globalStats.getValidationBytes() : 0,
                globalStats != null ? globalStats.getEstimatedTokens() : 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), "EVO_PIPELINE");
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
            TrainingDataPreferenceEvaluation preferenceEvaluation,
            long downloadedBytes,
            long extractedBytes,
            long rawRecordBytes,
            long acceptedContentBytes,
            long rejectedBytes,
            long duplicateBytes,
            long trainingBytes,
            long validationBytes,
            long estimatedTokens,
            List<String> sourcesUsed,
            List<String> sourcesExhausted,
            List<String> hardRequirementsFailed,
            List<String> warnings,
            List<String> errors,
            String provenance) {
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
        this.downloadedBytes = downloadedBytes;
        this.extractedBytes = extractedBytes;
        this.rawRecordBytes = rawRecordBytes;
        this.acceptedContentBytes = acceptedContentBytes;
        this.rejectedBytes = rejectedBytes;
        this.duplicateBytes = duplicateBytes;
        this.trainingBytes = trainingBytes;
        this.validationBytes = validationBytes;
        this.estimatedTokens = estimatedTokens;
        this.sourcesUsed = sourcesUsed != null ? List.copyOf(sourcesUsed) : List.of();
        this.sourcesExhausted = sourcesExhausted != null ? List.copyOf(sourcesExhausted) : List.of();
        this.hardRequirementsFailed = hardRequirementsFailed != null ? List.copyOf(hardRequirementsFailed) : List.of();
        this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
        this.errors = errors != null ? List.copyOf(errors) : List.of();
        this.provenance = provenance != null ? provenance : "EVO_PIPELINE";
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

    public long getDownloadedBytes() { return downloadedBytes; }
    public long getExtractedBytes() { return extractedBytes; }
    public long getRawRecordBytes() { return rawRecordBytes; }
    public long getAcceptedContentBytes() { return acceptedContentBytes; }
    public long getRejectedBytes() { return rejectedBytes; }
    public long getDuplicateBytes() { return duplicateBytes; }
    public long getTrainingBytes() { return trainingBytes; }
    public long getValidationBytes() { return validationBytes; }
    public long getEstimatedTokens() { return estimatedTokens; }
    public List<String> getSourcesUsed() { return sourcesUsed; }
    public List<String> getSourcesExhausted() { return sourcesExhausted; }
    public List<String> getHardRequirementsFailed() { return hardRequirementsFailed; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getErrors() { return errors; }
    public String getProvenance() { return provenance; }
}
