package eu.kalafatic.evolution.controller.cognitive;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.model.ConversationRecord;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Asynchronous post-interaction consolidation: conversation transcripts
 * become EPISODE entries; repeated tool outcomes become EXPERIENCE entries.
 * Runs on a dedicated executor and never blocks inference. Consolidation is
 * deterministic in the first generation - no LLM summarization dependency.
 */
public class MemoryConsolidationService {
    private static final Logger LOGGER = Logger.getLogger(MemoryConsolidationService.class.getName());

    private static MemoryConsolidationService instance;

    private final MemoryService memoryService;
    private final ConversationHistoryService conversationHistoryService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "evo-memory-consolidation");
        t.setDaemon(true);
        return t;
    });

    private MemoryConsolidationService() {
        this.memoryService = MemoryService.getInstance();
        this.conversationHistoryService = ConversationHistoryService.getInstance();
    }

    public static synchronized MemoryConsolidationService getInstance() {
        if (instance == null) {
            instance = new MemoryConsolidationService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public static class ConsolidationJob {
        public String conversationId;
        public String sessionId;
        public String projectRef;
        public String applicationId;
        public String userId;
        public boolean success;
        public String outcomeSummary;

        public ConsolidationJob(String conversationId, String sessionId, String projectRef,
                String applicationId, String userId, boolean success, String outcomeSummary) {
            this.conversationId = conversationId;
            this.sessionId = sessionId;
            this.projectRef = projectRef;
            this.applicationId = applicationId;
            this.userId = userId;
            this.success = success;
            this.outcomeSummary = outcomeSummary;
        }
    }

    /** Submits a post-session consolidation job; returns immediately. */
    public void consolidate(ConsolidationJob job) {
        if (job == null || job.conversationId == null) {
            return;
        }
        executor.submit(() -> {
            try {
                runConsolidation(job);
            } catch (Exception e) {
                LOGGER.severe("MemoryConsolidationService: consolidation failed: " + e.getMessage());
            }
        });
    }

    private void runConsolidation(ConsolidationJob job) {
        List<ConversationRecord> records = conversationHistoryService.list(job.conversationId, 0, Integer.MAX_VALUE);
        if (records.isEmpty()) {
            return;
        }
        StringBuilder transcript = new StringBuilder();
        for (ConversationRecord r : records) {
            if (r.getContent() != null && !r.getContent().isBlank()) {
                transcript.append(r.getRole()).append(": ").append(r.getContent()).append('\n');
            }
        }
        if (transcript.length() == 0) {
            return;
        }
        String summary = transcript.length() > 4000
                ? transcript.substring(0, 4000) + "..."
                : transcript.toString();

        MemoryEntry episode = new MemoryEntry();
        episode.setContent(summary);
        episode.setScope(job.projectRef != null ? MemoryScope.PROJECT : MemoryScope.SESSION);
        episode.setType(MemoryType.EPISODIC);
        episode.setSource(MemorySource.CONSOLIDATION);
        episode.setImportance(job.success ? MemoryImportance.HIGH : MemoryImportance.MEDIUM);
        episode.setConfidence(job.success ? 0.9 : 0.7);
        episode.setUserId(job.userId);
        episode.setApplicationId(job.applicationId);
        episode.setProjectRef(job.projectRef);
        episode.setSessionRef(job.sessionId);
        episode.getMetadata().put("conversationId", job.conversationId);
        episode.getMetadata().put("outcome", job.success ? "SUCCESS" : "FAILURE");
        if (job.outcomeSummary != null) {
            episode.getMetadata().put("outcomeSummary", job.outcomeSummary);
        }
        episode.getProvenanceChain().add("CONSOLIDATION:" + job.conversationId);
        memoryService.addEntry(episode);

        // Tool outcome summary as EXPERIENCE when meaningful results exist
        long toolCalls = records.stream().filter(r -> "TOOL_CALL".equals(r.getRole())).count();
        long toolResults = records.stream().filter(r -> "TOOL_RESULT".equals(r.getRole())).count();
        if (toolCalls > 0 && toolResults > 0 && job.outcomeSummary != null) {
            MemoryEntry experience = new MemoryEntry();
            experience.setContent("Outcome of " + toolCalls + " tool operations: " + job.outcomeSummary);
            experience.setScope(job.projectRef != null ? MemoryScope.PROJECT : MemoryScope.SESSION);
            experience.setType(MemoryType.EXPERIENCE);
            experience.setSource(MemorySource.CONSOLIDATION);
            experience.setImportance(MemoryImportance.MEDIUM);
            experience.setConfidence(0.8);
            experience.setUserId(job.userId);
            experience.setApplicationId(job.applicationId);
            experience.setProjectRef(job.projectRef);
            experience.setSessionRef(job.sessionId);
            experience.getMetadata().put("conversationId", job.conversationId);
            experience.getProvenanceChain().add("CONSOLIDATION:" + job.conversationId);
            memoryService.addEntry(experience);
        }

        publishEvent(job.sessionId, job.conversationId);
    }

    private void publishEvent(String sessionId, String conversationId) {
        try {
            if (sessionId == null) {
                return;
            }
            var container = SessionManager.getInstance().getSession(sessionId);
            if (container != null && container.getEventBus() != null) {
                container.getEventBus().publish(new RuntimeEvent(RuntimeEventType.MEMORY_CONSOLIDATED,
                        sessionId, "MemoryConsolidationService", conversationId));
            }
        } catch (Exception ignored) {
        }
    }
}
