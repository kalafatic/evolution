package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A versioned, immutable semantic snapshot of a target.
 */
public class TargetSnapshot {
    public enum TargetType { PROJECT, SELF }

    private final String snapshotId;
    private final String version;
    private final long timestamp;
    private final TargetType targetType;
    private final String rootPath;

    private final Map<String, SemanticNode> nodes = new HashMap<>();
    private final List<SemanticEdge> edges = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>();

    public TargetSnapshot(String snapshotId, String version, TargetType targetType, String rootPath) {
        this.snapshotId = snapshotId;
        this.version = version;
        this.timestamp = System.currentTimeMillis();
        this.targetType = targetType;
        this.rootPath = rootPath;
    }

    public String getSnapshotId() { return snapshotId; }
    public String getVersion() { return version; }
    public long getTimestamp() { return timestamp; }
    public TargetType getTargetType() { return targetType; }
    public String getRootPath() { return rootPath; }

    public Map<String, SemanticNode> getNodes() { return nodes; }
    public List<SemanticEdge> getEdges() { return edges; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void addNode(SemanticNode node) {
        nodes.put(node.getId(), node);
    }

    public void addEdge(SemanticEdge edge) {
        edges.add(edge);
    }
}
