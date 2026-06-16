package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.model.api.ModelLifecycleState;

public interface EvoKernel {
    void transitionTo(String sessionId, String modelId, ModelLifecycleState newState) throws Exception;
    boolean isValidTransition(ModelLifecycleState current, ModelLifecycleState next);
    void routeEvent(String sessionId, String eventType, String data);
}
