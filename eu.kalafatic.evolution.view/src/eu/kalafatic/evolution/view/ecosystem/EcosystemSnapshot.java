package eu.kalafatic.evolution.view.ecosystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable point-in-time snapshot of the EVO ecosystem topology: all
 * nodes, all edges and the event feed entries, built from the authoritative
 * UPM state by {@link EcosystemTopologyBuilder}.
 */
public final class EcosystemSnapshot {

    public static final class EventEntry {
        private final long timestamp;
        private final String kind;
        private final String nodeId;
        private final String message;

        public EventEntry(long timestamp, String kind, String nodeId, String message) {
            this.timestamp = timestamp;
            this.kind = kind == null ? "info" : kind;
            this.nodeId = nodeId;
            this.message = message;
        }

        public long getTimestamp() { return timestamp; }
        public String getKind() { return kind; }
        public String getNodeId() { return nodeId; }
        public String getMessage() { return message; }
    }

    private final long timestamp;
    private final List<EcosystemNode> nodes;
    private final List<EcosystemEdge> edges;
    private final List<EventEntry> events;
    private final Map<String, Object> meta;

    public EcosystemSnapshot(long timestamp, List<EcosystemNode> nodes,
                             List<EcosystemEdge> edges, List<EventEntry> events,
                             Map<String, Object> meta) {
        this.timestamp = timestamp;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.meta = meta == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(meta));
    }

    public long getTimestamp() { return timestamp; }
    public List<EcosystemNode> getNodes() { return nodes; }
    public List<EcosystemEdge> getEdges() { return edges; }
    public List<EventEntry> getEvents() { return events; }
    public Map<String, Object> getMeta() { return meta; }

    public EcosystemNode node(String id) {
        for (EcosystemNode n : nodes) {
            if (n.getId().equals(id)) {
                return n;
            }
        }
        return null;
    }

    public int size() { return nodes.size(); }

    /**
     * Serializes this snapshot for the JavaScript graph. The payload is the
     * full model; the UI applies it incrementally (preserving viewport,
     * selection, collapse and manual positions) by diffing node IDs.
     */
    public String toJson() {
        org.json.JSONObject o = new org.json.JSONObject();
        o.put("ts", timestamp);
        org.json.JSONObject metaJson = new org.json.JSONObject();
        for (Map.Entry<String, Object> e : meta.entrySet()) {
            metaJson.put(e.getKey(), e.getValue());
        }
        o.put("meta", metaJson);
        org.json.JSONArray nodeArr = new org.json.JSONArray();
        for (EcosystemNode n : nodes) {
            org.json.JSONObject nj = new org.json.JSONObject();
            nj.put("id", n.getId());
            nj.put("parentId", n.getParentId() == null ? "" : n.getParentId());
            nj.put("productId", n.getProductId() == null ? "" : n.getProductId());
            nj.put("componentId", n.getComponentId());
            nj.put("name", n.getName());
            nj.put("type", n.getType().name());
            nj.put("status", n.getStatus().name());
            if (n.getStatusDetail() != null) nj.put("statusDetail", n.getStatusDetail());
            if (n.getVersion() != null) nj.put("version", n.getVersion());
            if (n.getEndpoint() != null) nj.put("endpoint", n.getEndpoint());
            nj.put("port", n.getPort());
            nj.put("collapsed", n.isCollapsed());
            org.json.JSONObject mj = new org.json.JSONObject();
            for (Map.Entry<String, Object> e : n.getMetrics().entrySet()) {
                mj.put(e.getKey(), e.getValue());
            }
            nj.put("metrics", mj);
            org.json.JSONObject dj = new org.json.JSONObject();
            for (Map.Entry<String, Object> e : n.getMetadata().entrySet()) {
                dj.put(e.getKey(), e.getValue());
            }
            nj.put("metadata", dj);
            nodeArr.put(nj);
        }
        o.put("nodes", nodeArr);
        org.json.JSONArray edgeArr = new org.json.JSONArray();
        for (EcosystemEdge e : edges) {
            org.json.JSONObject ej = new org.json.JSONObject();
            ej.put("id", e.getId());
            ej.put("source", e.getSourceId());
            ej.put("target", e.getTargetId());
            ej.put("type", e.getType().name());
            ej.put("status", e.getStatus().name());
            if (e.getLabel() != null) ej.put("label", e.getLabel());
            edgeArr.put(ej);
        }
        o.put("edges", edgeArr);
        org.json.JSONArray evArr = new org.json.JSONArray();
        for (EventEntry ev : events) {
            org.json.JSONObject evj = new org.json.JSONObject();
            evj.put("t", ev.getTimestamp());
            evj.put("kind", ev.getKind());
            evj.put("nodeId", ev.getNodeId() == null ? "" : ev.getNodeId());
            evj.put("message", ev.getMessage());
            evArr.put(evj);
        }
        o.put("events", evArr);
        return o.toString();
    }
}
