package eu.kalafatic.forge.controller.api;

import java.util.List;

public interface SnapshotController {

    void saveSnapshot(String sessionId);

    void restoreSnapshot(
            String sessionId,
            String snapshotId);

    List<SnapshotInfo> getSnapshots(
            String sessionId);
}
