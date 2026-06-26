package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TransitionToken;

public interface StateService {
    void initializeState(TaskContext context, IterationManager manager);
    void transition(SystemState to, TaskContext context, IterationManager manager);
    void checkStep(String entityId, String type, String description, TaskContext context, IterationManager manager) throws Exception;
}
