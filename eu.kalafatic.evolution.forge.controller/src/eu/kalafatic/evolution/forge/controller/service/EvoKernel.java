package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.controller.api.ModelLifecycleState;
import eu.kalafatic.evolution.forge.model.ForgeModel;

public interface EvoKernel {
    void transitionTo(String sessionId, String modelId, ModelLifecycleState newState) throws Exception;
    boolean isValidTransition(ModelLifecycleState current, ModelLifecycleState next);
    void routeEvent(String sessionId, String eventType, String data);
}
