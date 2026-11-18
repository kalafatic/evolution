package eu.kalafatic.evolution.controller.cognitive;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.model.ConversationRecord;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.servers.database.DatabaseManager;

/**
 * Durable append-only conversation history store.
 *
 * <p>Every record is written to a per-conversation JSONL transcript under
 * <code>&lt;workspace&gt;/shared/conversations/</code> and indexed in the
 * SQLite conversation_messages table (FTS5 for search). The JSONL transcript
 * is authoritative for replay; the SQL index is authoritative for search.
 * Writes never delete: compaction only produces summaries.</p>
 */
public class ConversationHistoryService {
    private static final Logger LOGGER = Logger.getLogger(ConversationHistoryService.class.getName());

    private static ConversationHistoryService instance;

    private final ConcurrentHashMap<String, AtomicLong> sequences = new ConcurrentHashMap<>();
    private volatile DatabaseManager databaseManager;
    private volatile boolean indexAvailable = false;
    private File conversationsDir;

    private ConversationHistoryService() {
        initStoragePath();
        initDatabase();
    }

    public static synchronized ConversationHistoryService getInstance() {
        if (instance == null) {
            instance = new ConversationHistoryService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    private void initStoragePath() {
        try {
            String workspacePath = ProjectModelManager.getWorkspacePath();
            if (workspacePath != null && !workspacePath.isEmpty()) {
                this.conversationsDir = new File(new File(workspacePath, "shared"), "conversations");
            } else {
                this.conversationsDir = new File(new File(System.getProperty("user.home"), ".evo/shared"), "conversations");
            }
        } catch (Exception e) {
            this.conversationsDir = new File(new File(System.getProperty("user.home"), ".evo/shared"), "conversations");
        }
        if (!conversationsDir.exists()) {
            conversationsDir.mkdirs();
        }
    }

    private void initDatabase() {
        try {
            this.databaseManager = new DatabaseManager();
            this.indexAvailable = true;
        } catch (Exception e) {
            LOGGER.warning("ConversationHistoryService: SQLite index unavailable: " + e.getMessage());
            this.indexAvailable = false;
        }
    }

    private File transcriptFile(String conversationId) {
        return new File(conversationsDir, conversationId + ".jsonl");
    }

    private synchronized long nextSequence(String conversationId) {
        AtomicLong counter = sequences.computeIfAbsent(conversationId, k -> new AtomicLong(0));
        long max = counter.get();
        // Recover the sequence counter from the SQL index on first use.
        if (max == 0 && indexAvailable && databaseManager != null) {
            max = maxSequenceFromIndex(conversationId);
        }
        counter.set(max + 1);
        return max + 1;
    }

    private long maxSequenceFromIndex(String conversationId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(MAX(sequence), 0) FROM conversation_messages WHERE conversation_id = ?")) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Appends one record. The JSONL write and the SQL index row happen in
     * sequence; a SQL failure never loses the transcript record (durable
     * first, index second).
     */
    public ConversationRecord append(String conversationId, String role, String content) {
        return append(conversationId, role, null, null, content, null);
    }

    public ConversationRecord appendToolCall(String conversationId, String toolName, String content) {
        return append(conversationId, ConversationRecord.ROLE_TOOL_CALL, null, toolName, content, null);
    }

    public ConversationRecord appendToolResult(String conversationId, String toolName, String content) {
        return append(conversationId, ConversationRecord.ROLE_TOOL_RESULT, null, toolName, content, null);
    }

    public ConversationRecord appendDecision(String conversationId, String decisionId, String content) {
        return append(conversationId, ConversationRecord.ROLE_DECISION, null, null, content,
                decisionId != null ? new JSONObject().put("decisionId", decisionId).toString() : null);
    }

    public ConversationRecord appendError(String conversationId, String content) {
        return append(conversationId, ConversationRecord.ROLE_ERROR, null, null, content, null);
    }

    public synchronized ConversationRecord append(String conversationId, String role, String agentType,
            String toolName, String content, String metadata) {
        if (conversationId == null || conversationId.isEmpty()) {
            throw new IllegalArgumentException("conversationId is required");
        }
        if (content == null) {
            content = "";
        }
        ConversationRecord record = new ConversationRecord();
        record.setConversationId(conversationId);
        record.setSequence(nextSequence(conversationId));
        record.setRole(role != null ? role : ConversationRecord.ROLE_EVENT);
        record.setAgentType(agentType);
        record.setToolName(toolName);
        record.setContent(content);
        record.setTokens(TokenEstimationService.getInstance().estimateTokens(content));
        record.setTimestamp(System.currentTimeMillis());
        record.setMetadata(metadata);

        // 1. Durable transcript first
        try {
            File file = transcriptFile(conversationId);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            String line = record.toJsonObject().toString() + System.lineSeparator();
            Files.writeString(file.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            LOGGER.severe("ConversationHistoryService: transcript write failed: " + e.getMessage());
            throw new IllegalStateException("Failed to persist conversation record", e);
        }

        // 2. Index row (best effort)
        if (indexAvailable && databaseManager != null) {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO conversation_messages (conversation_id, sequence, role, agent_type, tool_name, content, tokens, timestamp, metadata) " +
                         "VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, conversationId);
                ps.setLong(2, record.getSequence());
                ps.setString(3, record.getRole());
                ps.setString(4, record.getAgentType());
                ps.setString(5, record.getToolName());
                ps.setString(6, record.getContent());
                ps.setInt(7, record.getTokens());
                ps.setLong(8, record.getTimestamp());
                ps.setString(9, record.getMetadata());
                ps.executeUpdate();
                refreshFts(conn);
            } catch (Exception e) {
                LOGGER.warning("ConversationHistoryService: index write failed (transcript intact): " + e.getMessage());
            }
        }
        return record;
    }

    private void refreshFts(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO conversation_messages_fts(conversation_messages_fts) VALUES('rebuild')");
        } catch (Exception ignored) {
            // FTS unavailable -> LIKE fallback covers search
        }
    }

    /**
     * Replays the transcript from the SQL index (searchable); falls back to
     * JSONL replay when the index is unavailable.
     */
    public List<ConversationRecord> list(String conversationId, long fromSequence, int limit) {
        List<ConversationRecord> records = new ArrayList<>();
        if (conversationId == null) {
            return records;
        }
        if (indexAvailable && databaseManager != null) {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, conversation_id, sequence, role, agent_type, tool_name, content, tokens, timestamp, metadata " +
                         "FROM conversation_messages WHERE conversation_id = ? AND sequence >= ? ORDER BY sequence ASC LIMIT ?")) {
                ps.setString(1, conversationId);
                ps.setLong(2, fromSequence);
                ps.setInt(3, limit > 0 ? limit : 200);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapRow(rs));
                    }
                }
                return records;
            } catch (Exception e) {
                LOGGER.warning("ConversationHistoryService: index read failed, falling back to transcript: " + e.getMessage());
            }
        }
        replayTranscript(conversationId, fromSequence, limit, records);
        return records;
    }

    private void replayTranscript(String conversationId, long fromSequence, int limit, List<ConversationRecord> out) {
        File file = transcriptFile(conversationId);
        if (!file.exists()) {
            return;
        }
        try {
            int count = 0;
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                try {
                    ConversationRecord r = ConversationRecord.fromJsonObject(new JSONObject(line));
                    if (r != null && r.getSequence() >= fromSequence) {
                        out.add(r);
                        if (limit > 0 && ++count >= limit) {
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    // skip corrupted line; append-only log is self-healing per line
                }
            }
        } catch (Exception e) {
            LOGGER.severe("ConversationHistoryService: transcript replay failed: " + e.getMessage());
        }
    }

    private ConversationRecord mapRow(ResultSet rs) throws java.sql.SQLException {
        ConversationRecord r = new ConversationRecord();
        r.setId(rs.getLong("id"));
        r.setConversationId(rs.getString("conversation_id"));
        r.setSequence(rs.getLong("sequence"));
        r.setRole(rs.getString("role"));
        r.setAgentType(rs.getString("agent_type"));
        r.setToolName(rs.getString("tool_name"));
        r.setContent(rs.getString("content"));
        r.setTokens(rs.getInt("tokens"));
        r.setTimestamp(rs.getLong("timestamp"));
        r.setMetadata(rs.getString("metadata"));
        return r;
    }

    /** FTS search over conversation content; LIKE fallback on the index. */
    public List<ConversationRecord> search(String query, int limit) {
        List<ConversationRecord> records = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return records;
        }
        if (indexAvailable && databaseManager != null) {
            boolean ftsMatched = false;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT cm.id, cm.conversation_id, cm.sequence, cm.role, cm.agent_type, cm.tool_name, " +
                         "cm.content, cm.tokens, cm.timestamp, cm.metadata FROM conversation_messages_fts f " +
                         "JOIN conversation_messages cm ON cm.id = f.rowid " +
                         "WHERE conversation_messages_fts MATCH ? ORDER BY rank LIMIT ?")) {
                ps.setString(1, ftsQuery(query));
                ps.setInt(2, limit > 0 ? limit : 50);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapRow(rs));
                    }
                }
                ftsMatched = !records.isEmpty();
            } catch (Exception e) {
                // LIKE fallback below
            }
            if (!ftsMatched) {
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT id, conversation_id, sequence, role, agent_type, tool_name, content, tokens, timestamp, metadata " +
                             "FROM conversation_messages WHERE content LIKE ? ORDER BY timestamp DESC LIMIT ?")) {
                    ps.setString(1, "%" + query.trim() + "%");
                    ps.setInt(2, limit > 0 ? limit : 50);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            records.add(mapRow(rs));
                        }
                    }
                } catch (Exception e2) {
                    LOGGER.warning("ConversationHistoryService: search failed: " + e2.getMessage());
                }
            }
        }
        return records;
    }

    private String ftsQuery(String query) {
        StringBuilder sb = new StringBuilder();
        for (String token : query.trim().split("\\s+")) {
            if (token.length() > 1) {
                if (sb.length() > 0) sb.append(" OR ");
                sb.append('"').append(token.replace("\"", "\"\"")).append('"');
            }
        }
        return sb.length() > 0 ? sb.toString() : query.trim();
    }

    /**
     * Produces an extractive summary of a conversation bounded by a token
     * budget: the opening records plus the most recent records until the
     * budget is exhausted. Never modifies the transcript.
     */
    public String compact(String conversationId, int budgetTokens) {
        List<ConversationRecord> all = list(conversationId, 0, Integer.MAX_VALUE);
        if (all.isEmpty()) {
            return "";
        }
        int head = Math.min(4, all.size());
        List<String> parts = new ArrayList<>();
        parts.add("[Conversation summary]");
        TokenEstimationService tokens = TokenEstimationService.getInstance();
        int used = tokens.estimateTokens(parts.get(0));
        int headAdded = 0;
        for (int i = 0; i < head; i++) {
            ConversationRecord r = all.get(i);
            String line = formatLine(r);
            int cost = tokens.estimateTokens(line);
            if (used + cost > budgetTokens) {
                break;
            }
            parts.add(line);
            used += cost;
            headAdded = i + 1;
        }
        List<String> tail = new ArrayList<>();
        for (int i = all.size() - 1; i >= headAdded; i--) {
            String line = formatLine(all.get(i));
            int cost = tokens.estimateTokens(line);
            if (used + cost > budgetTokens) {
                break;
            }
            tail.add(0, line);
            used += cost;
        }
        parts.addAll(tail);
        return String.join("\n", parts);
    }

    private String formatLine(ConversationRecord r) {
        String prefix = r.getRole();
        if (r.getToolName() != null) {
            prefix = prefix + "(" + r.getToolName() + ")";
        }
        String content = r.getContent();
        if (content != null && content.length() > 500) {
            content = content.substring(0, 500) + "...";
        }
        return prefix + ": " + content;
    }

    public long count(String conversationId) {
        if (indexAvailable && databaseManager != null) {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) FROM conversation_messages WHERE conversation_id = ?")) {
                ps.setString(1, conversationId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            } catch (Exception ignored) {
            }
        }
        List<ConversationRecord> all = new ArrayList<>();
        replayTranscript(conversationId, 0, Integer.MAX_VALUE, all);
        return all.size();
    }

    public boolean isIndexAvailable() {
        return indexAvailable;
    }
}
