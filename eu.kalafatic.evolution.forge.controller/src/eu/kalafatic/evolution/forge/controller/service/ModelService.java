package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.controller.api.ModelInfo;
import java.util.List;

public interface ModelService {
    String createModel(String sessionId, String modelType);
    void deleteModel(String sessionId, String modelId);
    void activateModel(String sessionId, String modelId);
    ModelInfo getActiveModel(String sessionId);
    List<ModelInfo> getModels(String sessionId);
}
