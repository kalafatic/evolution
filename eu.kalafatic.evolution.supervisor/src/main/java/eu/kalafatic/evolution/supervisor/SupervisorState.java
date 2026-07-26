package eu.kalafatic.evolution.supervisor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SupervisorState {
    private static final ObjectMapper mapper = new ObjectMapper();

    private DevelopmentTask currentTask;
    private List<DevelopmentTask> taskQueue = new ArrayList<>();
    private List<DevelopmentTask> taskHistory = new ArrayList<>();
    private int maxTaskAttempts = 3;
    private int maxRepairAttempts = 3;
    private int maxConsecutiveFailures = 3;
    private int consecutiveFailures = 0;

    public SupervisorState() {}

    public DevelopmentTask getCurrentTask() { return currentTask; }
    public void setCurrentTask(DevelopmentTask currentTask) { this.currentTask = currentTask; }

    public List<DevelopmentTask> getTaskQueue() { return taskQueue; }
    public void setTaskQueue(List<DevelopmentTask> taskQueue) { this.taskQueue = taskQueue; }

    public List<DevelopmentTask> getTaskHistory() { return taskHistory; }
    public void setTaskHistory(List<DevelopmentTask> taskHistory) { this.taskHistory = taskHistory; }

    public int getMaxTaskAttempts() { return maxTaskAttempts; }
    public void setMaxTaskAttempts(int maxTaskAttempts) { this.maxTaskAttempts = maxTaskAttempts; }

    public int getMaxRepairAttempts() { return maxRepairAttempts; }
    public void setMaxRepairAttempts(int maxRepairAttempts) { this.maxRepairAttempts = maxRepairAttempts; }

    public int getMaxConsecutiveFailures() { return maxConsecutiveFailures; }
    public void setMaxConsecutiveFailures(int maxConsecutiveFailures) { this.maxConsecutiveFailures = maxConsecutiveFailures; }

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    public static SupervisorState load(File file) throws IOException {
        if (!file.exists()) {
            return new SupervisorState();
        }
        return mapper.readValue(file, SupervisorState.class);
    }

    public void save(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, this);
    }
}
