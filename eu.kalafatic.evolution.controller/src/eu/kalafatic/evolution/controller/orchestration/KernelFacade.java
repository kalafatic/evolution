package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Unified entry point for the Evolutionary OS Kernel.
 * Routes all external requests through the {@link IterationManager} state machine.
 */
public class KernelFacade implements IOrchestrator {

    @Override
    public OrchestratorResponse handle(TaskRequest taskRequest, TaskContext context) throws Exception {
        IterationManager kernel = KernelFactory.create(context);
        return kernel.handle(taskRequest);
    }

    @Override
    public String execute(String request, TaskContext context) throws Exception {
        OrchestratorResponse response = handle(new TaskRequest(request, context.getProjectRoot()), context);
        if (response.getResultType() == ResultType.ERROR) {
            throw new Exception(response.getContent());
        }
        return response.getSummary();
    }

    @Override
    public String executeTask(Task task, TaskContext context) throws Exception {
        // Direct task execution is delegated to the blind executor,
        // but still goes through the facade for consistency if needed.
        EvolutionOrchestrator executor = new EvolutionOrchestrator();
        return executor.executeTask(task, context);
    }
}
