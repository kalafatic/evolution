package eu.kalafatic.evolution.forge.data.api.processor;

import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

/**
 * Component responsible for tracking raw, accepted, rejected, duplicate, training, and validation size accounting metrics.
 */
public interface DataSizeAccounting {

    void recordRaw(long bytes);

    void recordAccepted(long bytes, long tokens);

    void recordRejected(long bytes);

    void recordDuplicate(long bytes);

    void setTrainValidationRatio(double valSplitRatio);

    DatasetSourceStats getSnapshot();

    boolean isMinimumUsableSatisfied(long requestedUsableBytes);
}
