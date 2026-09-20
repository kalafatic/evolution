package eu.kalafatic.evolution.forge.data.api.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates dataset search parameters extracted from the current Forge configuration profile.
 */
public class DatasetSearchRequest {

    private final String datasetName;
    private final String task;
    private final String domain;
    private final String language;
    private final String format;
    private final List<String> requiredFields;
    private final String preferredSplit;
    private final long minimumSizeBytes;
    private final long targetSizeBytes;
    private final List<String> keywords;
    private final List<String> tags;
    private final int limit;

    public DatasetSearchRequest(Builder builder) {
        this.datasetName = builder.datasetName != null ? builder.datasetName : "";
        this.task = builder.task != null ? builder.task : "GENERAL_TEXT";
        this.domain = builder.domain != null ? builder.domain : "text";
        this.language = builder.language != null ? builder.language : "en";
        this.format = builder.format != null ? builder.format : "INSTRUCTION";
        this.requiredFields = builder.requiredFields != null ? List.copyOf(builder.requiredFields) : Collections.emptyList();
        this.preferredSplit = builder.preferredSplit != null ? builder.preferredSplit : "train";
        this.minimumSizeBytes = builder.minimumSizeBytes;
        this.targetSizeBytes = builder.targetSizeBytes;
        this.keywords = builder.keywords != null ? List.copyOf(builder.keywords) : Collections.emptyList();
        this.tags = builder.tags != null ? List.copyOf(builder.tags) : Collections.emptyList();
        this.limit = builder.limit > 0 ? builder.limit : 20;
    }

    public String getDatasetName() { return datasetName; }
    public String getTask() { return task; }
    public String getDomain() { return domain; }
    public String getLanguage() { return language; }
    public String getFormat() { return format; }
    public List<String> getRequiredFields() { return requiredFields; }
    public String getPreferredSplit() { return preferredSplit; }
    public long getMinimumSizeBytes() { return minimumSizeBytes; }
    public long getTargetSizeBytes() { return targetSizeBytes; }
    public List<String> getKeywords() { return keywords; }
    public List<String> getTags() { return tags; }
    public int getLimit() { return limit; }

    public static class Builder {
        private String datasetName = "";
        private String task = "GENERAL_TEXT";
        private String domain = "text";
        private String language = "en";
        private String format = "INSTRUCTION";
        private List<String> requiredFields = new ArrayList<>();
        private String preferredSplit = "train";
        private long minimumSizeBytes = 0;
        private long targetSizeBytes = 50 * 1024 * 1024L; // Default 50MB
        private List<String> keywords = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        private int limit = 20;

        public Builder datasetName(String datasetName) { this.datasetName = datasetName; return this; }
        public Builder task(String task) { this.task = task; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder format(String format) { this.format = format; return this; }
        public Builder requiredFields(List<String> requiredFields) { if (requiredFields != null) this.requiredFields = new ArrayList<>(requiredFields); return this; }
        public Builder preferredSplit(String preferredSplit) { this.preferredSplit = preferredSplit; return this; }
        public Builder minimumSizeBytes(long minimumSizeBytes) { this.minimumSizeBytes = minimumSizeBytes; return this; }
        public Builder targetSizeBytes(long targetSizeBytes) { this.targetSizeBytes = targetSizeBytes; return this; }
        public Builder keywords(List<String> keywords) { if (keywords != null) this.keywords = new ArrayList<>(keywords); return this; }
        public Builder tags(List<String> tags) { if (tags != null) this.tags = new ArrayList<>(tags); return this; }
        public Builder limit(int limit) { this.limit = limit; return this; }

        public DatasetSearchRequest build() {
            return new DatasetSearchRequest(this);
        }
    }
}
