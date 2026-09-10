package eu.kalafatic.evolution.forge.data.api.discovery;

import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;

import java.util.List;

/**
 * Value object representing a discovered training dataset candidate with relevance scoring and domain metadata.
 */
public class DataSourceCandidate {

    private final String datasetId;
    private final String provider; // e.g. "HUGGING_FACE", "LOCAL", "EVO_CODEBASE"
    private final DatasetSource source;
    private final String language;
    private final List<String> domainTags;
    private final long estimatedUsableBytes;
    private final double relevanceScore;

    public DataSourceCandidate(
            String datasetId,
            String provider,
            DatasetSource source,
            String language,
            List<String> domainTags,
            long estimatedUsableBytes,
            double relevanceScore) {
        this.datasetId = datasetId;
        this.provider = provider;
        this.source = source;
        this.language = language != null ? language : "en";
        this.domainTags = domainTags != null ? List.copyOf(domainTags) : List.of();
        this.estimatedUsableBytes = estimatedUsableBytes;
        this.relevanceScore = relevanceScore;
    }

    public String getDatasetId() { return datasetId; }
    public String getProvider() { return provider; }
    public DatasetSource getSource() { return source; }
    public String getLanguage() { return language; }
    public List<String> getDomainTags() { return domainTags; }
    public long getEstimatedUsableBytes() { return estimatedUsableBytes; }
    public double getRelevanceScore() { return relevanceScore; }
}
