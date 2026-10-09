package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;

/**
 * Thread-safe snapshot encapsulating UI selection, active process state, session context,
 * and retrieved persistent memories.
 */
public class ContextSnapshot {

    private final String snapshotId;
    private final long timestamp;

    // UI Context
    private final String activePartName;
    private final String selectedObjectName;
    private final String selectedObjectType;
    private final String resourcePath;
    private final String projectName;
    private final String selectionText;

    // Process Context
    private final String processId;
    private final String processName;
    private final String owningSessionId;
    private final String processState;
    private final String processPhase;
    private final double progressFraction;
    private final String lastStatus;
    private final String failureDetails;
    private final String correlatedResource;
    private final boolean correlatedWithSelection;

    // Retrieved Persistent Memory Context
    private final List<MemoryEntry> retrievedMemories;

    public ContextSnapshot(Builder builder) {
        this.snapshotId = builder.snapshotId != null ? builder.snapshotId : UUID.randomUUID().toString();
        this.timestamp = builder.timestamp > 0 ? builder.timestamp : System.currentTimeMillis();

        this.activePartName = builder.activePartName != null ? builder.activePartName : "";
        this.selectedObjectName = builder.selectedObjectName != null ? builder.selectedObjectName : "";
        this.selectedObjectType = builder.selectedObjectType != null ? builder.selectedObjectType : "";
        this.resourcePath = builder.resourcePath != null ? builder.resourcePath : "";
        this.projectName = builder.projectName != null ? builder.projectName : "";
        this.selectionText = builder.selectionText != null ? builder.selectionText : "";

        this.processId = builder.processId != null ? builder.processId : "";
        this.processName = builder.processName != null ? builder.processName : "";
        this.owningSessionId = builder.owningSessionId != null ? builder.owningSessionId : "";
        this.processState = builder.processState != null ? builder.processState : "IDLE";
        this.processPhase = builder.processPhase != null ? builder.processPhase : "NONE";
        this.progressFraction = builder.progressFraction;
        this.lastStatus = builder.lastStatus != null ? builder.lastStatus : "";
        this.failureDetails = builder.failureDetails != null ? builder.failureDetails : "";
        this.correlatedResource = builder.correlatedResource != null ? builder.correlatedResource : "";
        this.correlatedWithSelection = builder.correlatedWithSelection;

        this.retrievedMemories = builder.retrievedMemories != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.retrievedMemories))
                : Collections.emptyList();
    }

    public String getSnapshotId() { return snapshotId; }
    public long getTimestamp() { return timestamp; }

    public String getActivePartName() { return activePartName; }
    public String getSelectedObjectName() { return selectedObjectName; }
    public String getSelectedObjectType() { return selectedObjectType; }
    public String getResourcePath() { return resourcePath; }
    public String getProjectName() { return projectName; }
    public String getSelectionText() { return selectionText; }

    public String getProcessId() { return processId; }
    public String getProcessName() { return processName; }
    public String getOwningSessionId() { return owningSessionId; }
    public String getProcessState() { return processState; }
    public String getProcessPhase() { return processPhase; }
    public double getProgressFraction() { return progressFraction; }
    public String getLastStatus() { return lastStatus; }
    public String getFailureDetails() { return failureDetails; }
    public String getCorrelatedResource() { return correlatedResource; }
    public boolean isCorrelatedWithSelection() { return correlatedWithSelection; }

    public List<MemoryEntry> getRetrievedMemories() { return retrievedMemories; }

    public boolean hasSelection() {
        return !selectedObjectName.isEmpty() || !resourcePath.isEmpty() || !selectionText.isEmpty();
    }

    public boolean hasActiveProcess() {
        return !processId.isEmpty() && !"IDLE".equalsIgnoreCase(processState);
    }

    @Override
    public String toString() {
        return String.format("ContextSnapshot[id=%s, part=%s, selection=%s (%s), process=%s (%s, %s), memories=%d]",
                snapshotId, activePartName, selectedObjectName, selectedObjectType,
                processName, processState, processPhase, retrievedMemories.size());
    }

    public static class Builder {
        private String snapshotId;
        private long timestamp;

        private String activePartName;
        private String selectedObjectName;
        private String selectedObjectType;
        private String resourcePath;
        private String projectName;
        private String selectionText;

        private String processId;
        private String processName;
        private String owningSessionId;
        private String processState;
        private String processPhase;
        private double progressFraction;
        private String lastStatus;
        private String failureDetails;
        private String correlatedResource;
        private boolean correlatedWithSelection;

        private List<MemoryEntry> retrievedMemories;

        public Builder snapshotId(String snapshotId) { this.snapshotId = snapshotId; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }

        public Builder activePartName(String activePartName) { this.activePartName = activePartName; return this; }
        public Builder selectedObjectName(String selectedObjectName) { this.selectedObjectName = selectedObjectName; return this; }
        public Builder selectedObjectType(String selectedObjectType) { this.selectedObjectType = selectedObjectType; return this; }
        public Builder resourcePath(String resourcePath) { this.resourcePath = resourcePath; return this; }
        public Builder projectName(String projectName) { this.projectName = projectName; return this; }
        public Builder selectionText(String selectionText) { this.selectionText = selectionText; return this; }

        public Builder processId(String processId) { this.processId = processId; return this; }
        public Builder processName(String processName) { this.processName = processName; return this; }
        public Builder owningSessionId(String owningSessionId) { this.owningSessionId = owningSessionId; return this; }
        public Builder processState(String processState) { this.processState = processState; return this; }
        public Builder processPhase(String processPhase) { this.processPhase = processPhase; return this; }
        public Builder progressFraction(double progressFraction) { this.progressFraction = progressFraction; return this; }
        public Builder lastStatus(String lastStatus) { this.lastStatus = lastStatus; return this; }
        public Builder failureDetails(String failureDetails) { this.failureDetails = failureDetails; return this; }
        public Builder correlatedResource(String correlatedResource) { this.correlatedResource = correlatedResource; return this; }
        public Builder correlatedWithSelection(boolean correlatedWithSelection) { this.correlatedWithSelection = correlatedWithSelection; return this; }

        public Builder retrievedMemories(List<MemoryEntry> retrievedMemories) { this.retrievedMemories = retrievedMemories; return this; }

        public ContextSnapshot build() {
            return new ContextSnapshot(this);
        }
    }
}
