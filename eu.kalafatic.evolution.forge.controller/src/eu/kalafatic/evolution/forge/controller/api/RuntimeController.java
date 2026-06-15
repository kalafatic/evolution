package eu.kalafatic.evolution.forge.controller.api;

public interface RuntimeController {
    void deployModel(String sessionId, String modelName);
    String chat(String sessionId, String message);
}
