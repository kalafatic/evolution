package eu.kalafatic.forge.controller.service;

public interface EvolutionPolicyEngine {
    boolean isCompatible(String fromSubModelId, String toSubModelId, String connectionType);
    boolean canMutate(String modelId, String mutationType);
}
