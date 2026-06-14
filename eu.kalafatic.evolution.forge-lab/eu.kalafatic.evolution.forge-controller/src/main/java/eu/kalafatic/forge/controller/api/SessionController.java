package eu.kalafatic.forge.controller.api;

public interface SessionController {
    void startTraining(String sessionId);
    void stopTraining(String sessionId);
    void switchModel(String sessionId, String modelId);
    void saveSnapshot(String sessionId);
}
