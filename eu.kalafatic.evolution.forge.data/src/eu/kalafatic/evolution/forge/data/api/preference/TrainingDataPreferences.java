package eu.kalafatic.evolution.forge.data.api.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable domain object capturing what training data the user wants (intent, size, domains, language, quality).
 */
public class TrainingDataPreferences {

    private final long minimumUsableBytes;
    private final long targetUsableBytes;
    private final String requiredLanguage;
    private final String capabilityObjective; // e.g. "chat", "coding", "reasoning"
    private final double minQualityThreshold;
    private final List<String> primaryDomains;
    private final List<String> preferredProviders;
    private final double validationSplitRatio;
    private final RequirementLevel sizeRequirementLevel;
    private final RequirementLevel languageRequirementLevel;

    public TrainingDataPreferences(
            long minimumUsableBytes,
            long targetUsableBytes,
            String requiredLanguage,
            String capabilityObjective,
            double minQualityThreshold,
            List<String> primaryDomains,
            List<String> preferredProviders,
            double validationSplitRatio,
            RequirementLevel sizeRequirementLevel,
            RequirementLevel languageRequirementLevel) {
        this.minimumUsableBytes = Math.max(0, minimumUsableBytes);
        this.targetUsableBytes = Math.max(this.minimumUsableBytes, targetUsableBytes);
        this.requiredLanguage = requiredLanguage != null ? requiredLanguage : "en";
        this.capabilityObjective = capabilityObjective != null ? capabilityObjective : "chat";
        this.minQualityThreshold = minQualityThreshold;
        this.primaryDomains = primaryDomains != null ? List.copyOf(primaryDomains) : List.of();
        this.preferredProviders = preferredProviders != null ? List.copyOf(preferredProviders) : List.of();
        this.validationSplitRatio = validationSplitRatio;
        this.sizeRequirementLevel = sizeRequirementLevel != null ? sizeRequirementLevel : RequirementLevel.HARD;
        this.languageRequirementLevel = languageRequirementLevel != null ? languageRequirementLevel : RequirementLevel.HARD;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getMinimumUsableBytes() { return minimumUsableBytes; }
    public long getTargetUsableBytes() { return targetUsableBytes; }
    public String getRequiredLanguage() { return requiredLanguage; }
    public String getCapabilityObjective() { return capabilityObjective; }
    public double getMinQualityThreshold() { return minQualityThreshold; }
    public List<String> getPrimaryDomains() { return primaryDomains; }
    public List<String> getPreferredProviders() { return preferredProviders; }
    public double getValidationSplitRatio() { return validationSplitRatio; }
    public RequirementLevel getSizeRequirementLevel() { return sizeRequirementLevel; }
    public RequirementLevel getLanguageRequirementLevel() { return languageRequirementLevel; }

    public static class Builder {
        private long minimumUsableBytes = 50_000_000L; // 50 MB default
        private long targetUsableBytes = 50_000_000L;
        private String requiredLanguage = "en";
        private String capabilityObjective = "chat";
        private double minQualityThreshold = 0.5;
        private final List<String> primaryDomains = new ArrayList<>();
        private final List<String> preferredProviders = new ArrayList<>();
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

        public Builder requiredLanguage(String language) {
            this.requiredLanguage = language;
            return this;
        }

        public Builder capabilityObjective(String objective) {
            this.capabilityObjective = objective;
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

        public Builder addProvider(String provider) {
            if (provider != null && !provider.trim().isEmpty()) {
                this.preferredProviders.add(provider.trim());
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
                    requiredLanguage,
                    capabilityObjective,
                    minQualityThreshold,
                    primaryDomains,
                    preferredProviders,
                    validationSplitRatio,
                    sizeRequirementLevel,
                    languageRequirementLevel
            );
        }
    }
}
