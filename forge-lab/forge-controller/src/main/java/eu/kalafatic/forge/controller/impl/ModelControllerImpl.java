package eu.kalafatic.forge.controller.impl;

import eu.kalafatic.forge.controller.api.ModelController;
import eu.kalafatic.forge.controller.api.ModelInfo;
import eu.kalafatic.forge.controller.service.ModelService;
import java.util.List;

public class ModelControllerImpl implements ModelController {
    private final ModelService modelService;

    public ModelControllerImpl(ModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    public String createModel(String sessionId, String modelType) {
        return modelService.createModel(sessionId, modelType);
    }

    @Override
    public void deleteModel(String sessionId, String modelId) {
        modelService.deleteModel(sessionId, modelId);
    }

    @Override
    public void activateModel(String sessionId, String modelId) {
        modelService.activateModel(sessionId, modelId);
    }

    @Override
    public ModelInfo getActiveModel(String sessionId) {
        return modelService.getActiveModel(sessionId);
    }

    @Override
    public List<ModelInfo> getModels(String sessionId) {
        return modelService.getModels(sessionId);
    }
}
