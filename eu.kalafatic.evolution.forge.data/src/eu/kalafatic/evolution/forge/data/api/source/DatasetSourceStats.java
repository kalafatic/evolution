package eu.kalafatic.evolution.forge.data.api.source;

public class DatasetSourceStats {
    private long requestedUsableBytes = 0;
    private long downloadedBytes = 0;
    private long extractedBytes = 0;
    private long rawContentBytes = 0;

    private long acceptedBytes = 0;
    private long rejectedBytes = 0;
    private long duplicateBytes = 0;

    private long trainingBytes = 0;
    private long validationBytes = 0;

    private long totalSamplesRead = 0;
    private long totalBytesRead = 0; // Raw stream bytes read
    private long acceptedSamples = 0;
    private long rejectedSamples = 0;
    private long exactDuplicates = 0;
    private long nearDuplicates = 0;

    private long estimatedTokens = 0;
    private long trainingTokens = 0;
    private long validationTokens = 0;

    private double estimatedEnglishRatio = 1.0;

    public DatasetSourceStats() {}

    public long getRequestedUsableBytes() { return requestedUsableBytes; }
    public void setRequestedUsableBytes(long requestedUsableBytes) { this.requestedUsableBytes = requestedUsableBytes; }

    public long getDownloadedBytes() { return downloadedBytes; }
    public void setDownloadedBytes(long downloadedBytes) { this.downloadedBytes = downloadedBytes; }
    public void addDownloadedBytes(long bytes) { this.downloadedBytes += bytes; }

    public long getExtractedBytes() { return extractedBytes; }
    public void setExtractedBytes(long extractedBytes) { this.extractedBytes = extractedBytes; }
    public void addExtractedBytes(long bytes) { this.extractedBytes += bytes; }

    public long getRawContentBytes() { return rawContentBytes; }
    public void setRawContentBytes(long rawContentBytes) { this.rawContentBytes = rawContentBytes; }
    public void addRawContentBytes(long bytes) { this.rawContentBytes += bytes; }

    public long getAcceptedBytes() { return acceptedBytes; }
    public void setAcceptedBytes(long acceptedBytes) { this.acceptedBytes = acceptedBytes; }
    public void addAcceptedBytes(long bytes) { this.acceptedBytes += bytes; }

    public long getRejectedBytes() { return rejectedBytes; }
    public void setRejectedBytes(long rejectedBytes) { this.rejectedBytes = rejectedBytes; }
    public void addRejectedBytes(long bytes) { this.rejectedBytes += bytes; }

    public long getDuplicateBytes() { return duplicateBytes; }
    public void setDuplicateBytes(long duplicateBytes) { this.duplicateBytes = duplicateBytes; }
    public void addDuplicateBytes(long bytes) { this.duplicateBytes += bytes; }

    public long getTrainingBytes() { return trainingBytes; }
    public void setTrainingBytes(long trainingBytes) { this.trainingBytes = trainingBytes; }

    public long getValidationBytes() { return validationBytes; }
    public void setValidationBytes(long validationBytes) { this.validationBytes = validationBytes; }

    public long getTotalSamplesRead() { return totalSamplesRead; }
    public void setTotalSamplesRead(long totalSamplesRead) { this.totalSamplesRead = totalSamplesRead; }
    public void incrementSamplesRead() { this.totalSamplesRead++; }

    public long getTotalBytesRead() { return totalBytesRead; }
    public void setTotalBytesRead(long totalBytesRead) { this.totalBytesRead = totalBytesRead; }
    public void addBytesRead(long bytes) { this.totalBytesRead += bytes; }

    public long getAcceptedSamples() { return acceptedSamples; }
    public void setAcceptedSamples(long acceptedSamples) { this.acceptedSamples = acceptedSamples; }
    public void incrementAccepted() { this.acceptedSamples++; }
    public long getAcceptedRecords() { return acceptedSamples; }

    public long getRejectedSamples() { return rejectedSamples; }
    public void setRejectedSamples(long rejectedSamples) { this.rejectedSamples = rejectedSamples; }
    public void incrementRejected() { this.rejectedSamples++; }
    public long getRejectedRecords() { return rejectedSamples; }

    public long getExactDuplicates() { return exactDuplicates; }
    public void setExactDuplicates(long exactDuplicates) { this.exactDuplicates = exactDuplicates; }
    public void incrementExactDuplicates() { this.exactDuplicates++; }

    public long getNearDuplicates() { return nearDuplicates; }
    public void setNearDuplicates(long nearDuplicates) { this.nearDuplicates = nearDuplicates; }
    public void incrementNearDuplicates() { this.nearDuplicates++; }

    public long getDuplicateRecords() { return exactDuplicates + nearDuplicates; }

    public long getEstimatedTokens() { return estimatedTokens; }
    public void setEstimatedTokens(long estimatedTokens) { this.estimatedTokens = estimatedTokens; }
    public void addEstimatedTokens(long tokens) { this.estimatedTokens += tokens; }

    public long getTrainingTokens() { return trainingTokens; }
    public void setTrainingTokens(long trainingTokens) { this.trainingTokens = trainingTokens; }

    public long getValidationTokens() { return validationTokens; }
    public void setValidationTokens(long validationTokens) { this.validationTokens = validationTokens; }

    public double getAcceptanceRatio() {
        long total = rawContentBytes > 0 ? rawContentBytes : totalBytesRead;
        return total > 0 ? (double) acceptedBytes / total : 0.0;
    }

    public double getDuplicateRatio() {
        long total = rawContentBytes > 0 ? rawContentBytes : totalBytesRead;
        return total > 0 ? (double) duplicateBytes / total : 0.0;
    }

    public double getEstimatedEnglishRatio() { return estimatedEnglishRatio; }
    public void setEstimatedEnglishRatio(double ratio) { this.estimatedEnglishRatio = ratio; }
}
