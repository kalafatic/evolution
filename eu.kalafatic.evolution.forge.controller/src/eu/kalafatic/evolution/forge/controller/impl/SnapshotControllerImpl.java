package eu.kalafatic.evolution.forge.controller.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.forge.controller.api.SnapshotController;
import eu.kalafatic.evolution.forge.controller.service.SnapshotService;

public class SnapshotControllerImpl implements SnapshotController {
    private final SnapshotService snapshotService;

    public SnapshotControllerImpl(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    public void saveSnapshot(String sessionId) {
        if (snapshotService != null) snapshotService.saveSnapshot(sessionId);
    }

    @Override
    public void restoreSnapshot(String sessionId, String snapshotId) {
        if (snapshotService != null) snapshotService.restoreSnapshot(sessionId, snapshotId);
    }

    @Override
    public List<Map<String, Object>> getSnapshots(String sessionId) {
        // Honest unavailability: no fabricated "Initial Snapshot" entries.
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getSnapshot(String snapshotId) {
        Map<String, Object> s = new HashMap<>();
        s.put("id", snapshotId);
        s.put("available", snapshotService != null);
        return s;
    }

    @Override
    public Map<String, Object> compareSnapshots(String snapshotA, String snapshotB) {
        // Honest unavailability: a real diff requires persisted snapshot state,
        // which the legacy snapshot service does not provide.
        Map<String, Object> diff = new HashMap<>();
        diff.put("available", false);
        diff.put("reason", "Snapshot comparison is not available through the legacy forge controller");
        return diff;
    }
}
