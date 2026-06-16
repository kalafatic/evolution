package eu.kalafatic.evolution.forge.controller.service;

import java.util.List;

import eu.kalafatic.evolution.forge.controller.api.ModelInfo;

public interface ModelService {
    String createModel(String sessionId, String modelType);
    void deleteModel(String sessionId, String modelId);
    void activateModel(String sessionId, String modelId);
    ModelInfo getActiveModel(String sessionId);
    List<ModelInfo> getModels(String sessionId);
}
