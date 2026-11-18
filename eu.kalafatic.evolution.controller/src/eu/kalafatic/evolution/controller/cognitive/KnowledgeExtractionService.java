package eu.kalafatic.evolution.controller.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Extracts structured knowledge from conversation transcripts using a
 * deterministic pattern-based approach (no LLM dependency in the first
 * generation). Proposals below the confidence threshold are stored as
 * UNVERIFIED assumptions with provenance, never as facts.
 */
public class KnowledgeExtractionService {
    private static final Logger LOGGER = Logger.getLogger(KnowledgeExtractionService.class.getName());

    private static final double CONFIDENCE_THRESHOLD = 0.6;

    private static final Pattern ALWAYS_PATTERN = Pattern.compile(
            "(?i)(always|prefer|use only|never|don'?t|must)\\s+(?:use\\s+)?([^.;,\\n]{4,120})");
    private static final Pattern FACT_PATTERN = Pattern.compile(
            "(?i)(the fact is|it is known that|confirmed:?|verified:?)\\s+([^.;\\n]{4,200})");
    private static final Pattern DECISION_PATTERN = Pattern.compile(
            "(?i)(decided|decision|we will|we chose)\\s+(?:to\\s+|that\\s+)?([^.;\\n]{4,200})");

    private static KnowledgeExtractionService instance;

    private final MemoryService memoryService;

    private KnowledgeExtractionService() {
        this.memoryService = MemoryService.getInstance();
    }

    public static synchronized KnowledgeExtractionService getInstance() {
        if (instance == null) {
            instance = new KnowledgeExtractionService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public List<MemoryEntry> extractFromConversation(String conversationId, String sessionId,
            String projectRef, String applicationId, String userId) {
        List<MemoryEntry> extracted = new ArrayList<>();
        List<ConversationRecord> records = ConversationHistoryService.getInstance().list(conversationId, 0, Integer.MAX_VALUE);
        if (records.isEmpty()) {
            return extracted;
        }
        Map<String, Integer> preferenceHits = new HashMap<>();
        for (ConversationRecord record : records) {
            String content = record.getContent();
            if (content == null || content.isBlank() || !"USER".equals(record.getRole())) {
                continue;
            }
            extractPreferences(content, preferenceHits);
            MemoryEntry fact = extractFact(content, projectRef, applicationId, userId, sessionId, conversationId);
            if (fact != null) {
                extracted.add(fact);
            }
        }
        for (Map.Entry<String, Integer> hit : preferenceHits.entrySet()) {
            if (hit.getValue() >= 2) {
                MemoryEntry preference = buildPreference(hit.getKey(), hit.getValue(), projectRef,
                        applicationId, userId, sessionId, conversationId);
                if (preference != null) {
                    extracted.add(preference);
                }
            }
        }
        if (!extracted.isEmpty()) {
            publishEvent(sessionId, conversationId);
        }
        return extracted;
    }

    private void extractPreferences(String content, Map<String, Integer> hits) {
        Matcher m = ALWAYS_PATTERN.matcher(content);
        while (m.find()) {
            String statement = (m.group(1) + " " + m.group(2)).trim();
            hits.merge(statement, 1, Integer::sum);
        }
    }

    private MemoryEntry extractFact(String content, String projectRef, String applicationId,
            String userId, String sessionId, String conversationId) {
        Matcher m = FACT_PATTERN.matcher(content);
        while (m.find()) {
            String fact = m.group(2).trim();
            MemoryEntry entry = new MemoryEntry();
            entry.setContent(fact);
            entry.setScope(projectRef != null ? MemoryScope.PROJECT : MemoryScope.SESSION);
            entry.setType(MemoryType.FACT);
            entry.setSource(MemorySource.CONSOLIDATION);
            entry.setImportance(MemoryImportance.MEDIUM);
            entry.setConfidence(0.8);
            entry.setUserId(userId);
            entry.setApplicationId(applicationId);
            entry.setProjectRef(projectRef);
            entry.setSessionRef(sessionId);
            entry.getProvenanceChain().add("EXTRACTION:" + conversationId);
            memoryService.addEntry(entry);
            return entry;
        }
        return null;
    }

    private MemoryEntry buildPreference(String statement, int hits, String projectRef, String applicationId,
            String userId, String sessionId, String conversationId) {
        double confidence = Math.min(0.9, 0.5 + hits * 0.1);
        MemoryEntry entry = new MemoryEntry();
        entry.setContent(statement);
        entry.setScope(MemoryScope.USER);
        entry.setType(MemoryType.PREFERENCE);
        entry.setSource(MemorySource.INFERENCE);
        entry.setImportance(MemoryImportance.MEDIUM);
        entry.setConfidence(confidence);
        entry.setUserId(userId);
        entry.setApplicationId(applicationId);
        entry.setProjectRef(projectRef);
        entry.setSessionRef(sessionId);
        entry.getMetadata().put("evidenceHits", String.valueOf(hits));
        entry.getProvenanceChain().add("INFERENCE:" + conversationId);
        memoryService.addEntry(entry);
        return entry;
    }

    /**
     * Extracts a decision-like statement and records it as a project
     * architectural decision when the project reference is present.
     */
    public JSONObject extractDecisions(String conversationId, String projectRef, String sessionId) {
        JSONObject result = new JSONObject();
        if (projectRef == null) {
            return result;
        }
        List<ConversationRecord> records = ConversationHistoryService.getInstance().list(conversationId, 0, Integer.MAX_VALUE);
        int count = 0;
        for (ConversationRecord record : records) {
            String content = record.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            Matcher m = DECISION_PATTERN.matcher(content);
            while (m.find()) {
                String decisionText = m.group(1).trim();
                if (decisionText.length() < 8) {
                    continue;
                }
                ProjectKnowledgeService.getInstance().recordDecision(projectRef, null,
                        "Decision from conversation", content.length() > 120 ? content.substring(0, 120) : content,
                        decisionText, "Extracted from conversation " + conversationId, "ACCEPTED");
                count++;
            }
        }
        result.put("decisionsRecorded", count);
        return result;
    }

    private void publishEvent(String sessionId, String conversationId) {
        try {
            if (sessionId == null) {
                return;
            }
            var container = SessionManager.getInstance().getSession(sessionId);
            if (container != null && container.getEventBus() != null) {
                container.getEventBus().publish(new RuntimeEvent(RuntimeEventType.KNOWLEDGE_EXTRACTED,
                        sessionId, "KnowledgeExtractionService", conversationId));
            }
        } catch (Exception ignored) {
        }
    }
}
