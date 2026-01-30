package eu.kalafatic.evolution.controller.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager to track orchestration progress and status.
 */
public class OrchestrationStatusManager {
    private static final OrchestrationStatusManager INSTANCE = new OrchestrationStatusManager();

    private final Map<String, Double> progressMap = new ConcurrentHashMap<>();
    private final Map<String, String> statusMap = new ConcurrentHashMap<>();

    private OrchestrationStatusManager() {}

    public static OrchestrationStatusManager getInstance() {
        return INSTANCE;
    }

    public void updateStatus(String id, double progress, String status) {
        if (id != null) {
            progressMap.put(id, progress);
            statusMap.put(id, status);
        }
    }

    public double getProgress(String id) {
        if (id == null) return 0.0;
        return progressMap.getOrDefault(id, 0.0);
    }

    public String getStatus(String id) {
        if (id == null) return "Idle";
        return statusMap.getOrDefault(id, "Idle");
    }
}
