package eu.kalafatic.evolution.forge.controller.service;

import java.util.function.Consumer;

public interface OllamaService {
    String generate(String prompt) throws Exception;
    String chat(String message, String sessionId) throws Exception;
    void pullModel(String modelName, Consumer<Double> progressCallback) throws Exception;
    void setModel(String modelName);
}
