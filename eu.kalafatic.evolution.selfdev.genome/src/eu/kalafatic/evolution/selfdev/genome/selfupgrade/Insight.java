package eu.kalafatic.evolution.selfdev.genome.selfupgrade;

public class Insight {
    private final String topic;
    private final String type;

    public Insight(String topic, String type) {
        this.topic = topic;
        this.type = type;
    }

    public String getTopic() {
        return topic;
    }

    public String getType() {
        return type;
    }
}
