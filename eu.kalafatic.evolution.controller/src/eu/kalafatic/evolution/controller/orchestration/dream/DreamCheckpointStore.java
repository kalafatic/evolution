package eu.kalafatic.evolution.controller.orchestration.dream;

import java.io.File;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;

/**
 * Persistence utility for saving and loading DREAM checkpoints across application restarts.
 */
public class DreamCheckpointStore {

    public static synchronized void saveCheckpoint(DreamCheckpoint cp) {
        if (cp == null) return;
        try {
            File file = getStorageFile();
            if (file != null) {
                file.getParentFile().mkdirs();
                java.nio.file.Files.writeString(file.toPath(), cp.toJsonObject().toString(2));
            }
        } catch (Exception e) {
            System.err.println("[DREAM_STORE] Failed to save DREAM checkpoint: " + e.getMessage());
        }
    }

    public static synchronized DreamCheckpoint loadCheckpoint() {
        try {
            File file = getStorageFile();
            if (file != null && file.exists()) {
                String jsonStr = java.nio.file.Files.readString(file.toPath());
                if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                    return DreamCheckpoint.fromJsonObject(new JSONObject(jsonStr));
                }
            }
        } catch (Exception e) {
            System.err.println("[DREAM_STORE] Failed to load DREAM checkpoint: " + e.getMessage());
        }
        return null;
    }

    public static synchronized void clearCheckpoint() {
        try {
            File file = getStorageFile();
            if (file != null && file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            System.err.println("[DREAM_STORE] Failed to clear DREAM checkpoint: " + e.getMessage());
        }
    }

    private static File getStorageFile() {
        String ws = ProjectModelManager.getWorkspacePath();
        if (ws != null && !ws.isEmpty()) {
            return new File(ws, "shared/memory/dream_checkpoint.json");
        }
        return new File(System.getProperty("user.home"), ".evo/shared/memory/dream_checkpoint.json");
    }
}
