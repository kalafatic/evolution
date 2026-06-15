package eu.kalafatic.evolution.forge.controller.service.impl;

import eu.kalafatic.evolution.forge.controller.api.ModelLifecycleState;
import eu.kalafatic.evolution.forge.controller.service.EvoKernel;
import eu.kalafatic.evolution.forge.controller.repository.ForgeRepository;
import eu.kalafatic.evolution.forge.model.ForgeModel;
import eu.kalafatic.evolution.forge.model.ForgeSession;

public class EvoKernelImpl implements EvoKernel {
    private final ForgeRepository repository;

    public EvoKernelImpl(ForgeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void transitionTo(String sessionId, String modelId, ModelLifecycleState newState) throws Exception {
        ForgeSession session = repository.load(sessionId);
        ForgeModel model = session.getActiveModel();
        if (model == null || !model.getId().equals(modelId)) throw new Exception("Model not found");

        ModelLifecycleState current = model.getLifecycleState();
        if (!isValidTransition(current, newState)) {
            throw new Exception("Invalid transition from " + current + " to " + newState);
        }

        model.setLifecycleState(newState);
        repository.save(session);
    }

    @Override
    public boolean isValidTransition(ModelLifecycleState current, ModelLifecycleState next) {
        if (current == null) return next == ModelLifecycleState.CREATED;
        return switch (current) {
            case CREATED -> next == ModelLifecycleState.CONFIGURED;
            case CONFIGURED -> next == ModelLifecycleState.TRAINING || next == ModelLifecycleState.EVOLVING;
            case TRAINING -> next == ModelLifecycleState.STABILIZING || next == ModelLifecycleState.EVOLVING;
            case STABILIZING -> next == ModelLifecycleState.FROZEN;
            case FROZEN -> next == ModelLifecycleState.COMPILING || next == ModelLifecycleState.EVOLVING;
            case COMPILING -> next == ModelLifecycleState.EXPORTED;
            case EXPORTED -> next == ModelLifecycleState.DEPLOYED;
            case DEPLOYED -> next == ModelLifecycleState.OBSERVING;
            case OBSERVING -> next == ModelLifecycleState.EVOLVING || next == ModelLifecycleState.TRAINING;
            case EVOLVING -> next == ModelLifecycleState.TRAINING;
            default -> false;
        };
    }

    @Override
    public void routeEvent(String sessionId, String eventType, String data) {
        // Centralized event routing logic
    }
}
