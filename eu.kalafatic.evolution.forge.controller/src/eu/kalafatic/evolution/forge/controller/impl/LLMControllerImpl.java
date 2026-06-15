package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.LLMController;
import eu.kalafatic.evolution.forge.controller.service.OllamaService;

public class LLMControllerImpl implements LLMController {
    private final OllamaService ollamaService;

    public LLMControllerImpl(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @Override
    public String generate(String prompt) throws Exception {
        return ollamaService != null ? ollamaService.generate(prompt) : "Service Unavailable";
    }

    @Override
    public String chat(String message, String sessionId) throws Exception {
        return ollamaService != null ? ollamaService.chat(message, sessionId) : "Service Unavailable";
    }
}
