package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public interface IntentService {
    void expandIntent(TaskContext context, IterationManager manager) throws Exception;
}
