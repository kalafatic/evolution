package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CognitiveTrace;
import eu.kalafatic.evolution.controller.orchestration.workspace.SemanticWorkspace;
import java.util.Set;
import java.util.UUID;

/**
 * Centralized container for orchestration state.
 * This class should be the single source of truth for the current orchestration lifecycle.
 */
public class OrchestrationState {
    private String rawInput;
    private IntentAnalysisResult intentAnalysis;
    private Set<TaskIntent> taskIntents;
    private String attachmentContext;
    private List<Task> executionPlan = new ArrayList<>();
    private List<String> diagnostics = new ArrayList<>();
    private Map<String, Object> metadata = new ConcurrentHashMap<>();

    // Evolution & Iteration state
    private String currentIterationId;
    private int iterationCount = 0;
    private boolean curiosityEnabled = false;
    private List<String> rejectionHistory = new ArrayList<>();
    private long bitState = 0;
    private String currentPhase;
    private SemanticWorkspace semanticWorkspace;
    private CognitiveTrace cognitiveTrace;
    private final String sessionId;

    public OrchestrationState() {
        this("GLOBAL");
    }

    public OrchestrationState(String sessionId) {
        this.sessionId = sessionId;
        this.semanticWorkspace = new SemanticWorkspace(sessionId);
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }

    public IntentAnalysisResult getIntentAnalysis() {
        return intentAnalysis;
    }

    public void setIntentAnalysis(IntentAnalysisResult intentAnalysis) {
        this.intentAnalysis = intentAnalysis;
    }

    public Set<TaskIntent> getTaskIntents() {
        return taskIntents;
    }

    public void setTaskIntents(Set<TaskIntent> taskIntents) {
        this.taskIntents = taskIntents;
    }

    public String getAttachmentContext() {
        return attachmentContext;
    }

    public void setAttachmentContext(String attachmentContext) {
        this.attachmentContext = attachmentContext;
    }

    public List<Task> getExecutionPlan() {
        return executionPlan;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    public void addDiagnostic(String message) {
        this.diagnostics.add("[" + System.currentTimeMillis() + "] " + message);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getCurrentIterationId() {
        return currentIterationId;
    }

    public void setCurrentIterationId(String currentIterationId) {
        this.currentIterationId = currentIterationId;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

    public boolean isCuriosityEnabled() {
        return curiosityEnabled;
    }

    public void setCuriosityEnabled(boolean curiosityEnabled) {
        this.curiosityEnabled = curiosityEnabled;
    }

    public List<String> getRejectionHistory() {
        return rejectionHistory;
    }

    public void addRejection(String reason) {
        this.rejectionHistory.add(reason);
    }

    public long getBitState() {
        return bitState;
    }

    public void setBitState(long bitState) {
        this.bitState = bitState;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public SemanticWorkspace getSemanticWorkspace() {
        return semanticWorkspace;
    }

    public CognitiveTrace getCognitiveTrace() {
        if (cognitiveTrace == null) {
            cognitiveTrace = new CognitiveTrace(UUID.randomUUID().toString(), currentIterationId, "main");
        }
        return cognitiveTrace;
    }

    public void setCognitiveTrace(CognitiveTrace cognitiveTrace) {
        this.cognitiveTrace = cognitiveTrace;
    }
}
