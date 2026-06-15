package eu.kalafatic.evolution.forge.controller.api;

import java.util.List;

public interface ModelController {
    String createModel(String sessionId, String modelType);
    void deleteModel(String sessionId, String modelId);
    void activateModel(String sessionId, String modelId);
    ModelInfo getActiveModel(String sessionId);
    List<ModelInfo> getModels(String sessionId);
    String getModelStructure(String sessionId);
}
