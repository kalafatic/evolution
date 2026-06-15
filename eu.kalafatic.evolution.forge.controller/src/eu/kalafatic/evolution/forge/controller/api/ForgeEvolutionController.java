package eu.kalafatic.evolution.forge.controller.api;

public interface ForgeEvolutionController {
    String addSubModel(String sessionId, String modelId, String type, String config);
    void connectSubModels(String sessionId, String modelId, String fromId, String toId, String connectionType);
    void evolve(String sessionId, String modelId);
}
