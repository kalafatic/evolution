package eu.kalafatic.evolution.forge.controller.api;

import java.util.List;
import java.util.Map;

public interface SnapshotController {

    void saveSnapshot(String sessionId);

    void restoreSnapshot(
            String sessionId,
            String snapshotId);

    List<Map<String, Object>> getSnapshots(
            String sessionId);

    Map<String, Object> getSnapshot(String snapshotId);

    Map<String, Object> compareSnapshots(String snapshotA, String snapshotB);
}
