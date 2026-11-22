package eu.kalafatic.evolution.forge.controller.service;

import java.nio.file.Path;
import java.util.List;

public interface SelfEvoForgingService {
    void startForging(String sessionId, Path projectPath, List<String> dataSources) throws Exception;

    /**
     * Headless/REST variant: the caller (controller bundle, which owns the
     * ForgeSessionManager) supplies the authoritative uiState JSON text directly,
     * avoiding the reflective ForgeSessionManager lookup that cannot resolve
     * controller classes from inside this bundle in an OSGi runtime.
     * (JSON text is used instead of JSONObject to avoid cross-bundle org.json
     * classloader collisions.)
     */
    default void startForging(String sessionId, Path projectPath, List<String> dataSources,
                              String uiStateJson) throws Exception {
        startForging(sessionId, projectPath, dataSources);
    }

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
