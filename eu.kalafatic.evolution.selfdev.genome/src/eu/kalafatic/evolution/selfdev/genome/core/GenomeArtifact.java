package eu.kalafatic.evolution.selfdev.genome.core;

import java.time.Instant;
import java.util.List;

public abstract class GenomeArtifact {

    protected String id;
    protected String sourceProject;
    protected Instant timestamp;
    protected String topic;
    protected double fitness;
    protected List<String> tags;

    public abstract ArtifactType getType();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceProject() {
        return sourceProject;
    }

    public void setSourceProject(String sourceProject) {
        this.sourceProject = sourceProject;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
