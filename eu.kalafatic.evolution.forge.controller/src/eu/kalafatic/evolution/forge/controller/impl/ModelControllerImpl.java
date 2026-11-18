package eu.kalafatic.evolution.forge.controller.impl;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.forge.controller.api.ModelController;
import eu.kalafatic.evolution.forge.controller.api.ModelInfo;
import eu.kalafatic.evolution.forge.controller.service.ModelService;

public class ModelControllerImpl implements ModelController {
    private final ModelService modelService;

    public ModelControllerImpl(ModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    public String createModel(String sessionId, String modelType) {
        if (modelService == null) {
            throw new UnsupportedOperationException(
                    "Model creation through the legacy forge controller is not supported; use the Darwin Forge orchestration flow");
        }
        return modelService.createModel(sessionId, modelType);
    }

    @Override
    public void deleteModel(String sessionId, String modelId) {
        if (modelService != null) modelService.deleteModel(sessionId, modelId);
    }

    @Override
    public void activateModel(String sessionId, String modelId) {
        if (modelService != null) modelService.activateModel(sessionId, modelId);
    }

    @Override
    public ModelInfo getActiveModel(String sessionId) {
        if (modelService == null) {
            return null;
        }
        return modelService.getActiveModel(sessionId);
    }

    @Override
    public List<ModelInfo> getModels(String sessionId) {
        if (modelService == null) return new ArrayList<>();
        return modelService.getModels(sessionId);
    }

    @Override
    public String getModelStructure(String sessionId) {
        if (modelService == null) {
            return "{\"available\":false,\"reason\":\"No model service is registered for this session\"}";
        }
        ModelInfo active = modelService.getActiveModel(sessionId);
        String type = active != null && active.getType() != null ? active.getType() : "Unknown";
        return "{\"nodes\":[" +
               "{\"id\":\"in\",\"name\":\"Input\",\"type\":\"DATA\"}," +
               "{\"id\":\"emb\",\"name\":\"Embedding\",\"type\":\"LAYER\"}," +
               "{\"id\":\"t1\",\"name\":\"" + type + " Block\",\"type\":\"TRANSFORMER\"}," +
               "{\"id\":\"out\",\"name\":\"Output Head\",\"type\":\"LAYER\"}" +
               "],\"links\":[]}";
    }
}
