package eu.kalafatic.evolution.forge.controller.service;

import java.nio.file.Path;
import java.util.List;

public interface SelfEvoForgingService {
    void startForging(String sessionId, Path projectPath, List<String> dataSources) throws Exception;
    ForgingStats getStats(String sessionId);
    void stopForging(String sessionId);

    public static record ForgingStats(
        String status,
        int progress,
        int filesScanned,
        int totalFiles,
        int instructionsGenerated,
        double currentLoss,
        String currentEpoch,
        String outputFolder
    ) {}
}
