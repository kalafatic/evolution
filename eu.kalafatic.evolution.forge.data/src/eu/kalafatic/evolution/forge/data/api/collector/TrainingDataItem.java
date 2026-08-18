package eu.kalafatic.evolution.forge.data.api.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Common normalized training item produced by all collectors.
 */
public class TrainingDataItem {

    private SourceType sourceType;
    private String source;
    private String title;
    private String content;
    private Map<String, Object> metadata;
    private List<String> relatedSources;

    public TrainingDataItem() {
        this.metadata = new HashMap<>();
        this.relatedSources = new ArrayList<>();
    }

    public TrainingDataItem(SourceType sourceType, String source, String title, String content) {
        this();
        this.sourceType = sourceType;
        this.source = source;
        this.title = title;
        this.content = content;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public List<String> getRelatedSources() {
        return relatedSources;
    }

    public void setRelatedSources(List<String> relatedSources) {
        this.relatedSources = relatedSources != null ? relatedSources : new ArrayList<>();
    }

    public void addRelatedSource(String relatedSource) {
        if (this.relatedSources == null) {
            this.relatedSources = new ArrayList<>();
        }
        this.relatedSources.add(relatedSource);
    }

    @Override
    public String toString() {
        return "TrainingDataItem{" +
                "sourceType=" + sourceType +
                ", source='" + source + '\'' +
                ", title='" + title + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", metadata=" + metadata +
                '}';
    }
}
