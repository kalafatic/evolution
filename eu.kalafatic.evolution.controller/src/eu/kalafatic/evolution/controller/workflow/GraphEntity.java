package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GraphEntity {
    private String id;
    private EntityType type;
    private String status;
    private List<String> actions = new ArrayList<>();
    private JSONObject metadata = new JSONObject();
    private String runtimeState;

    public GraphEntity(String id, EntityType type) {
        this.id = id;
        this.type = type;
        this.status = "IDLE";
    }

    public String getId() { return id; }
    public EntityType getType() { return type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getActions() { return actions; }
    public JSONObject getMetadata() { return metadata; }
    public String getRuntimeState() { return runtimeState; }
    public void setRuntimeState(String runtimeState) { this.runtimeState = runtimeState; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", type.toString());
        json.put("status", status);
        json.put("actions", new JSONArray(actions));
        json.put("metadata", metadata);
        json.put("runtimeState", runtimeState);
        return json;
    }
}
