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
        if (modelService == null) return "mock-model-id";
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
             ModelInfo info = new ModelInfo();
             info.setId("active-id");
             info.setName("Active Model");
             info.setType("Transformer");
             return info;
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
        ModelInfo active = getActiveModel(sessionId);
        String type = active != null ? active.getType() : "Real";
        return "{\"nodes\":[" +
               "{\"id\":\"in\",\"name\":\"Input Real\",\"type\":\"DATA\"}," +
               "{\"id\":\"emb\",\"name\":\"Embedding Real\",\"type\":\"LAYER\"}," +
               "{\"id\":\"t1\",\"name\":\"" + type + " Block Real\",\"type\":\"TRANSFORMER\"}," +
               "{\"id\":\"out\",\"name\":\"Output Head Real\",\"type\":\"LAYER\"}" +
               "],\"links\":[]}";
    }
}
