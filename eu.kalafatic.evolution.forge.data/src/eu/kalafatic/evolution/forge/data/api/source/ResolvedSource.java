package eu.kalafatic.evolution.forge.data.api.source;

/**
 * Deterministic preflight resolution metadata representing a concrete acquisition candidate.
 */
public class ResolvedSource {
    private final String provider;
    private final String repository;
    private final String requestedSplit;
    private final String resolvedSplit;
    private final String configuration;
    private final String revision;
    private final String downloadLocation;
    private final String contentType;
    private final long estimatedSize;
    private final boolean available;
    private final boolean accessible;
    private final String failureReason;

    public ResolvedSource(String provider, String repository, String requestedSplit, String resolvedSplit, String configuration, String revision, String downloadLocation, String contentType, long estimatedSize, boolean available, boolean accessible, String failureReason) {
        this.provider = provider != null ? provider : "UNKNOWN";
        this.repository = repository != null ? repository : "";
        this.requestedSplit = requestedSplit != null ? requestedSplit : "train";
        this.resolvedSplit = resolvedSplit != null ? resolvedSplit : requestedSplit;
        this.configuration = configuration != null ? configuration : "default";
        this.revision = revision != null ? revision : "main";
        this.downloadLocation = downloadLocation != null ? downloadLocation : "";
        this.contentType = contentType != null ? contentType : "application/json";
        this.estimatedSize = estimatedSize;
        this.available = available;
        this.accessible = accessible;
        this.failureReason = failureReason;
    }

    public String getProvider() { return provider; }
    public String getRepository() { return repository; }
    public String getRequestedSplit() { return requestedSplit; }
    public String getResolvedSplit() { return resolvedSplit; }
    public String getConfiguration() { return configuration; }
    public String getRevision() { return revision; }
    public String getDownloadLocation() { return downloadLocation; }
    public String getContentType() { return contentType; }
    public long getEstimatedSize() { return estimatedSize; }
    public boolean isAvailable() { return available; }
    public boolean isAccessible() { return accessible; }
    public String getFailureReason() { return failureReason; }

    public void logPreflight() {
        System.out.printf("[ACQ-PREFLIGHT]\n  provider=%s\n  repository=%s\n  requestedSplit=%s\n  resolvedSplit=%s\n  configuration=%s\n  revision=%s\n  accessible=%b\n  estimatedSize=%d\n  downloadMethod=%s\n  failureReason=%s\n",
                provider, repository, requestedSplit, resolvedSplit, configuration, revision, accessible, estimatedSize, downloadLocation, failureReason != null ? failureReason : "none");
    }
}
