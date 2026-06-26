package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class DefaultMediationService implements MediationService {
    @Override
    public void performMediation(TaskContext context, IterationManager manager) throws Exception {
        manager.performMediatedExportConvergence(context.getOrchestrationState().getRawInput(), context);
    }

    @Override
    public String performMediatedExportConvergence(String request, TaskContext context, IterationManager manager) {
        return manager.performMediatedExportConvergence(request, context);
    }
}
