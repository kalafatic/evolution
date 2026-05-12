package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents reusable semantic knowledge within the orchestration kernel.
 */
public class WorkspaceArtifact {
    private final String artifactId;
    private final String artifactType;
    private final Set<String> semanticTags = new HashSet<>();
    private double confidence = 1.0;
    private String sourceIteration;
    private String lineageId;
    private final Set<String> relatedVariants = new HashSet<>();
    private String content;
    private long timestamp;
    private double decayScore = 1.0;
    private double relevanceScore = 0.0;

    public WorkspaceArtifact(String artifactId, String artifactType) {
        this.artifactId = artifactId;
        this.artifactType = artifactType;
        this.timestamp = System.currentTimeMillis();
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public Set<String> getSemanticTags() {
        return semanticTags;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getSourceIteration() {
        return sourceIteration;
    }

    public void setSourceIteration(String sourceIteration) {
        this.sourceIteration = sourceIteration;
    }

    public String getLineageId() {
        return lineageId;
    }

    public void setLineageId(String lineageId) {
        this.lineageId = lineageId;
    }

    public Set<String> getRelatedVariants() {
        return relatedVariants;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getDecayScore() {
        return decayScore;
    }

    public void setDecayScore(double decayScore) {
        this.decayScore = decayScore;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}
