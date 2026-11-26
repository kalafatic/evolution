package eu.kalafatic.evolution.view.ecosystem;

/**
 * A relationship between two ecosystem nodes (Phase 2 model).
 */
public final class EcosystemEdge {

    public enum EdgeType {
        /** Parent-child containment (the primary hierarchy). */
        HIERARCHICAL,
        /** Product requires another component to function. */
        DEPENDENCY,
        /** Network endpoint / API a component talks to. */
        COMMUNICATION,
        /** Live runtime connection (socket/HTTP session). */
        RUNTIME_CONNECTION,
        /** Resource consumption (e.g. model → GPU/VRAM). */
        RESOURCE_ALLOCATION,
        /** Data flowing between components (e.g. inference traffic). */
        DATA_FLOW
    }

    public enum EdgeStatus {
        ACTIVE, IDLE, DOWN, UNKNOWN
    }

    private final String id;
    private final String sourceId;
    private final String targetId;
    private final EdgeType type;
    private volatile EdgeStatus status;
    private volatile String label;

    public EcosystemEdge(String id, String sourceId, String targetId, EdgeType type) {
        this(id, sourceId, targetId, type, EdgeStatus.UNKNOWN, null);
    }

    public EcosystemEdge(String id, String sourceId, String targetId, EdgeType type,
                         EdgeStatus status, String label) {
        this.id = id;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.status = status == null ? EdgeStatus.UNKNOWN : status;
        this.label = label;
    }

    public String getId() { return id; }
    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public EdgeType getType() { return type; }
    public EdgeStatus getStatus() { return status; }
    public String getLabel() { return label; }

    public void setStatus(EdgeStatus status) { this.status = status; }
    public void setLabel(String label) { this.label = label; }
}
