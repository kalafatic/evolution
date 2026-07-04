package eu.kalafatic.evolution.forge.controller.service.impl;

import eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelfEvoForgingServiceImpl implements SelfEvoForgingService {
    private final Map<String, ForgingStats> sessionStats = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void startForging(String sessionId, Path projectPath) throws Exception {
        updateStats(sessionId, new ForgingStats("STARTING", 0, 0, 0, 0, 0.0, "0"));

        executor.submit(() -> {
            try {
                // Forging process simulation for the UI
                updateStats(sessionId, new ForgingStats("SCANNING", 10, 50, 200, 0, 0.0, "0"));
                Thread.sleep(2000);

                updateStats(sessionId, new ForgingStats("ENHANCING", 30, 200, 200, 45, 0.0, "0"));
                Thread.sleep(2000);

                updateStats(sessionId, new ForgingStats("TRAINING", 60, 200, 200, 150, 0.724, "1/3"));
                Thread.sleep(3000);

                updateStats(sessionId, new ForgingStats("EXPORTING", 90, 200, 200, 150, 0.089, "3/3"));
                Thread.sleep(2000);

                updateStats(sessionId, new ForgingStats("COMPLETE", 100, 200, 200, 150, 0.042, "DONE"));

            } catch (Exception e) {
                updateStats(sessionId, new ForgingStats("ERROR", 0, 0, 0, 0, 0.0, "ERR"));
            }
        });
    }

    private void updateStats(String sessionId, ForgingStats stats) {
        sessionStats.put(sessionId, stats);
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
