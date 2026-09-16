package eu.kalafatic.evolution.forge.data.api.service;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object encapsulating the outcome of dataset preparation for a Forge run.
 */
public class DatasetPreparationResult {

    public enum Status {
        SUCCESS,
        PARTIAL,
        INSUFFICIENT_DATA,
        CANCELLED,
        FAILED
    }

    private Status status = Status.SUCCESS;
    private EvoDatasetArtifact artifact;
    private DatasetSourceStats stats = new DatasetSourceStats();
    private List<NormalizedSample> samples = new ArrayList<>();
    private int sourcesProcessed = 0;
    private List<String> successfulSources = new ArrayList<>();
    private List<String> failedSources = new ArrayList<>();
    private List<String> skippedSources = new ArrayList<>();
    private long recordsRead = 0;
    private long recordsAccepted = 0;
    private long recordsRejected = 0;
    private long duplicateCount = 0;
    private long acceptedBytes = 0;
    private long rejectedBytes = 0;
    private long duplicateBytes = 0;
    private long targetBytes = 0;
    private long estimatedTokens = 0;
    private int trainSamplesCount = 0;
    private int valSamplesCount = 0;
    private int conversationsCount = 0;
    private int messagesCount = 0;
    private String outputPath = "";
    private String diagnosticLog = "";
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public DatasetPreparationResult() {}

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public EvoDatasetArtifact getArtifact() { return artifact; }
    public void setArtifact(EvoDatasetArtifact artifact) { this.artifact = artifact; }

    public DatasetSourceStats getStats() { return stats; }
    public void setStats(DatasetSourceStats stats) { this.stats = stats; }

    public List<NormalizedSample> getSamples() { return samples; }
    public void setSamples(List<NormalizedSample> samples) { this.samples = samples; }

    public int getSourcesProcessed() { return sourcesProcessed; }
    public void setSourcesProcessed(int sourcesProcessed) { this.sourcesProcessed = sourcesProcessed; }

    public List<String> getSuccessfulSources() { return successfulSources; }
    public void setSuccessfulSources(List<String> successfulSources) { this.successfulSources = successfulSources; }

    public List<String> getFailedSources() { return failedSources; }
    public void setFailedSources(List<String> failedSources) { this.failedSources = failedSources; }

    public List<String> getSkippedSources() { return skippedSources; }
    public void setSkippedSources(List<String> skippedSources) { this.skippedSources = skippedSources; }

    public long getRecordsRead() { return recordsRead; }
    public void setRecordsRead(long recordsRead) { this.recordsRead = recordsRead; }

    public long getRecordsAccepted() { return recordsAccepted; }
    public void setRecordsAccepted(long recordsAccepted) { this.recordsAccepted = recordsAccepted; }

    public long getRecordsRejected() { return recordsRejected; }
    public void setRecordsRejected(long recordsRejected) { this.recordsRejected = recordsRejected; }

    public long getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(long duplicateCount) { this.duplicateCount = duplicateCount; }

    public long getAcceptedBytes() { return acceptedBytes; }
    public void setAcceptedBytes(long acceptedBytes) { this.acceptedBytes = acceptedBytes; }

    public long getUsableBytes() { return acceptedBytes; }

    public long getRejectedBytes() { return rejectedBytes; }
    public void setRejectedBytes(long rejectedBytes) { this.rejectedBytes = rejectedBytes; }

    public long getDuplicateBytes() { return duplicateBytes; }
    public void setDuplicateBytes(long duplicateBytes) { this.duplicateBytes = duplicateBytes; }

    public long getTargetBytes() { return targetBytes; }
    public void setTargetBytes(long targetBytes) { this.targetBytes = targetBytes; }

    public long getEstimatedTokens() { return estimatedTokens; }
    public void setEstimatedTokens(long estimatedTokens) { this.estimatedTokens = estimatedTokens; }

    public int getTrainSamplesCount() { return trainSamplesCount; }
    public void setTrainSamplesCount(int trainSamplesCount) { this.trainSamplesCount = trainSamplesCount; }

    public int getValSamplesCount() { return valSamplesCount; }
    public void setValSamplesCount(int valSamplesCount) { this.valSamplesCount = valSamplesCount; }

    public int getConversationsCount() { return conversationsCount; }
    public void setConversationsCount(int conversationsCount) { this.conversationsCount = conversationsCount; }

    public int getMessagesCount() { return messagesCount; }
    public void setMessagesCount(int messagesCount) { this.messagesCount = messagesCount; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public String getDiagnosticLog() { return diagnosticLog; }
    public void setDiagnosticLog(String diagnosticLog) { this.diagnosticLog = diagnosticLog; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
