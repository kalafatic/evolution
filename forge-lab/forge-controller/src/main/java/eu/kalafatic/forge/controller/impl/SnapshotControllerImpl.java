package eu.kalafatic.forge.controller.impl;

import eu.kalafatic.forge.controller.api.SnapshotController;
import eu.kalafatic.forge.controller.api.SnapshotInfo;
import eu.kalafatic.forge.controller.service.SnapshotService;
import java.util.List;

public class SnapshotControllerImpl implements SnapshotController {
    private final SnapshotService snapshotService;

    public SnapshotControllerImpl(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    public void saveSnapshot(String sessionId) {
        snapshotService.saveSnapshot(sessionId);
    }

    @Override
    public void restoreSnapshot(String sessionId, String snapshotId) {
        snapshotService.restoreSnapshot(sessionId, snapshotId);
    }

    @Override
    public List<SnapshotInfo> getSnapshots(String sessionId) {
        return snapshotService.getSnapshots(sessionId);
    }
}
