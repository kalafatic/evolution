package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GraphEntity {
    private String id;
    private String label;
    private String description;
    private String category;
    private EntityType type;
    private String status;
    private List<String> actions = new ArrayList<>();
    private JSONObject metadata = new JSONObject();
    private String runtimeState;

    public GraphEntity(String id, EntityType type) {
        this.id = id;
        this.label = id;
        this.type = type;
        this.status = "IDLE";
    }

    public GraphEntity(String id, String label, EntityType type) {
        this.id = id;
        this.label = (label != null && !label.trim().isEmpty()) ? label : id;
        this.type = type;
        this.status = "IDLE";
    }

    public String getId() { return id; }
    public String getLabel() { return label != null ? label : id; }
    public void setLabel(String label) { this.label = (label != null && !label.trim().isEmpty()) ? label : id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
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
        json.put("label", getLabel());
        if (description != null) {
            json.put("description", description);
        }
        if (category != null) {
            json.put("category", category);
        }
        json.put("type", type.toString());
        json.put("status", status);
        json.put("actions", new JSONArray(actions));
        json.put("metadata", metadata);
        json.put("runtimeState", runtimeState);
        return json;
    }
}
