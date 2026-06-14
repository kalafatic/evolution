package eu.kalafatic.forge.controller.impl;

import eu.kalafatic.forge.controller.api.SnapshotController;
import eu.kalafatic.forge.controller.api.SnapshotInfo;
import eu.kalafatic.forge.controller.service.SnapshotService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (snapshotService == null) {
            List<Map<String, Object>> list = new ArrayList<>();
            Map<String, Object> s1 = new HashMap<>();
            s1.put("id", "snap1");
            s1.put("name", "Initial Snapshot");
            s1.put("timestamp", System.currentTimeMillis() - 3600000);
            list.add(s1);
            return list;
        }
        // In a real implementation we would convert List<SnapshotInfo> to List<Map>
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getSnapshot(String snapshotId) {
        Map<String, Object> s = new HashMap<>();
        s.put("id", snapshotId);
        s.put("name", "Snapshot " + snapshotId);
        s.put("timestamp", System.currentTimeMillis());
        return s;
    }

    @Override
    public Map<String, Object> compareSnapshots(String snapshotA, String snapshotB) {
        Map<String, Object> diff = new HashMap<>();
        diff.put("layersAdded", 0);
        diff.put("layersRemoved", 0);
        diff.put("paramDelta", 0);
        diff.put("lossDelta", 0);
        return diff;
    }
}
