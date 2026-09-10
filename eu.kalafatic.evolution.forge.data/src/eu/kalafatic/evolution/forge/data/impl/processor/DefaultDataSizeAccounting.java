package eu.kalafatic.evolution.forge.data.impl.processor;

import eu.kalafatic.evolution.forge.data.api.processor.DataSizeAccounting;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

/**
 * Standard implementation of DataSizeAccounting tracking dataset metric totals.
 */
public class DefaultDataSizeAccounting implements DataSizeAccounting {

    private final DatasetSourceStats stats;

    public DefaultDataSizeAccounting() {
        this(new DatasetSourceStats());
    }

    public DefaultDataSizeAccounting(DatasetSourceStats stats) {
        this.stats = stats != null ? stats : new DatasetSourceStats();
    }

    @Override
    public void recordRaw(long bytes) {
        stats.addRawContentBytes(bytes);
        stats.addDownloadedBytes(bytes);
        stats.incrementSamplesRead();
    }

    @Override
    public void recordAccepted(long bytes, long tokens) {
        stats.addAcceptedBytes(bytes);
        stats.incrementAccepted();
        stats.addEstimatedTokens(tokens);
    }

    @Override
    public void recordRejected(long bytes) {
        stats.addRejectedBytes(bytes);
        stats.incrementRejected();
    }

    @Override
    public void recordDuplicate(long bytes) {
        stats.addDuplicateBytes(bytes);
        stats.incrementExactDuplicates();
    }

    @Override
    public void setTrainValidationRatio(double valSplitRatio) {
        long accepted = stats.getAcceptedBytes();
        long valBytes = (long) (accepted * valSplitRatio);
        long trainBytes = accepted - valBytes;
        stats.setTrainingBytes(trainBytes);
        stats.setValidationBytes(valBytes);
    }

    @Override
    public DatasetSourceStats getSnapshot() {
        return stats;
    }

    @Override
    public boolean isMinimumUsableSatisfied(long requestedUsableBytes) {
        return requestedUsableBytes <= 0 || stats.getAcceptedBytes() >= requestedUsableBytes;
    }
}
