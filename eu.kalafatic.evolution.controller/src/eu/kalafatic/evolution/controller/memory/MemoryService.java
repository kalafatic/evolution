package eu.kalafatic.evolution.controller.memory;

import java.io.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;

/**
 * Singleton service for thread-safe CRUD and persistent storage of Memory entries.
 */
public class MemoryService {
    private static MemoryService instance;

    private final Map<String, MemoryEntry> entries = new ConcurrentHashMap<>();
    private File storageFile;

    private MemoryService() {
        initStoragePath();
        load();
    }

    public static synchronized MemoryService getInstance() {
        if (instance == null) {
            instance = new MemoryService();
        }
        return instance;
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

    public MemoryEntry addEntry(MemoryEntry entry) {
        if (entry == null) return null;
        if (entry.getId() == null || entry.getId().trim().isEmpty()) {
            entry.setId(java.util.UUID.randomUUID().toString());
        }
        long now = System.currentTimeMillis();
        if (entry.getCreatedAt() <= 0) entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entries.put(entry.getId(), entry);
        save();
        return entry;
    }

    public MemoryEntry addEntry(String content, MemoryScope scope, MemoryType type, MemorySource source, MemoryImportance importance) {
        MemoryEntry entry = new MemoryEntry(content, scope, type, source, importance);
        return addEntry(entry);
    }

    public MemoryEntry updateEntry(MemoryEntry entry) {
        if (entry == null || entry.getId() == null) return null;
        entry.setUpdatedAt(System.currentTimeMillis());
        entries.put(entry.getId(), entry);
        save();
        return entry;
    }

    public boolean deleteEntry(String id) {
        if (id == null) return false;
        MemoryEntry removed = entries.remove(id);
        if (removed != null) {
            save();
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

    public synchronized void clearAll() {
        entries.clear();
        save();
    }

    public File getStorageFile() {
        return storageFile;
    }
}
