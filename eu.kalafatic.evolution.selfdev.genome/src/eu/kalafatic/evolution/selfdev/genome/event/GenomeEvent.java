package eu.kalafatic.evolution.selfdev.genome.event;

public class GenomeEvent {

    private String type;
    private String artifactId;
    private String topic;

    public GenomeEvent(String type, String artifactId, String topic) {
        this.type = type;
        this.artifactId = artifactId;
        this.topic = topic;
    }

    public String getType() {
        return type;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getTopic() {
        return topic;
    }
}
