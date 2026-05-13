package eu.kalafatic.evolution.controller.orchestration.mediated.model;

/**
 * Represents a relationship between two SemanticNodes.
 */
public class SemanticEdge {
    public enum EdgeType {
        DEPENDS_ON,
        REFERENCES,
        IMPLEMENTS,
        EXTENDS,
        CONTAINS,
        CALLS
    }

    private final String sourceId;
    private final String targetId;
    private final EdgeType type;
    private final double weight;

    public SemanticEdge(String sourceId, String targetId, EdgeType type) {
        this(sourceId, targetId, type, 1.0);
    }

    public SemanticEdge(String sourceId, String targetId, EdgeType type, double weight) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.weight = weight;
    }

    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public EdgeType getType() { return type; }
    public double getWeight() { return weight; }
}
