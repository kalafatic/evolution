package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Unified entry point for the Evolutionary OS Kernel.
 * Routes all external requests through the {@link IterationManager} state machine.
 */
public class KernelFacade implements IOrchestrator {

    @Override
    public OrchestratorResponse handle(TaskRequest taskRequest, TaskContext context) throws Exception {
        SessionContext session = (SessionContext) context.getMetadata().get("sessionContext");
        IterationManager kernel = KernelFactory.create(context, session);
        return kernel.handle(taskRequest);
    }

    @Override
    public String execute(String request, TaskContext context) throws Exception {
        if (context == null) throw new Exception("Cannot execute kernel command: No active task context.");
        OrchestratorResponse response = handle(new TaskRequest(request, context.getProjectRoot()), context);
        if (response.getResultType() == ResultType.ERROR) {
            throw new Exception(response.getContent());
        }
        return response.getSummary();
    }

    @Override
    public String executeTask(Task task, TaskContext context) throws Exception {
        // Direct task execution is still routed through IterationManager
        // to ensure the system is in the correct state (EXECUTING).
        SessionContext session = (SessionContext) context.getMetadata().get("sessionContext");
        IterationManager kernel = KernelFactory.create(context, session);
        java.util.List<Task> tasks = new java.util.ArrayList<>();
        tasks.add(task);
        boolean success = kernel.executeTasksWithRetries(tasks);
        if (!success) throw new Exception("Task failed: " + task.getName());
        return task.getResponse();
    }
}
