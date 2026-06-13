package eu.kalafatic.evolution.selfdev.genome.core;

import java.util.ArrayList;
import java.util.List;

/**
 * MetricArtifact
 *
 * Derived evolutionary signals for long-term memory.
 */
public class MetricArtifact extends GenomeArtifact {
    private String metricName;
    private double value;
    private List<String> observations = new ArrayList<>();

    public MetricArtifact(String id, String metricName, double value) {
        this.id = id;
        this.metricName = metricName;
        this.value = value;
        this.timestamp = java.time.Instant.now();
        this.topic = "metrics";
    }

    @Override
    public ArtifactType getType() {
        return ArtifactType.DISCOVERY;
    }

    public String getMetricName() { return metricName; }
    public double getValue() { return value; }
    public List<String> getObservations() { return observations; }
}
