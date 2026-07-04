package eu.kalafatic.evolution.forge.controller.service.impl;

import eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SelfEvoForgingServiceImpl implements SelfEvoForgingService {
    private final Map<String, ForgingStats> sessionStats = new ConcurrentHashMap<>();

    @Override
    public void startForging(String sessionId, Path projectPath) throws Exception {
        sessionStats.put(sessionId, new ForgingStats("INITIALIZING", 0, 0, 0, 0, 0.0, "0"));
        // Orchestration logic will be added here, delegating to Forge Agent components
    }

    @Override
    public ForgingStats getStats(String sessionId) {
        return sessionStats.getOrDefault(sessionId, new ForgingStats("IDLE", 0, 0, 0, 0, 0.0, "0"));
    }

    @Override
    public void stopForging(String sessionId) {
        sessionStats.remove(sessionId);
    }
}
