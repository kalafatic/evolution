package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public interface MediationService {
    void performMediation(TaskContext context, IterationManager manager) throws Exception;
    String performMediatedExportConvergence(String request, TaskContext context, IterationManager manager);
}
