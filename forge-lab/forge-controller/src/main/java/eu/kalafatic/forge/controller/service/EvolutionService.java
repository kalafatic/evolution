package eu.kalafatic.forge.controller.service;

public interface EvolutionService {
    String addSubModel(String sessionId, String modelId, String type, String config);
    void removeSubModel(String sessionId, String modelId, String subModelId);
    void connectSubModels(String sessionId, String modelId, String fromId, String toId, String connectionType);
    void disconnectSubModels(String sessionId, String modelId, String fromId, String toId);
    void replaceSubModel(String sessionId, String modelId, String oldSubModelId, String newType, String newConfig);
    void freezeSubModel(String sessionId, String modelId, String subModelId);
    void unfreezeSubModel(String sessionId, String modelId, String subModelId);

    void evolve(String sessionId, String modelId);
    void rollback(String sessionId, String modelId, String snapshotId);
}
