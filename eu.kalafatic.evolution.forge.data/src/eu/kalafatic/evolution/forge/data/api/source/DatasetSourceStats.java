package eu.kalafatic.evolution.forge.data.api.source;

public class DatasetSourceStats {
    private long totalSamplesRead = 0;
    private long totalBytesRead = 0;
    private long estimatedTokens = 0;
    private long acceptedSamples = 0;
    private long rejectedSamples = 0;
    private long exactDuplicates = 0;
    private long nearDuplicates = 0;

    public DatasetSourceStats() {}

    public long getTotalSamplesRead() { return totalSamplesRead; }
    public void setTotalSamplesRead(long totalSamplesRead) { this.totalSamplesRead = totalSamplesRead; }
    public void incrementSamplesRead() { this.totalSamplesRead++; }

    public long getTotalBytesRead() { return totalBytesRead; }
    public void setTotalBytesRead(long totalBytesRead) { this.totalBytesRead = totalBytesRead; }
    public void addBytesRead(long bytes) { this.totalBytesRead += bytes; }

    public long getEstimatedTokens() { return estimatedTokens; }
    public void setEstimatedTokens(long estimatedTokens) { this.estimatedTokens = estimatedTokens; }
    public void addEstimatedTokens(long tokens) { this.estimatedTokens += tokens; }

    public long getAcceptedSamples() { return acceptedSamples; }
    public void setAcceptedSamples(long acceptedSamples) { this.acceptedSamples = acceptedSamples; }
    public void incrementAccepted() { this.acceptedSamples++; }

    public long getRejectedSamples() { return rejectedSamples; }
    public void setRejectedSamples(long rejectedSamples) { this.rejectedSamples = rejectedSamples; }
    public void incrementRejected() { this.rejectedSamples++; }

    public long getExactDuplicates() { return exactDuplicates; }
    public void setExactDuplicates(long exactDuplicates) { this.exactDuplicates = exactDuplicates; }
    public void incrementExactDuplicates() { this.exactDuplicates++; }

    public long getNearDuplicates() { return nearDuplicates; }
    public void setNearDuplicates(long nearDuplicates) { this.nearDuplicates = nearDuplicates; }
    public void incrementNearDuplicates() { this.nearDuplicates++; }
}
