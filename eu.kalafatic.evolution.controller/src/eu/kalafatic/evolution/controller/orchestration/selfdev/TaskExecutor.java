package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

public class TaskExecutor {
    private final EvolutionOrchestrator orchestrator = new EvolutionOrchestrator();
    private final TaskContext context;

    public TaskExecutor(TaskContext context) {
        this.context = context;
    }

    public boolean executeTasks(List<Task> tasks) {
        context.log("[EXECUTOR] Starting execution of " + tasks.size() + " tasks.");
        for (Task task : tasks) {
            try {
                context.log("[EXECUTOR] Executing task: " + task.getName());
                task.setStatus(TaskStatus.RUNNING);

                // Use the new handle method to run the atomic task directly
                TaskRequest request = new TaskRequest(task.getName(), context.getProjectRoot());
                request.getContext().put("orchestrator", context.getOrchestrator());
                request.getContext().put("taskContext", context);

                eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse orchResponse = orchestrator.handle(request, context);

                if (orchResponse.getResultType() == eu.kalafatic.evolution.controller.orchestration.ResultType.ERROR) {
                    throw new Exception(orchResponse.getContent());
                }

                task.setResponse(orchResponse.getSummary());
                context.log("[EXECUTOR] Task completed: " + task.getName());
            } catch (Exception e) {
                task.setStatus(TaskStatus.FAILED);
                task.setFeedback("Execution error: " + e.getMessage());
                context.log("[EXECUTOR] Task failed: " + task.getName() + ". Error: " + e.getMessage());
                return false; // Stop execution on first failure
            }
        }
        return true;
    }
}
