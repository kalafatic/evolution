package eu.kalafatic.evolution.forge.data.api.discovery;

import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;

import java.util.List;

/**
 * Value object representing a discovered training dataset candidate with relevance scoring and domain metadata.
 */
public class DataSourceCandidate {

    private final String datasetId;
    private final String provider; // e.g. "HUGGING_FACE", "LOCAL", "EVO_CODEBASE", "WEB", "PUBLIC_DOMAIN"
    private final String title;
    private final String description;
    private final String uri;
    private final String sourceType; // e.g. "dataset", "repository", "book", "webpage", "archive"
    private final DatasetSource source;
    private final String language;
    private final List<String> languages;
    private final List<String> domainTags;
    private final List<String> topics;
    private final String contentType;
    private final String format;
    private final long estimatedTotalBytes;
    private final long estimatedUsableBytes;
    private final double qualityIndicator;
    private final double relevanceScore;
    private final String license;
    private final String provenance;
    private final double sourceReliability;
    private final double duplicationRisk;
    private final long recommendedAllocationBytes;

    public DataSourceCandidate(
            String datasetId,
            String provider,
            DatasetSource source,
            String language,
            List<String> domainTags,
            long estimatedUsableBytes,
            double relevanceScore) {
        this(datasetId, provider, datasetId, "", "", "dataset", source, language, List.of(language != null ? language : "en"), domainTags, List.of(), "general", "text", estimatedUsableBytes, estimatedUsableBytes, 0.8, relevanceScore, "unknown", provider, 0.9, 0.1, estimatedUsableBytes);
    }

    public DataSourceCandidate(
            String datasetId,
            String provider,
            String title,
            String description,
            String uri,
            String sourceType,
            DatasetSource source,
            String language,
            List<String> languages,
            List<String> domainTags,
            List<String> topics,
            String contentType,
            String format,
            long estimatedTotalBytes,
            long estimatedUsableBytes,
            double qualityIndicator,
            double relevanceScore,
            String license,
            String provenance,
            double sourceReliability,
            double duplicationRisk,
            long recommendedAllocationBytes) {
        this.datasetId = datasetId;
        this.provider = provider != null ? provider : "UNKNOWN";
        this.title = title != null ? title : datasetId;
        this.description = description != null ? description : "";
        this.uri = uri != null ? uri : "";
        this.sourceType = sourceType != null ? sourceType : "dataset";
        this.source = source;
        this.language = language != null ? language : "en";
        this.languages = languages != null && !languages.isEmpty() ? List.copyOf(languages) : List.of(this.language);
        this.domainTags = domainTags != null ? List.copyOf(domainTags) : List.of();
        this.topics = topics != null ? List.copyOf(topics) : List.of();
        this.contentType = contentType != null ? contentType : "general";
        this.format = format != null ? format : "text";
        this.estimatedTotalBytes = estimatedTotalBytes;
        this.estimatedUsableBytes = estimatedUsableBytes;
        this.qualityIndicator = qualityIndicator;
        this.relevanceScore = relevanceScore;
        this.license = license != null ? license : "unknown";
        this.provenance = provenance != null ? provenance : this.provider;
        this.sourceReliability = sourceReliability;
        this.duplicationRisk = duplicationRisk;
        this.recommendedAllocationBytes = recommendedAllocationBytes;
    }

    public String getDatasetId() { return datasetId; }
    public String getProvider() { return provider; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUri() { return uri; }
    public String getSourceType() { return sourceType; }
    public DatasetSource getSource() { return source; }
    public String getLanguage() { return language; }
    public List<String> getLanguages() { return languages; }
    public List<String> getDomainTags() { return domainTags; }
    public List<String> getTopics() { return topics; }
    public String getContentType() { return contentType; }
    public String getFormat() { return format; }
    public long getEstimatedTotalBytes() { return estimatedTotalBytes; }
    public long getEstimatedUsableBytes() { return estimatedUsableBytes; }
    public double getQualityIndicator() { return qualityIndicator; }
    public double getRelevanceScore() { return relevanceScore; }
    public String getLicense() { return license; }
    public String getProvenance() { return provenance; }
    public double getSourceReliability() { return sourceReliability; }
    public double getDuplicationRisk() { return duplicationRisk; }
    public long getRecommendedAllocationBytes() { return recommendedAllocationBytes; }
}
