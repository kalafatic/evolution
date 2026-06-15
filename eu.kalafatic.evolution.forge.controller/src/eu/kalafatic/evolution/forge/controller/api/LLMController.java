package eu.kalafatic.evolution.forge.controller.api;

public interface LLMController {
    String generate(String prompt) throws Exception;
    String chat(String message, String sessionId) throws Exception;
}
