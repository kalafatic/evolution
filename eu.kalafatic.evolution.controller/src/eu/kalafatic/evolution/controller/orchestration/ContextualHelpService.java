package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryQuery;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker.ProcessInfo;

/**
 * Core engine for context resolution, process correlation, persistent memory retrieval,
 * and 3-level contextual assistance generation.
 */
public class ContextualHelpService {

    private static final ContextualHelpService INSTANCE = new ContextualHelpService();

    public static ContextualHelpService getInstance() {
        return INSTANCE;
    }

    public static class ContextualHelpResult {
        private final ContextSnapshot snapshot;
        private final String selectionHelp;
        private final String processAwareness;
        private final String cognitiveAssistance;
        private final List<String> recommendedActions;
        private final long durationMs;

        public ContextualHelpResult(ContextSnapshot snapshot, String selectionHelp, String processAwareness,
                                    String cognitiveAssistance, List<String> recommendedActions, long durationMs) {
            this.snapshot = snapshot;
            this.selectionHelp = selectionHelp != null ? selectionHelp : "";
            this.processAwareness = processAwareness != null ? processAwareness : "";
            this.cognitiveAssistance = cognitiveAssistance != null ? cognitiveAssistance : "";
            this.recommendedActions = recommendedActions != null ? recommendedActions : new ArrayList<>();
            this.durationMs = durationMs;
        }

        public ContextSnapshot getSnapshot() { return snapshot; }
        public String getSelectionHelp() { return selectionHelp; }
        public String getProcessAwareness() { return processAwareness; }
        public String getCognitiveAssistance() { return cognitiveAssistance; }
        public List<String> getRecommendedActions() { return recommendedActions; }
        public long getDurationMs() { return durationMs; }
    }

    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ContextualHelpService-Debouncer");
        t.setDaemon(true);
        return t;
    });

    private final AtomicLong requestCounter = new AtomicLong(0);
    private volatile Future<?> pendingTask = null;

    public ContextualHelpService() {
    }

    /**
     * Resolves a snapshot synchronously given raw UI selection parameters and session ID.
     */
    public ContextSnapshot resolveSnapshot(String activePartName, String selectedObjectName,
                                           String selectedObjectType, String resourcePath,
                                           String projectName, String selectionText,
                                           String sessionId) {
        long startMs = System.currentTimeMillis();
        String sid = (sessionId != null && !sessionId.isEmpty()) ? sessionId : "GLOBAL";

        // 1. Process Context Lookup
        ProcessInfo processInfo = ContextProcessTracker.getInstance().getProcessState(sid);
        String procId = processInfo != null ? processInfo.getProcessId() : "";
        String procName = processInfo != null ? processInfo.getProcessName() : "";
        String procState = processInfo != null ? processInfo.getState() : "IDLE";
        String procPhase = processInfo != null ? processInfo.getPhase() : "NONE";
        double progress = processInfo != null ? processInfo.getProgressFraction() : 0.0;
        String status = processInfo != null ? processInfo.getLastStatus() : "";
        String failure = processInfo != null ? processInfo.getFailureDetails() : "";
        String corrRes = processInfo != null ? processInfo.getCorrelatedResource() : "";

        // 2. Correlation Resolution
        boolean correlated = isCorrelated(selectedObjectName, resourcePath, corrRes, procName, status);

        // 3. Persistent Memory Retrieval (Scoped query derivation)
        String queryText = buildQueryText(selectedObjectName, resourcePath, procName, procPhase);
        MemoryQuery query = new MemoryQuery()
                .setSearchText(queryText)
                .setLimit(5)
                .setMinImportance(MemoryImportance.LOW);

        List<MemoryEntry> memories = MemoryService.getInstance().retrieveRelevant(query);

        ContextSnapshot snapshot = new ContextSnapshot.Builder()
                .activePartName(activePartName)
                .selectedObjectName(selectedObjectName)
                .selectedObjectType(selectedObjectType)
                .resourcePath(resourcePath)
                .projectName(projectName)
                .selectionText(selectionText)
                .processId(procId)
                .processName(procName)
                .owningSessionId(sid)
                .processState(procState)
                .processPhase(procPhase)
                .progressFraction(progress)
                .lastStatus(status)
                .failureDetails(failure)
                .correlatedResource(corrRes)
                .correlatedWithSelection(correlated)
                .retrievedMemories(memories)
                .build();

        long duration = System.currentTimeMillis() - startMs;
        System.out.println("[CONTEXT_HELP] Snapshot captured in " + duration + "ms: " + snapshot);
        return snapshot;
    }

    /**
     * Evaluates a ContextSnapshot and generates 3 levels of contextual assistance.
     */
    public ContextualHelpResult generateAssistance(ContextSnapshot snapshot) {
        long startMs = System.currentTimeMillis();
        if (snapshot == null) {
            return new ContextualHelpResult(null, "No selection available.", "No process active.", "No assistance available.", new ArrayList<>(), 0);
        }

        // Level A: Selection Help
        String selectionHelp = buildSelectionHelp(snapshot);

        // Level B: Process Awareness
        String processAwareness = buildProcessAwareness(snapshot);

        // Level C: Cognitive Assistance
        List<String> actions = new ArrayList<>();
        String cognitiveAssistance = buildCognitiveAssistance(snapshot, actions);

        long duration = System.currentTimeMillis() - startMs;
        System.out.println("[CONTEXT_HELP] Assistance generated in " + duration + "ms for snapshot " + snapshot.getSnapshotId());

        return new ContextualHelpResult(snapshot, selectionHelp, processAwareness, cognitiveAssistance, actions, duration);
    }

    /**
     * Debounces rapid selection changes and asynchronously generates contextual help.
     * Cancels stale pending requests when a new selection arrives.
     */
    public void requestAssistanceDebounced(String activePartName, String selectedObjectName,
                                           String selectedObjectType, String resourcePath,
                                           String projectName, String selectionText,
                                           String sessionId, long debounceMs,
                                           Consumer<ContextualHelpResult> callback) {
        final long requestId = requestCounter.incrementAndGet();

        synchronized (this) {
            if (pendingTask != null && !pendingTask.isDone()) {
                pendingTask.cancel(true);
                System.out.println("[CONTEXT_HELP] Cancelled stale request " + (requestId - 1));
            }

            pendingTask = debounceExecutor.schedule(() -> {
                if (Thread.currentThread().isInterrupted() || requestId != requestCounter.get()) {
                    System.out.println("[CONTEXT_HELP] Discarded outdated assistance computation for request " + requestId);
                    return;
                }

                ContextSnapshot snapshot = resolveSnapshot(activePartName, selectedObjectName, selectedObjectType, resourcePath, projectName, selectionText, sessionId);

                if (Thread.currentThread().isInterrupted() || requestId != requestCounter.get()) {
                    System.out.println("[CONTEXT_HELP] Discarded stale result after resolution for request " + requestId);
                    return;
                }

                ContextualHelpResult result = generateAssistance(snapshot);

                if (requestId == requestCounter.get() && callback != null) {
                    callback.accept(result);
                }
            }, debounceMs > 0 ? debounceMs : 250, TimeUnit.MILLISECONDS);
        }
    }

    private boolean isCorrelated(String selectedName, String resourcePath, String corrRes, String procName, String status) {
        if (corrRes != null && !corrRes.isEmpty()) {
            if (resourcePath != null && resourcePath.contains(corrRes)) return true;
            if (selectedName != null && selectedName.contains(corrRes)) return true;
        }
        if (selectedName != null && !selectedName.isEmpty()) {
            String lowerSel = selectedName.toLowerCase();
            if (procName != null && procName.toLowerCase().contains(lowerSel)) return true;
            if (status != null && status.toLowerCase().contains(lowerSel)) return true;
        }
        return false;
    }

    private String buildQueryText(String selectedName, String resourcePath, String procName, String procPhase) {
        StringBuilder sb = new StringBuilder();
        if (selectedName != null && !selectedName.isEmpty()) sb.append(selectedName).append(" ");
        if (resourcePath != null && !resourcePath.isEmpty()) sb.append(resourcePath).append(" ");
        if (procName != null && !procName.isEmpty()) sb.append(procName).append(" ");
        if (procPhase != null && !procPhase.isEmpty()) sb.append(procPhase).append(" ");
        return sb.toString().trim();
    }

    private String buildSelectionHelp(ContextSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();

        if (!snapshot.hasSelection()) {
            sb.append("No active UI selection. Select an editor, file, or domain object to receive contextual guidance.");
            return sb.toString();
        }

        sb.append("Selection: ").append(snapshot.getSelectedObjectName());
        if (!snapshot.getSelectedObjectType().isEmpty()) {
            sb.append(" (").append(snapshot.getSelectedObjectType()).append(")");
        }
        sb.append("\n");

        if (!snapshot.getActivePartName().isEmpty()) {
            sb.append("Originating View/Page: ").append(snapshot.getActivePartName()).append("\n");
        }

        if (!snapshot.getResourcePath().isEmpty()) {
            sb.append("Resource Path: ").append(snapshot.getResourcePath()).append("\n");
        }

        if (!snapshot.getRetrievedMemories().isEmpty()) {
            sb.append("\nRelevant Persistent Memory:\n");
            for (MemoryEntry mem : snapshot.getRetrievedMemories()) {
                sb.append("• [").append(mem.getType()).append("] ")
                  .append(mem.getContent())
                  .append("\n");
            }
        } else {
            sb.append("\nNo specific persistent memories found for this selection.");
        }

        return sb.toString().trim();
    }

    private String buildProcessAwareness(ContextSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();

        if (!snapshot.hasActiveProcess()) {
            sb.append("Status: Platform Idle. No active background processes or tasks in session '")
              .append(snapshot.getOwningSessionId()).append("'.");
            return sb.toString();
        }

        sb.append("Active Task: ").append(snapshot.getProcessName()).append("\n");
        sb.append("State: ").append(snapshot.getProcessState()).append(" | Phase: ").append(snapshot.getProcessPhase()).append("\n");
        sb.append("Progress: ").append(String.format("%.1f%%", snapshot.getProgressFraction() * 100)).append("\n");

        if (!snapshot.getLastStatus().isEmpty()) {
            sb.append("Recent Status: ").append(snapshot.getLastStatus()).append("\n");
        }

        if (snapshot.isCorrelatedWithSelection()) {
            sb.append("Correlation: Active process directly relates to current selection ('")
              .append(snapshot.getSelectedObjectName()).append("').\n");
        } else {
            sb.append("Correlation: Active process is executing independently of current selection.\n");
        }

        if (!snapshot.getFailureDetails().isEmpty()) {
            sb.append("Failure Details: ").append(snapshot.getFailureDetails()).append("\n");
        }

        return sb.toString().trim();
    }

    private String buildCognitiveAssistance(ContextSnapshot snapshot, List<String> actions) {
        StringBuilder sb = new StringBuilder();

        if ("FAILED".equalsIgnoreCase(snapshot.getProcessState())) {
            sb.append("Observation: Process '").append(snapshot.getProcessName())
              .append("' experienced a failure during phase ").append(snapshot.getProcessPhase()).append(".\n");
            if (!snapshot.getFailureDetails().isEmpty()) {
                sb.append("Evidence: ").append(snapshot.getFailureDetails()).append("\n");
            }
            sb.append("Recommendation: Inspect task logs or run diagnostic verification on affected files.\n");
            actions.add("Inspect Task Logs");
            actions.add("Save Failure Insight as Memory");
            return sb.toString().trim();
        }

        if ("PAUSED".equalsIgnoreCase(snapshot.getProcessState())) {
            sb.append("Observation: Process '").append(snapshot.getProcessName())
              .append("' is paused awaiting user confirmation/approval.\n");
            sb.append("Recommendation: Review pending changes on the Approvals page before proceeding.\n");
            actions.add("Switch to Approvals Page");
            return sb.toString().trim();
        }

        if (snapshot.hasSelection()) {
            sb.append("Observation: User is currently examining '").append(snapshot.getSelectedObjectName()).append("'.\n");
            if (snapshot.hasActiveProcess() && snapshot.isCorrelatedWithSelection()) {
                sb.append("Synthesis: Current process is actively processing this selected component.\n");
                sb.append("Recommendation: Monitor progress or review generated artifacts upon process completion.\n");
                actions.add("Refresh Assistance");
            } else {
                sb.append("Synthesis: Selection is available for AI analysis, code editing, or memory expansion.\n");
                sb.append("Recommendation: Query Memory context or trigger targeted analysis if required.\n");
                actions.add("Save Selection as Memory");
                actions.add("Analyze with AI Chat");
            }
            return sb.toString().trim();
        }

        sb.append("Observation: Platform is operational.\n");
        sb.append("Recommendation: Select a file, task, or editor element to enable targeted contextual help.");
        actions.add("Refresh Assistance");

        return sb.toString().trim();
    }

    public void shutdown() {
        debounceExecutor.shutdownNow();
    }
}
