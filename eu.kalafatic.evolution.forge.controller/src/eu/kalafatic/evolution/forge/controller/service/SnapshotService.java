package eu.kalafatic.evolution.forge.controller.service;

import java.util.List;

import eu.kalafatic.evolution.forge.controller.api.SnapshotInfo;

public interface SnapshotService {
    void saveSnapshot(String sessionId);
    void restoreSnapshot(String sessionId, String snapshotId);
    List<SnapshotInfo> getSnapshots(String sessionId);
}
