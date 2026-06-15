package eu.kalafatic.evolution.forge.controller.api;

public interface RuntimeController {
    void deployModel(String modelPath) throws Exception;
    String chat(String message) throws Exception;
}
