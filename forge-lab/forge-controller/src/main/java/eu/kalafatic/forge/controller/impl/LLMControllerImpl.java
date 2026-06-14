package eu.kalafatic.forge.controller.impl;

import eu.kalafatic.forge.controller.api.LLMController;
import eu.kalafatic.evolution.controller.manager.OllamaService;

public class LLMControllerImpl implements LLMController {
    private final OllamaService ollamaService;

    public LLMControllerImpl(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @Override
    public String generate(String prompt) throws Exception {
        return ollamaService.generate(prompt);
    }

    @Override
    public String chat(String message, String sessionId) throws Exception {
        return ollamaService.chat(message, sessionId);
    }
}
