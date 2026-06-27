package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Records the outcome of a branch execution.
 */
public class ExecutionRecord {
    private boolean success;
    private List<String> createdFiles = new ArrayList<>();
    private List<String> modifiedFiles = new ArrayList<>();
    private List<String> deletedFiles = new ArrayList<>();
    private String logs;
    private long duration;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<String> getCreatedFiles() { return createdFiles; }
    public void setCreatedFiles(List<String> createdFiles) { this.createdFiles = createdFiles; }

    public List<String> getModifiedFiles() { return modifiedFiles; }
    public void setModifiedFiles(List<String> modifiedFiles) { this.modifiedFiles = modifiedFiles; }

    public List<String> getDeletedFiles() { return deletedFiles; }
    public void setDeletedFiles(List<String> deletedFiles) { this.deletedFiles = deletedFiles; }

    public String getLogs() { return logs; }
    public void setLogs(String logs) { this.logs = logs; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
}
