package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.controller.api.SnapshotInfo;
import java.util.List;

public interface SnapshotService {
    void saveSnapshot(String sessionId);
    void restoreSnapshot(String sessionId, String snapshotId);
    List<SnapshotInfo> getSnapshots(String sessionId);
}
