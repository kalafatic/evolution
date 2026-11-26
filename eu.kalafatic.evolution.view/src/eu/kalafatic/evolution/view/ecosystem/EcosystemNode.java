package eu.kalafatic.evolution.view.ecosystem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single node of the EVO ecosystem topology (Phase 2 model).
 *
 * <p>The model is a pure data carrier: it is built by
 * {@link EcosystemTopologyBuilder} from the authoritative UPM state and
 * rendered by the Ecosystem page. No business logic lives here.</p>
 *
 * <p>Node types follow the task specification: ROOT, PRODUCT, SERVICE,
 * PROVIDER, MODEL, INSTANCE, PROJECT, WORKSPACE, SESSION, TASK, RESOURCE,
 * HARDWARE.</p>
 */
public final class EcosystemNode {

    public enum NodeType {
        ROOT, PRODUCT, SERVICE, PROVIDER, MODEL, INSTANCE,
        PROJECT, WORKSPACE, SESSION, TASK, RESOURCE, HARDWARE
    }

    public enum RuntimeStatus {
        RUNNING, STARTING, STOPPED, FAILED, READY, OCCUPIED, UNKNOWN, DISABLED
    }

    private final String id;
    private final String parentId;
    private final String productId;
    private final String componentId;
    private final String name;
    private final NodeType type;
    private volatile RuntimeStatus status;
    private volatile String statusDetail;
    private volatile String version;
    private volatile String endpoint;
    private volatile int port;
    private final Map<String, Object> metrics = new LinkedHashMap<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private volatile boolean collapsed;
    // Visual layout state (optional; kept across refreshes by the UI bridge).
    private volatile Double x;
    private volatile Double y;

    public EcosystemNode(String id, String parentId, String productId, String componentId,
                         String name, NodeType type, RuntimeStatus status) {
        this.id = id;
        this.parentId = parentId;
        this.productId = productId;
        this.componentId = componentId == null ? id : componentId;
        this.name = name;
        this.type = type;
        this.status = status == null ? RuntimeStatus.UNKNOWN : status;
    }

    public String getId() { return id; }
    public String getParentId() { return parentId; }
    public String getProductId() { return productId; }
    public String getComponentId() { return componentId; }
    public String getName() { return name; }
    public NodeType getType() { return type; }
    public RuntimeStatus getStatus() { return status; }
    public String getStatusDetail() { return statusDetail; }
    public String getVersion() { return version; }
    public String getEndpoint() { return endpoint; }
    public int getPort() { return port; }
    public Map<String, Object> getMetrics() { return metrics; }
    public Map<String, Object> getMetadata() { return metadata; }
    public boolean isCollapsed() { return collapsed; }
    public Double getX() { return x; }
    public Double getY() { return y; }

    public void setStatus(RuntimeStatus status, String detail) {
        this.status = status == null ? RuntimeStatus.UNKNOWN : status;
        this.statusDetail = detail;
    }

    public void setStatusDetail(String detail) { this.statusDetail = detail; }

    public void setVersion(String version) { this.version = version; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setPort(int port) { this.port = port; }
    public void setCollapsed(boolean collapsed) { this.collapsed = collapsed; }
    public void setPosition(Double x, Double y) { this.x = x; this.y = y; }
    public void metric(String key, Object value) { metrics.put(key, value); }
    public void meta(String key, Object value) { metadata.put(key, value); }
}
