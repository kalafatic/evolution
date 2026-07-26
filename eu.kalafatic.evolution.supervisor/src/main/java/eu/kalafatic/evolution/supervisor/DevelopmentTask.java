package eu.kalafatic.evolution.supervisor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DevelopmentTask {
    private String id;
    private String objective;
    private List<String> scope = new ArrayList<>();
    private List<String> acceptanceCriteria = new ArrayList<>();
    private TaskStatus status = TaskStatus.CREATED;
    private int attempts = 0;
    private String parentTaskId;
    private String failureReason;
    private Map<String, Object> metadata = new HashMap<>();

    public DevelopmentTask() {}

    public DevelopmentTask(String id, String objective) {
        this.id = id;
        this.objective = objective;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public List<String> getScope() { return scope; }
    public void setScope(List<String> scope) { this.scope = scope; }

    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
