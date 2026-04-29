package eu.kalafatic.evolution.controller.orchestration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tracks file changes during an orchestration session.
 */
public class FileChangeTracker {

    public enum ChangeType {
        NEW, EDITED, REMOVED
    }

    private final Map<String, ChangeType> changedFiles = new HashMap<>();

    public synchronized void recordChange(String path, ChangeType type) {
        if (type == ChangeType.EDITED && changedFiles.get(path) == ChangeType.NEW) {
            // Keep it as NEW if it was just created in this session
            return;
        }
        changedFiles.put(path, type);
    }

    public synchronized ChangeType getChangeType(String path) {
        return changedFiles.get(path);
    }

    public synchronized Map<String, ChangeType> getChangedFiles() {
        return Collections.unmodifiableMap(new HashMap<>(changedFiles));
    }

    public synchronized void clear() {
        changedFiles.clear();
    }
}
