package eu.kalafatic.evolution.controller.memory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.servers.database.DatabaseManager;

/**
 * Singleton service for thread-safe CRUD and persistent storage of Memory entries.
 *
 * <p>This is the single memory authority of the Cognitive Services platform.
 * It extends the legacy user-memory store with: versioned mutations (JSONL
 * append-only history), explicit validity lifecycle (VALID/OUTDATED/
 * SUPERSEDED/RETRACTED - never hard-deleted), directed relationships between
 * entries, provenance chains, and a SQLite FTS mirror for search. The JSON
 * file format stays backward compatible: legacy entries without the new
 * fields load with safe defaults.</p>
 */
public class MemoryService {
    private static MemoryService instance;

    private final Map<String, MemoryEntry> entries = new ConcurrentHashMap<>();
    private File storageFile;
    private volatile DatabaseManager databaseManager;
    private volatile boolean indexAvailable = false;

    private MemoryService() {
        initStoragePath();
        load();
        initIndexMirror();
        rebuildIndexFromEntries();
    }

    public static synchronized MemoryService getInstance() {
        if (instance == null) {
            instance = new MemoryService();
        }
        return instance;
    }

    /**
     * Test hook: resets the singleton so tests can point it at an isolated
     * workspace. Never used by production code.
     */
    public static synchronized void resetInstance() {
        instance = null;
    }

    private void initStoragePath() {
        try {
            String workspacePath = ProjectModelManager.getWorkspacePath();
            if (workspacePath != null && !workspacePath.isEmpty()) {
                File memoryDir = new File(workspacePath, "shared/memory");
                if (!memoryDir.exists()) {
                    memoryDir.mkdirs();
                }
                this.storageFile = new File(memoryDir, "user_memory.json");
            } else {
                File memoryDir = new File(System.getProperty("user.home"), ".evo/shared/memory");
                if (!memoryDir.exists()) {
                    memoryDir.mkdirs();
                }
                this.storageFile = new File(memoryDir, "user_memory.json");
            }
        } catch (Exception e) {
            File fallback = new File(System.getProperty("user.home"), ".evo/shared/memory/user_memory.json");
            fallback.getParentFile().mkdirs();
            this.storageFile = fallback;
        }
    }

    public synchronized void load() {
        if (storageFile == null || !storageFile.exists()) {
            return;
        }
        try {
            String content = Files.readString(storageFile.toPath(), StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                return;
            }
            JSONArray array = new JSONArray(content);
            entries.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                MemoryEntry entry = MemoryEntry.fromJsonObject(obj);
                if (entry != null && entry.getId() != null) {
                    entries.put(entry.getId(), entry);
                }
            }
        } catch (Exception e) {
            System.err.println("MemoryService: Failed to load user memory from " + storageFile + ": " + e.getMessage());
        }
    }

    public synchronized void save() {
        if (storageFile == null) {
            initStoragePath();
        }
        try {
            JSONArray array = new JSONArray();
            for (MemoryEntry entry : entries.values()) {
                array.put(entry.toJsonObject());
            }
            if (storageFile.getParentFile() != null && !storageFile.getParentFile().exists()) {
                storageFile.getParentFile().mkdirs();
            }
            Files.writeString(storageFile.toPath(), array.toString(2), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("MemoryService: Failed to save user memory to " + storageFile + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Index mirror (SQLite memory_index + FTS) - best effort, never
    // blocks or breaks the JSON store when unavailable.
    // ------------------------------------------------------------------

    private void initIndexMirror() {
        try {
            this.databaseManager = new DatabaseManager();
            this.indexAvailable = true;
        } catch (Exception e) {
            System.err.println("MemoryService: SQLite index mirror unavailable: " + e.getMessage());
            this.indexAvailable = false;
        }
    }

    private void upsertIndex(MemoryEntry entry) {
        if (!indexAvailable || databaseManager == null || entry == null || entry.getId() == null) {
            return;
        }
        String sql = "INSERT OR REPLACE INTO memory_index " +
                "(entry_id, content, scope, type, project_ref, user_ref, application_ref, confidence, validity, updated_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.getId());
            ps.setString(2, entry.getContent() != null ? entry.getContent() : "");
            ps.setString(3, entry.getScope() != null ? entry.getScope().name() : null);
            ps.setString(4, entry.getType() != null ? entry.getType().name() : null);
            ps.setString(5, entry.getProjectRef());
            ps.setString(6, entry.getUserId());
            ps.setString(7, entry.getApplicationId());
            ps.setDouble(8, entry.getConfidence());
            ps.setString(9, entry.getValidity().name());
            ps.setLong(10, entry.getUpdatedAt());
            ps.executeUpdate();
            refreshFts(conn);
        } catch (Exception e) {
            System.err.println("MemoryService: index mirror update failed: " + e.getMessage());
        }
    }

    private void deleteFromIndex(String entryId) {
        if (!indexAvailable || databaseManager == null || entryId == null) {
            return;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM memory_index WHERE entry_id = ?")) {
            ps.setString(1, entryId);
            ps.executeUpdate();
            refreshFts(conn);
        } catch (Exception e) {
            System.err.println("MemoryService: index mirror delete failed: " + e.getMessage());
        }
    }

    private void refreshFts(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO memory_fts(memory_fts) VALUES('rebuild')");
        } catch (Exception ignored) {
            // FTS unavailable -> LIKE fallback covers search
        }
    }

    private void rebuildIndexFromEntries() {
        for (MemoryEntry entry : entries.values()) {
            upsertIndex(entry);
        }
    }

    // ------------------------------------------------------------------
    // Version history (append-only JSONL)
    // ------------------------------------------------------------------

    private void appendVersionRecord(MemoryEntry entry, String changeKind, String actor) {
        if (entry == null || entry.getId() == null) {
            return;
        }
        try {
            MemoryVersion version = new MemoryVersion(entry.getId(), entry.getVersion(),
                    entry.getContent(), entry.getValidity().name(), System.currentTimeMillis(),
                    actor != null ? actor : (changeKind != null ? changeKind : "MUTATION"));
            File versionsDir = new File(storageFile.getParentFile(), "versions");
            if (!versionsDir.exists()) {
                versionsDir.mkdirs();
            }
            File versionFile = new File(versionsDir, entry.getId() + ".jsonl");
            String line = version.toJsonObject().toString() + System.lineSeparator();
            Files.writeString(versionFile.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("MemoryService: failed to append version record: " + e.getMessage());
        }
    }

    public List<MemoryVersion> getVersionHistory(String entryId) {
        if (entryId == null || storageFile == null) {
            return List.of();
        }
        File versionFile = new File(new File(storageFile.getParentFile(), "versions"), entryId + ".jsonl");
        if (!versionFile.exists()) {
            return List.of();
        }
        List<MemoryVersion> versions = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(versionFile.toPath(), StandardCharsets.UTF_8)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                try {
                    MemoryVersion v = MemoryVersion.fromJsonObject(new JSONObject(line));
                    if (v != null) {
                        versions.add(v);
                    }
                } catch (Exception ignored) {
                    // skip corrupted line; append-only log is self-healing per line
                }
            }
        } catch (Exception e) {
            System.err.println("MemoryService: failed to read version history: " + e.getMessage());
        }
        return versions;
    }

    // ------------------------------------------------------------------
    // CRUD (backward compatible) + versioned mutations
    // ------------------------------------------------------------------

    public MemoryEntry addEntry(MemoryEntry entry) {
        if (entry == null) return null;
        if (entry.getId() == null || entry.getId().trim().isEmpty()) {
            entry.setId(java.util.UUID.randomUUID().toString());
        }
        long now = System.currentTimeMillis();
        if (entry.getCreatedAt() <= 0) entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        if (entry.getVersion() < 1) entry.setVersion(1);
        entries.put(entry.getId(), entry);
        save();
        upsertIndex(entry);
        appendVersionRecord(entry, "STORE", entry.getApplicationId());
        publishEvent(RuntimeEventType.MEMORY_STORED, entry);
        return entry;
    }

    public MemoryEntry addEntry(String content, MemoryScope scope, MemoryType type, MemorySource source, MemoryImportance importance) {
        MemoryEntry entry = new MemoryEntry(content, scope, type, source, importance);
        return addEntry(entry);
    }

    public MemoryEntry updateEntry(MemoryEntry entry) {
        if (entry == null || entry.getId() == null) return null;
        MemoryEntry existing = entries.get(entry.getId());
        int previousVersion = existing != null ? existing.getVersion() : entry.getVersion();
        entry.setUpdatedAt(System.currentTimeMillis());
        entry.setVersion(previousVersion + 1);
        entries.put(entry.getId(), entry);
        save();
        upsertIndex(entry);
        appendVersionRecord(entry, "UPDATE", entry.getApplicationId());
        publishEvent(RuntimeEventType.MEMORY_UPDATED, entry);
        return entry;
    }

    /**
     * Retraction is the user-facing deletion: the entry remains persisted with
     * validity=RETRACTED and a version record; derived entries are flagged for
     * re-verification. Hard deletion is admin-only via deleteEntry.
     */
    public MemoryEntry retractEntry(String id, String reason, String actor) {
        MemoryEntry entry = entries.get(id);
        if (entry == null) return null;
        entry.setValidity(MemoryValidity.RETRACTED);
        entry.getMetadata().put("retractionReason", reason != null ? reason : "");
        return updateEntryInternal(entry, "RETRACT", actor);
    }

    public MemoryEntry markOutdated(String id, String actor) {
        MemoryEntry entry = entries.get(id);
        if (entry == null) return null;
        entry.setValidity(MemoryValidity.OUTDATED);
        return updateEntryInternal(entry, "MARK_OUTDATED", actor);
    }

    public MemoryEntry markSuperseded(String id, String supersededByEntryId, String actor) {
        MemoryEntry entry = entries.get(id);
        if (entry == null) return null;
        entry.setValidity(MemoryValidity.SUPERSEDED);
        if (supersededByEntryId != null) {
            // "successor SUPERSEDES superseded-entry"
            entry.getRelationships().add(new MemoryRelationship(supersededByEntryId, id, MemoryRelationship.Kind.SUPERSEDES));
        }
        return updateEntryInternal(entry, "SUPERSEDE", actor);
    }

    /** Re-verification refreshes freshness and validity. */
    public MemoryEntry verifyEntry(String id, double confidence, String actor) {
        MemoryEntry entry = entries.get(id);
        if (entry == null) return null;
        entry.setLastVerifiedAt(System.currentTimeMillis());
        entry.setConfidence(confidence);
        if (entry.getValidity() == MemoryValidity.OUTDATED) {
            entry.setValidity(MemoryValidity.VALID);
        }
        return updateEntryInternal(entry, "VERIFY", actor);
    }

    /** Adds a directed relationship between two existing entries. */
    public MemoryRelationship addRelationship(String fromEntryId, String toEntryId, MemoryRelationship.Kind kind) {
        MemoryEntry from = entries.get(fromEntryId);
        MemoryEntry to = entries.get(toEntryId);
        if (from == null || to == null) {
            throw new IllegalArgumentException("Relationship references unknown entry: " + fromEntryId + " -> " + toEntryId);
        }
        MemoryRelationship rel = new MemoryRelationship(fromEntryId, toEntryId, kind);
        from.getRelationships().add(rel);
        updateEntryInternal(from, "RELATE", null);
        if (kind == MemoryRelationship.Kind.SUPERSEDES) {
            // "from SUPERSEDES to": the target is the superseded entry
            to.setValidity(MemoryValidity.SUPERSEDED);
            updateEntryInternal(to, "SUPERSEDE", null);
        }
        return rel;
    }

    /** Returns all relationships pointing at the given entry. */
    public List<MemoryRelationship> getIncomingRelationships(String entryId) {
        if (entryId == null) return List.of();
        return entries.values().stream()
                .flatMap(e -> e.getRelationships().stream())
                .filter(r -> entryId.equals(r.getToEntryId()))
                .collect(Collectors.toList());
    }

    public List<MemoryRelationship> findConflicts(String entryId) {
        if (entryId == null) return List.of();
        return entries.values().stream()
                .flatMap(e -> e.getRelationships().stream())
                .filter(r -> r.getKind() == MemoryRelationship.Kind.CONTRADICTS)
                .filter(r -> entryId.equals(r.getFromEntryId()) || entryId.equals(r.getToEntryId()))
                .collect(Collectors.toList());
    }

    private MemoryEntry updateEntryInternal(MemoryEntry entry, String changeKind, String actor) {
        int previousVersion = entry.getVersion();
        entry.setUpdatedAt(System.currentTimeMillis());
        entry.setVersion(previousVersion + 1);
        entries.put(entry.getId(), entry);
        save();
        upsertIndex(entry);
        appendVersionRecord(entry, changeKind, actor);
        if ("RETRACT".equals(changeKind)) {
            publishEvent(RuntimeEventType.MEMORY_RETRACTED, entry);
        } else {
            publishEvent(RuntimeEventType.MEMORY_UPDATED, entry);
        }
        return entry;
    }

    public boolean deleteEntry(String id) {
        if (id == null) return false;
        MemoryEntry removed = entries.remove(id);
        if (removed != null) {
            save();
            deleteFromIndex(id);
            return true;
        }
        return false;
    }

    public MemoryEntry getEntryById(String id) {
        if (id == null) return null;
        return entries.get(id);
    }

    public List<MemoryEntry> getAllEntries() {
        List<MemoryEntry> list = new ArrayList<>(entries.values());
        list.sort(Comparator.comparingLong(MemoryEntry::getUpdatedAt).reversed());
        return list;
    }

    public List<MemoryEntry> getAllEntries(MemoryValidity validity) {
        return entries.values().stream()
                .filter(e -> e.getValidity() == validity)
                .sorted(Comparator.comparingLong(MemoryEntry::getUpdatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<MemoryEntry> listByType(MemoryType type) {
        if (type == null) return List.of();
        return entries.values().stream()
                .filter(e -> e.getType() == type)
                .filter(e -> e.getValidity() != MemoryValidity.RETRACTED)
                .sorted(Comparator.comparingLong(MemoryEntry::getUpdatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<MemoryEntry> retrieveRelevant(MemoryQuery query) {
        if (query == null) {
            return getAllEntries();
        }
        return entries.values().stream()
                .filter(query::matches)
                .sorted((e1, e2) -> {
                    int imp1 = e1.getImportance() != null ? e1.getImportance().getLevel() : 1;
                    int imp2 = e2.getImportance() != null ? e2.getImportance().getLevel() : 1;
                    if (imp1 != imp2) {
                        return Integer.compare(imp2, imp1); // higher importance first
                    }
                    return Long.compare(e2.getUpdatedAt(), e1.getUpdatedAt()); // newer first
                })
                .limit(query.getLimit() > 0 ? query.getLimit() : 50)
                .collect(Collectors.toList());
    }

    /**
     * Full-text search over the memory store. Uses SQLite FTS5 over the
     * memory_index mirror when available; falls back to case-insensitive
     * LIKE on the mirror, then to an in-memory substring scan.
     */
    public List<MemoryEntry> search(String query, MemoryScope scope, MemoryType type, String projectRef,
            Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return filteredAll(scope, type, projectRef, limit);
        }
        List<String> ids = new ArrayList<>();
        if (indexAvailable && databaseManager != null) {
            ids = searchIndex(query, scope, type, projectRef, limit != null ? limit : 50);
        }
        if (ids.isEmpty()) {
            // In-memory fallback: token containment
            List<MemoryEntry> results = entries.values().stream()
                    .filter(e -> e.getValidity() != MemoryValidity.RETRACTED)
                    .filter(e -> containsAllTokens(e.getContent(), query))
                    .filter(e -> scope == null || e.getScope() == scope)
                    .filter(e -> type == null || e.getType() == type)
                    .filter(e -> projectRef == null || projectRef.equals(e.getProjectRef()))
                    .sorted(Comparator.comparingLong(MemoryEntry::getUpdatedAt).reversed())
                    .collect(Collectors.toList());
            if (limit != null && results.size() > limit) {
                return results.subList(0, limit);
            }
            return results;
        }
        List<MemoryEntry> results = new ArrayList<>();
        for (String id : ids) {
            MemoryEntry entry = entries.get(id);
            if (entry != null && entry.getValidity() != MemoryValidity.RETRACTED) {
                results.add(entry);
            }
        }
        return results;
    }

    private List<MemoryEntry> filteredAll(MemoryScope scope, MemoryType type, String projectRef, Integer limit) {
        List<MemoryEntry> results = entries.values().stream()
                .filter(e -> e.getValidity() != MemoryValidity.RETRACTED)
                .filter(e -> scope == null || e.getScope() == scope)
                .filter(e -> type == null || e.getType() == type)
                .filter(e -> projectRef == null || projectRef.equals(e.getProjectRef()))
                .sorted(Comparator.comparingLong(MemoryEntry::getUpdatedAt).reversed())
                .collect(Collectors.toList());
        if (limit != null && results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    private List<String> searchIndex(String query, MemoryScope scope, MemoryType type, String projectRef, int limit) {
        List<String> ids = new ArrayList<>();
        // FTS first
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT m.entry_id FROM memory_fts f JOIN memory_index m ON m.rowid = f.rowid " +
                     "WHERE memory_fts MATCH ? LIMIT ?")) {
            ps.setString(1, toFtsQuery(query));
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            // LIKE fallback below
        }
        if (ids.isEmpty()) {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT entry_id FROM memory_index WHERE content LIKE ? LIMIT ?")) {
                ps.setString(1, "%" + query.trim() + "%");
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getString(1));
                    }
                }
            } catch (Exception e) {
                System.err.println("MemoryService: index search failed: " + e.getMessage());
            }
        }
        return ids;
    }

    private String toFtsQuery(String query) {
        StringBuilder sb = new StringBuilder();
        for (String token : query.trim().split("\\s+")) {
            if (token.length() > 1) {
                if (sb.length() > 0) sb.append(" OR ");
                sb.append('"').append(token.replace("\"", "\"\"")).append('"');
            }
        }
        return sb.length() > 0 ? sb.toString() : query.trim();
    }

    private boolean containsAllTokens(String content, String query) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        for (String token : query.trim().toLowerCase().split("\\s+")) {
            if (token.length() > 1 && !lower.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public synchronized void clearAll() {
        entries.clear();
        save();
        if (indexAvailable && databaseManager != null) {
            try (Connection conn = databaseManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM memory_index");
                refreshFts(conn);
            } catch (Exception e) {
                System.err.println("MemoryService: index clear failed: " + e.getMessage());
            }
        }
    }

    public File getStorageFile() {
        return storageFile;
    }

    public int getEntryCount() {
        return entries.size();
    }

    public boolean isIndexAvailable() {
        return indexAvailable;
    }

    private void publishEvent(RuntimeEventType type, MemoryEntry entry) {
        try {
            String sessionRef = entry.getSessionRef();
            if (sessionRef == null || sessionRef.isEmpty()) {
                return;
            }
            var container = SessionManager.getInstance().getSession(sessionRef);
            if (container != null && container.getEventBus() != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("entryId", entry.getId());
                payload.put("type", type.name());
                payload.put("memoryType", entry.getType() != null ? entry.getType().name() : null);
                container.getEventBus().publish(new RuntimeEvent(type, sessionRef, "MemoryService", payload));
            }
        } catch (Exception ignored) {
            // Events are best-effort; never break the memory operation
        }
    }
}
