package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.RuntimeController;
import eu.kalafatic.evolution.controller.manager.OllamaService;

public class RuntimeControllerImpl implements RuntimeController {
    private final OllamaService ollamaService;

    public RuntimeControllerImpl(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @Override
    public void deployModel(String modelPath) throws Exception {
        // Register model in Ollama using the service
        String modelName = "forge-evolved-" + System.currentTimeMillis();
        ollamaService.pullModel(modelName, progress -> {
             // Track deployment progress
        });
        ollamaService.setModel(modelName);
    }

    @Override
    public String chat(String message) throws Exception {
        return ollamaService.chat(message, "runtime-session");
    }
}
