package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.util.HashMap;
import java.util.Map;

public class KnowledgeUnit {
    private final String relativePath;
    private final String fileType;
    private final String content;
    private final String hash;
    private final Map<String, Object> metadata = new HashMap<>();

    public KnowledgeUnit(String relativePath, String fileType, String content, String hash) {
        this.relativePath = relativePath;
        this.fileType = fileType;
        this.content = content;
        this.hash = hash;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getFileType() {
        return fileType;
    }

    public String getContent() {
        return content;
    }

    public String getHash() {
        return hash;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
