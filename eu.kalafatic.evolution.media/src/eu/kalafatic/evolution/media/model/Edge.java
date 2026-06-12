package eu.kalafatic.evolution.media.model;

public class Edge {
    private String sourceId;
    private String targetId;
    private String label;
    private String type;

    public Edge() {}
    public Edge(String sourceId, String targetId, String label, String type) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.label = label;
        this.type = type;
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
