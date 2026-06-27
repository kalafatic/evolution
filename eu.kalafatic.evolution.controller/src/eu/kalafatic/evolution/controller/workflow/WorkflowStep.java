package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

public class WorkflowStep {
    private String id;
    private String entityId;
    private String stepType;
    private WorkflowStatus status;
    private String description;
    private JSONObject runtimeMetadata = new JSONObject();
    private boolean requiresUserAction;
    private List<String> logs = new ArrayList<>();
    private List<String> relatedFiles = new ArrayList<>();
    private String relatedPrompt;
    private JSONObject outputs = new JSONObject();

    public WorkflowStep(String id, String entityId, String stepType) {
        this.id = id;
        this.entityId = entityId;
        this.stepType = stepType;
        this.status = WorkflowStatus.PENDING;
    }

    public String getId() { return id; }
    public String getEntityId() { return entityId; }
    public String getStepType() { return stepType; }
    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public JSONObject getRuntimeMetadata() { return runtimeMetadata; }
    public boolean isRequiresUserAction() { return requiresUserAction; }
    public void setRequiresUserAction(boolean requiresUserAction) { this.requiresUserAction = requiresUserAction; }
    public List<String> getLogs() { return logs; }
    public List<String> getRelatedFiles() { return relatedFiles; }
    public String getRelatedPrompt() { return relatedPrompt; }
    public void setRelatedPrompt(String relatedPrompt) { this.relatedPrompt = relatedPrompt; }
    public JSONObject getOutputs() { return outputs; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("entityId", entityId);
        json.put("stepType", stepType);
        json.put("status", status.toString());
        json.put("description", description);
        json.put("runtimeMetadata", runtimeMetadata);
        json.put("requiresUserAction", requiresUserAction);
        json.put("logs", new JSONArray(logs));
        json.put("relatedFiles", new JSONArray(relatedFiles));
        json.put("relatedPrompt", relatedPrompt);
        json.put("outputs", outputs);
        return json;
    }
}
