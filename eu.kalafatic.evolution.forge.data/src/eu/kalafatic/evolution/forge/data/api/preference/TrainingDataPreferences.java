package eu.kalafatic.evolution.forge.data.api.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable domain object capturing what training data the user wants (intent, size, domains, language, quality).
 */
public class TrainingDataPreferences {

    // Technical constraints
    private final long minimumUsableBytes;
    private final long targetUsableBytes;
    private final long maximumDownloadBytes;
    private final int maximumFiles;
    private final long maximumProcessingTimeMs;

    // Semantic requirements
    private final String requiredLanguage;
    private final List<String> allowedLanguages;
    private final String capabilityObjective; // e.g. "chat", "coding", "reasoning"
    private final String contentType; // e.g. "conversational", "code", "books", "documentation"
    private final String style; // e.g. "literary", "technical", "qa"
    private final String requiredLicense; // e.g. "public-domain", "mit", "apache-2.0"
    private final double minQualityThreshold;
    private final List<String> primaryDomains;
    private final List<String> topics;
    private final List<String> preferredProviders;
    private final List<String> excludedSourceTypes;
    private final double validationSplitRatio;
    private final RequirementLevel sizeRequirementLevel;
    private final RequirementLevel languageRequirementLevel;

    public TrainingDataPreferences(
            long minimumUsableBytes,
            long targetUsableBytes,
            long maximumDownloadBytes,
            int maximumFiles,
            long maximumProcessingTimeMs,
            String requiredLanguage,
            List<String> allowedLanguages,
            String capabilityObjective,
            String contentType,
            String style,
            String requiredLicense,
            double minQualityThreshold,
            List<String> primaryDomains,
            List<String> topics,
            List<String> preferredProviders,
            List<String> excludedSourceTypes,
            double validationSplitRatio,
            RequirementLevel sizeRequirementLevel,
            RequirementLevel languageRequirementLevel) {
        this.minimumUsableBytes = Math.max(0, minimumUsableBytes);
        this.targetUsableBytes = Math.max(this.minimumUsableBytes, targetUsableBytes);
        this.maximumDownloadBytes = Math.max(this.targetUsableBytes, maximumDownloadBytes);
        this.maximumFiles = Math.max(0, maximumFiles);
        this.maximumProcessingTimeMs = Math.max(0, maximumProcessingTimeMs);
        this.requiredLanguage = requiredLanguage != null ? requiredLanguage : "en";
        this.allowedLanguages = allowedLanguages != null && !allowedLanguages.isEmpty() ? List.copyOf(allowedLanguages) : List.of(this.requiredLanguage);
        this.capabilityObjective = capabilityObjective != null ? capabilityObjective : "chat";
        this.contentType = contentType != null ? contentType : "general";
        this.style = style != null ? style : "general";
        this.requiredLicense = requiredLicense != null ? requiredLicense : "any";
        this.minQualityThreshold = minQualityThreshold;
        this.primaryDomains = primaryDomains != null ? List.copyOf(primaryDomains) : List.of();
        this.topics = topics != null ? List.copyOf(topics) : List.of();
        this.preferredProviders = preferredProviders != null ? List.copyOf(preferredProviders) : List.of();
        this.excludedSourceTypes = excludedSourceTypes != null ? List.copyOf(excludedSourceTypes) : List.of();
        this.validationSplitRatio = validationSplitRatio;
        this.sizeRequirementLevel = sizeRequirementLevel != null ? sizeRequirementLevel : RequirementLevel.HARD;
        this.languageRequirementLevel = languageRequirementLevel != null ? languageRequirementLevel : RequirementLevel.HARD;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getMinimumUsableBytes() { return minimumUsableBytes; }
    public long getTargetUsableBytes() { return targetUsableBytes; }
    public long getMaximumDownloadBytes() { return maximumDownloadBytes; }
    public int getMaximumFiles() { return maximumFiles; }
    public long getMaximumProcessingTimeMs() { return maximumProcessingTimeMs; }
    public String getRequiredLanguage() { return requiredLanguage; }
    public List<String> getAllowedLanguages() { return allowedLanguages; }
    public String getCapabilityObjective() { return capabilityObjective; }
    public String getContentType() { return contentType; }
    public String getStyle() { return style; }
    public String getRequiredLicense() { return requiredLicense; }
    public double getMinQualityThreshold() { return minQualityThreshold; }
    public List<String> getPrimaryDomains() { return primaryDomains; }
    public List<String> getTopics() { return topics; }
    public List<String> getPreferredProviders() { return preferredProviders; }
    public List<String> getExcludedSourceTypes() { return excludedSourceTypes; }
    public double getValidationSplitRatio() { return validationSplitRatio; }
    public RequirementLevel getSizeRequirementLevel() { return sizeRequirementLevel; }
    public RequirementLevel getLanguageRequirementLevel() { return languageRequirementLevel; }

    public static class Builder {
        private long minimumUsableBytes = 50_000_000L; // 50 MB default
        private long targetUsableBytes = 50_000_000L;
        private long maximumDownloadBytes = 500_000_000L;
        private int maximumFiles = 10_000;
        private long maximumProcessingTimeMs = 3_600_000L; // 1 hour
        private String requiredLanguage = "en";
        private final List<String> allowedLanguages = new ArrayList<>();
        private String capabilityObjective = "chat";
        private String contentType = "general";
        private String style = "general";
        private String requiredLicense = "any";
        private double minQualityThreshold = 0.5;
        private final List<String> primaryDomains = new ArrayList<>();
        private final List<String> topics = new ArrayList<>();
        private final List<String> preferredProviders = new ArrayList<>();
        private final List<String> excludedSourceTypes = new ArrayList<>();
        private double validationSplitRatio = 0.02;
        private RequirementLevel sizeRequirementLevel = RequirementLevel.HARD;
        private RequirementLevel languageRequirementLevel = RequirementLevel.HARD;

        public Builder minimumUsableBytes(long bytes) {
            this.minimumUsableBytes = bytes;
            if (this.targetUsableBytes < bytes) this.targetUsableBytes = bytes;
            return this;
        }

        public Builder targetUsableBytes(long bytes) {
            this.targetUsableBytes = bytes;
            return this;
        }

        public Builder maximumDownloadBytes(long bytes) {
            this.maximumDownloadBytes = bytes;
            return this;
        }

        public Builder maximumFiles(int files) {
            this.maximumFiles = files;
            return this;
        }

        public Builder maximumProcessingTimeMs(long ms) {
            this.maximumProcessingTimeMs = ms;
            return this;
        }

        public Builder requiredLanguage(String language) {
            this.requiredLanguage = language;
            return this;
        }

        public Builder addAllowedLanguage(String language) {
            if (language != null && !language.trim().isEmpty()) {
                this.allowedLanguages.add(language.trim());
            }
            return this;
        }

        public Builder capabilityObjective(String objective) {
            this.capabilityObjective = objective;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public Builder requiredLicense(String license) {
            this.requiredLicense = license;
            return this;
        }

        public Builder minQualityThreshold(double threshold) {
            this.minQualityThreshold = threshold;
            return this;
        }

        public Builder addDomain(String domain) {
            if (domain != null && !domain.trim().isEmpty()) {
                this.primaryDomains.add(domain.trim());
            }
            return this;
        }

        public Builder addTopic(String topic) {
            if (topic != null && !topic.trim().isEmpty()) {
                this.topics.add(topic.trim());
            }
            return this;
        }

        public Builder addProvider(String provider) {
            if (provider != null && !provider.trim().isEmpty()) {
                this.preferredProviders.add(provider.trim());
            }
            return this;
        }

        public Builder addExcludedSourceType(String sourceType) {
            if (sourceType != null && !sourceType.trim().isEmpty()) {
                this.excludedSourceTypes.add(sourceType.trim());
            }
            return this;
        }

        public Builder validationSplitRatio(double ratio) {
            this.validationSplitRatio = ratio;
            return this;
        }

        public Builder sizeRequirementLevel(RequirementLevel level) {
            this.sizeRequirementLevel = level;
            return this;
        }

        public Builder languageRequirementLevel(RequirementLevel level) {
            this.languageRequirementLevel = level;
            return this;
        }

        public TrainingDataPreferences build() {
            return new TrainingDataPreferences(
                    minimumUsableBytes,
                    targetUsableBytes,
                    maximumDownloadBytes,
                    maximumFiles,
                    maximumProcessingTimeMs,
                    requiredLanguage,
                    allowedLanguages,
                    capabilityObjective,
                    contentType,
                    style,
                    requiredLicense,
                    minQualityThreshold,
                    primaryDomains,
                    topics,
                    preferredProviders,
                    excludedSourceTypes,
                    validationSplitRatio,
                    sizeRequirementLevel,
                    languageRequirementLevel
            );
        }
    }
}
