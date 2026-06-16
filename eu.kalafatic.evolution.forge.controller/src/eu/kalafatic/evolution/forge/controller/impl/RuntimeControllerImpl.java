package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.RuntimeController;
import eu.kalafatic.evolution.forge.controller.service.OllamaService;

public class RuntimeControllerImpl implements RuntimeController {
    private final OllamaService ollamaService;

    public RuntimeControllerImpl(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @Override
    public void deployModel(String sessionId, String modelName) {
        if (ollamaService != null) {
            try {
                ollamaService.pullModel(modelName, progress -> {
                    // Progress tracking handled via UI events
                });
                ollamaService.setModel(modelName);
            } catch (Exception e) {}
        }
    }

    @Override
    public String chat(String sessionId, String message) {
        if (ollamaService != null) {
            try {
                return ollamaService.chat(message, "runtime-session");
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        return "Service Unavailable";
    }
}
